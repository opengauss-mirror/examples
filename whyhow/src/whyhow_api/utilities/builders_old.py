"""Graph builders."""

import asyncio
import inspect
import json
import logging
import time
from abc import ABC, abstractmethod
from json.decoder import JSONDecodeError
from typing import Any, List, Optional, Tuple

# import backoff
# import openai
import logfire
import spacy
import spacy.cli
from openai.types.chat import ChatCompletion

from whyhow_api.dependencies import LLMClient
from whyhow_api.models.common import (
    Entity,
    OpenAICompletionsConfig,
    OpenAIDirectivesConfig,
    SchemaEntity,
    SchemaRelation,
    SchemaTriplePattern,
    TextWithEntities,
    Triple,
    TriplePattern,
)
from whyhow_api.schemas.base import ErrorDetails
from whyhow_api.schemas.chunks import ChunkDocumentModel
from whyhow_api.schemas.schemas import GeneratedSchema
from whyhow_api.utilities.config import (
    create_schema_guided_graph_prompt,
    create_zeroshot_graph_prompt,
)

logger = logging.getLogger(__name__)

def _normalize_schema_shape(data: dict[str, Any]) -> tuple[dict[str, list[Any]], list[str]]:
    """把 LLM 返回的 schema 归一化到要求的形状；返回 (fixed_schema, warnings)。"""
    warnings: list[str] = []
    entities = list(data.get("entities") or [])
    relations = list(data.get("relations") or [])
    patterns  = list(data.get("patterns")  or [])

    rel2pair: dict[str, tuple[str, str]] = {}
    for i, p in enumerate(patterns):
        if not isinstance(p, dict): 
            warnings.append(f"patterns[{i}] is not an object, dropped")
            continue
        h = (p.get("head") or "").strip()
        r = (p.get("relation") or "").strip()
        t = (p.get("tail") or "").strip()
        if h and r and t:
            rel2pair.setdefault(r, (h, t))

    fixed_entities: list[dict[str, Any]] = []
    seen_e = set()
    for i, e in enumerate(entities):
        if not isinstance(e, dict):
            warnings.append(f"entities[{i}] is not an object, dropped"); continue
        name = (e.get("name") or "").strip()
        if not name:
            warnings.append(f"entities[{i}] missing name, dropped"); continue
        if name in seen_e: 
            continue
        seen_e.add(name)
        fixed_entities.append({"name": name, "description": e.get("description")})

    fixed_relations: list[dict[str, Any]] = []
    seen_r = set()
    for i, rel in enumerate(relations):
        if not isinstance(rel, dict):
            warnings.append(f"relations[{i}] is not an object, dropped"); continue
        name = (rel.get("name") or "").strip()
        if not name:
            warnings.append(f"relations[{i}] missing name, dropped"); continue
        head = (rel.get("head") or "").strip()
        tail = (rel.get("tail") or "").strip()
        if not (head and tail):
            if name in rel2pair:
                head, tail = rel2pair[name]
            else:
                warnings.append(f"relation '{name}' missing head/tail, dropped"); 
                continue
        if name in seen_r:
            continue
        seen_r.add(name)
        fixed_relations.append({
            "name": name,
            "head": head,
            "tail": tail,
            "description": rel.get("description"),
        })

    fixed_patterns: list[dict[str, Any]] = []
    seen_p = set()
    valid_entity_names = {e["name"] for e in fixed_entities}
    valid_relation_names = {r["name"] for r in fixed_relations}
    for i, p in enumerate(patterns):
        if not isinstance(p, dict): 
            continue
        h = (p.get("head") or "").strip()
        r = (p.get("relation") or "").strip()
        t = (p.get("tail") or "").strip()
        if not (h and r and t):
            continue
        if (h in valid_entity_names and t in valid_entity_names and r in valid_relation_names):
            key = (h, r, t)
            if key in seen_p: 
                continue
            seen_p.add(key)
            fixed_patterns.append({
                "head": h, "relation": r, "tail": t, "description": p.get("description")
            })
    return {"entities": fixed_entities, "relations": fixed_relations, "patterns": fixed_patterns}, warnings


class TextEntityExtractor(ABC):
    """Text entity extractor interface."""

    @abstractmethod
    def load(self) -> None:
        """Load the underlying model necessary for entity extraction."""

    @abstractmethod
    def extract_entities(self, text: str) -> TextWithEntities:
        """
        Extract entities from the given text.

        Parameters
        ----------
        text : str
            The input text from which to extract entities.

        Returns
        -------
        TextWithEntities
            A data structure containing the original text and the extracted entities.
        """


class SpacyEntityExtractor(TextEntityExtractor):
    """A text entity extractor based on spaCy's named entity recognition (NER)."""

    def __init__(
        self,
        model_name: str = "en_core_web_sm",
        model_disables: List[str] = ["parser", "lemmatizer"],
    ):
        self.model_name = model_name
        self.model_disables = model_disables
        self.model: spacy.language.Language | None = None

    def download_and_load_spacy_model(self) -> spacy.language.Language:
        """Attempt to load a spaCy model by its name.

        If the model is not found, it tries to download it
        and then loads it. Additional keyword arguments can be passed to specify which pipeline components
        to disable or for other loading options.

        Returns
        -------
        Language
            The loaded spaCy model.
        """
        try:
            return spacy.load(
                name=self.model_name, disable=self.model_disables
            )
        except OSError:
            print(f"{self.model_name} model not found. Downloading...")
            spacy.cli.download(self.model_name)
            return spacy.load(
                name=self.model_name, disable=self.model_disables
            )

    def load(self) -> None:
        """Load the spaCy model specified by model_name."""
        self.model = self.download_and_load_spacy_model()

    def extract_entities(self, text: str) -> TextWithEntities:
        """Extract entities using the loaded spaCy model."""
        if self.model is None:
            self.load()
        doc = self.model(text)  # type: ignore[misc]
        entities = [
            Entity(text=ent.text, label=ent.label_) for ent in doc.ents
        ]
        return TextWithEntities(text=text, entities=entities)


class OpenAIBuilder:
    """OpenAI API builder."""

    def __init__(
        self,
        llm_client: LLMClient,
        seed_entity_extractor: type[TextEntityExtractor],
    ):
        self.llm_client = llm_client
        self.seed_entity_extractor = seed_entity_extractor()

    # @backoff.on_exception(
    #     backoff.expo, openai.RateLimitError, max_time=60, max_tries=5
    # )
    @staticmethod
    async def fetch_triples(
        llm_client: LLMClient,
        chunk: ChunkDocumentModel,
        pattern: SchemaTriplePattern,
        completions_config: OpenAICompletionsConfig,
        # **kwargs,
    ) -> List[Triple] | None:
        """Fetch triples based on a schema pattern using the OpenAI API."""
        _content_str = create_schema_guided_graph_prompt(
            text=chunk.content, pattern=pattern
        )

        # Logfire trace of LLM client
        logfire.instrument_openai(llm_client.client)

        response = None
        try:
            if llm_client.metadata.language_model_name:
                completions_config.model = (
                    llm_client.metadata.language_model_name
                )
            response = await llm_client.client.chat.completions.create(
                messages=[{"role": "system", "content": _content_str}],
                **completions_config.model_dump(),
                # **kwargs,
            )
        except Exception as e:
            logger.error(f"Failed to fetch triple: {e}")

        if response is not None:
            message_content = response.choices[0].message.content
        else:
            return None

        res_entities = []
        try:
            response_text = response.choices[0].message.content.strip()

            if response_text.startswith("```") and response_text.endswith(
                "```"
            ):
                response_text = response_text[7:-3].strip()

            res_entities = json.loads(response_text)

        except JSONDecodeError as je:
            logger.error(
                f"Failed to parse message content - {je}: {message_content}"
            )
        except Exception as e:
            logger.error(f"Unexpected error parsing message content: {e}")

        if isinstance(res_entities, list):
            triples = []
            for entities in res_entities:
                head, tail = entities
                triples.append(
                    Triple(
                        head=head,
                        head_type=pattern.head.name,
                        head_properties={"chunks": [chunk.id]},
                        relation=pattern.relation.name,
                        relation_properties={"chunks": [chunk.id]},
                        tail=tail,
                        tail_type=pattern.tail.name,
                        tail_properties={"chunks": [chunk.id]},
                    )
                )
        return triples

    @staticmethod
    def parse_response_into_triples(response: ChatCompletion) -> List[Triple]:
        """Parse OpenAI response.

        Parse the response from the OpenAI API into a list
        of Triple objects.
        """
        try:
            if response is None or response.choices is None:
                return []
            message_content = response.choices[0].message.content
            if message_content is None:
                return []
            res_triples = json.loads(message_content)
            if not isinstance(res_triples, list):
                return []
            return [
                Triple(
                    head=t[0],
                    relation=t[1],
                    tail=t[2],
                )
                for t in (
                    t.strip().split(",")
                    for t in res_triples
                    if isinstance(t, str) and t.count(",") == 2
                )
            ]
        except json.JSONDecodeError as e:
            logger.error(f"Error decoding JSON from response: {e}")
            return []
        except Exception as e:
            logger.error(f"Error processing response into triples: {e}")
            return []

    @staticmethod
    async def extract_zeroshot_triples(
        llm_client: LLMClient,
        chunk: ChunkDocumentModel,
        prompt: str,
        completions_config: OpenAICompletionsConfig,
    ) -> List[Triple] | None:
        """Extract triples using zero-shot prompts."""
        # Logfire trace of LLM client
        logfire.instrument_openai(llm_client.client)

        if llm_client.metadata.language_model_name:
            completions_config.model = llm_client.metadata.language_model_name
        extract_triples_response = (
            await llm_client.client.chat.completions.create(
                messages=[
                    {
                        "role": "system",
                        "content": create_zeroshot_graph_prompt(
                            text=chunk.content, context=prompt
                        ),
                    }
                ],
                **completions_config.model_dump(),
            )
        )
        # logger.info(f"extract_triples_response: {extract_triples_response}")
        return OpenAIBuilder.parse_response_into_triples(
            response=extract_triples_response
        )

    @staticmethod
    async def extract_triples(
        llm_client: LLMClient,
        chunk: ChunkDocumentModel,
        completions_config: OpenAICompletionsConfig,
        patterns: List[SchemaTriplePattern],
    ) -> Optional[list[Triple]]:
        """
        Extract triples.

        Extracts triples from a text chunk using patterns, processing them in parallel.

        Parameters
        ----------
        llm_client : LLMClient
            The OpenAI or Azure OpenAI client instance.
        chunk : ChunkDocumentModel
            The chunk document model for triple extraction.
        completions_config : OpenAICompletionsConfig
            Configuration for OpenAI completion requests.
        patterns : List[SchemaTriplePattern] | None, optional
            A list of schema triple patterns for guided triple
            extraction, defaults to None.

        Returns
        -------
        List[Triple] | None
            A list of extracted triples if successful, or None
            if an error occurs.

        Raises
        ------
        ValueError
            If neither prompts nor patterns are provided, indicating
            that the necessary parameters for extraction are missing.
        """
        try:

            # Logfire trace of LLM client
            logfire.instrument_openai(llm_client.client)

            task_list = []

            if patterns:
                task_source: List[SchemaTriplePattern] = patterns
                extractor = OpenAIBuilder.fetch_triples
            else:
                raise ValueError("Patterns must be provided for extraction")

            # Generate tasks
            for item in task_source:
                task = extractor(
                    llm_client,
                    chunk,
                    item,
                    completions_config,
                )
                task_list.append(task)

            # Gather tasks and aggregate results
            triple_results = await asyncio.gather(*task_list)

            # Ensure triple_results does not contain None
            triples = [
                item
                for sublist in triple_results
                if sublist is not None
                for item in sublist
            ]

            return triples

        except Exception as e:
            logger.error(f"Failed to extract triples: {e}")
            return None

    async def improve_entities_matching(
        self,
        extracted_entities: List[str],
        graph_entities: List[str],
        user_query: str,
        matched_entities: List[str],
        directives: OpenAIDirectivesConfig,
        completions_config: OpenAICompletionsConfig,
    ) -> List[str]:
        """
        Improves the matching of entities using the OpenAI API.

        This function formats a prompt using the provided entities and user query, sends the prompt to the OpenAI API,
        and returns the improved entities from the response.

        Parameters
        ----------
        extracted_entities : List[str]
            A list of entities extracted from the user's query.
        graph_entities : List[str]
            A list of entities in the graph.
        user_query : str
            The user's query.
        matched_entities : List[str]
            A list of entities that were matched in the graph.
        directives : OpenAIDirectivesConfig
            Configuration for OpenAI directives.
        completions_config : OpenAICompletionsConfig
            Configuration for OpenAI completions.

        Returns
        -------
        List[str]
            The improved entities from the response from the OpenAI API.

        Raises
        ------
        json.decoder.JSONDecodeError
            If the response from the OpenAI API cannot be parsed as JSON.
        """
        prompt = directives.improve_matched_entities.format(
            extracted_entities=json.dumps(extracted_entities, indent=2),
            graph_entities=json.dumps(graph_entities, indent=2),
            user_query=user_query,
            matched_entities=json.dumps(matched_entities, indent=2),
        )

        if self.llm_client.metadata.language_model_name:
            completions_config.model = (
                self.llm_client.metadata.language_model_name
            )
        response = await self.llm_client.client.chat.completions.create(
            messages=[{"role": "system", "content": prompt}],
            **completions_config.model_dump(),
        )

        formatted_response = response.choices[0].message.content.strip()

        formatted_response = formatted_response.replace(
            "```json\n", ""
        ).replace("\n```", "")

        try:
            improved_entities = json.loads(formatted_response)
        except json.decoder.JSONDecodeError:
            print("Failed to parse formatted response as JSON")
            improved_entities = []

        return improved_entities

    async def improve_relations_matching(
        self,
        extracted_relations: List[str],
        graph_relations: List[str],
        user_query: str,
        matched_relations: List[str],
        directives: OpenAIDirectivesConfig,
        completions_config: OpenAICompletionsConfig,
    ) -> List[str]:
        """
        Improves the matching of relations using the OpenAI API.

        This function formats a prompt using the provided relations and user query, sends the prompt to the OpenAI API,
        and returns the improved relations from the response.

        Parameters
        ----------
        extracted_relations : List[str]
            A list of relations extracted from the user's query.
        graph_relations : List[str]
            A list of relations in the graph.
        user_query : str
            The user's query.
        matched_relations : List[str]
            A list of relations that were matched in the graph.
        directives : OpenAIDirectivesConfig
            Configuration for OpenAI directives.
        completions_config : OpenAICompletionsConfig
            Configuration for OpenAI completions.

        Returns
        -------
        List[str]
            The improved relations from the response from the OpenAI API.

        Raises
        ------
        json.decoder.JSONDecodeError
            If the response from the OpenAI API cannot be parsed as JSON.
        """
        prompt = directives.improve_matched_relations.format(
            extracted_relations=json.dumps(extracted_relations, indent=2),
            graph_relations=json.dumps(graph_relations, indent=2),
            user_query=user_query,
            matched_relations=json.dumps(matched_relations, indent=2),
        )

        if self.llm_client.metadata.language_model_name:
            completions_config.model = (
                self.llm_client.metadata.language_model_name
            )
        response = await self.llm_client.client.chat.completions.create(
            messages=[{"role": "system", "content": prompt}],
            **completions_config.model_dump(),
        )

        formatted_response = response.choices[0].message.content.strip()

        formatted_response = formatted_response.replace(
            "```json\n", ""
        ).replace("\n```", "")

        try:
            improved_relations = json.loads(formatted_response)
        except json.decoder.JSONDecodeError:
            print("Failed to parse formatted response as JSON")
            improved_relations = []

        return improved_relations

    @staticmethod
    async def generate_schema(
        llm_client: LLMClient, questions: List[str]
    ) -> Tuple[GeneratedSchema, list[ErrorDetails]]:
        """Generate a schema.

        Generates a basic schema from a list of questions using the LLM client.
        This implementation does not consider the `fields` of entities.
        """
        try:
            service_start_time = time.time()
            logfire.instrument_openai(llm_client.client)
            errors: list[ErrorDetails] = []

            async def execute_prompt(prompt_messages: list[dict]) -> dict[str, list[Any]] | None:
                """执行对话并解析为 JSON。"""
                try:
                    response = await llm_client.client.chat.completions.create(
                        messages=prompt_messages,
                        model=(llm_client.metadata.language_model_name or "gpt-4o-mini"),
                        temperature=0.0,
                        max_tokens=2000,

                        response_format={"type": "json_object"},
                    )
                    text = response.choices[0].message.content or ""
                    return json.loads(text)
                except Exception:
                    response = await llm_client.client.chat.completions.create(
                        messages=prompt_messages,
                        model=(llm_client.metadata.language_model_name or "gpt-4o-mini"),
                        temperature=0.0,
                        max_tokens=2000,
                    )
                    text = response.choices[0].message.content or ""
                    if "```json" in text:
                        start = text.find("```json") + len("```json")
                        end = text.rfind("```")
                        text = text[start:end].strip()
                    try:
                        return json.loads(text)
                    except JSONDecodeError as je:
                        logger.error(f"Failed to parse message content {je}: {text}")
                        return None

            
            async def generate_json_schema(idx: int, question: str) -> dict[str, list[Any]] | None:
                """针对单个问题生成 schema（返回字典，不在此处做校验）。"""

                logger.info(f'Extracting schema for question: "{question}"')

                other = questions[:idx] + questions[idx + 1:]

                sys_prompt = f"""
            You are a structured data generator. Return ONLY a JSON object that can be parsed by json.loads().

            You must produce a knowledge-graph schema with this EXACT shape:

            {{
            "entities": [{{"name": "string", "description": "string"}}],
            "relations": [{{"name": "string", "head": "string", "tail": "string", "description": "string"}}],
            "patterns": [{{"head": "string", "relation": "string", "tail": "string", "description": "string"}}]
            }}

            Hard requirements:
            - all names are lowercase, words separated by spaces (no underscores, no camelCase)
            - EVERY relation item MUST include "head" and "tail"
            - patterns must reference existing entity names and relation names
            - output plain JSON, no markdown fences, no extra text

            Example (just a shape hint, adapt to the actual questions):
            {{
            "entities": [
                {{"name": "company", "description": "an organization engaged in business activities"}},
                {{"name": "employee", "description": "an individual who works under a contract of employment"}}
            ],
            "relations": [
                {{"name": "employs", "head": "company", "tail": "employee", "description": "a company hires or has individuals working for it"}}
            ],
            "patterns": [
                {{"head": "company", "relation": "employs", "tail": "employee", "description": "a company employs an individual as an employee"}}
            ]
            }}
                """.strip()

                user_prompt = f"""
            Primary question:
            - {question}

            Secondary questions (context only):
            - {("\n- ".join(other)) if other else "(none)"} 

            Now return the schema JSON only.
            """.strip()

                return await execute_prompt(
                    [
                        {"role": "system", "content": sys_prompt},
                        {"role": "user", "content": user_prompt},
                    ]
                )

            
            def merge_and_validate_schemas(json_schemas: list[dict[str, list[Any]]]) -> GeneratedSchema:
                normalized: list[dict[str, list[Any]]] = []
                for sc in json_schemas:
                    fixed, warns = _normalize_schema_shape(sc)
                    for w in warns:
                        errors.append(ErrorDetails(message=w, level="warning"))
                    normalized.append(fixed)

                schema: dict[str, list[Any]] = {"entities": [], "relations": [], "patterns": []}
                seen_e, seen_r, seen_p = set(), set(), set()

                for sc in normalized:
                    for e in sc["entities"]:
                        if e["name"] not in seen_e:
                            schema["entities"].append(e)
                            seen_e.add(e["name"])

                    for r in sc["relations"]:
                        if r["name"] not in seen_r:
                            schema["relations"].append(r)
                            seen_r.add(r["name"])

                    valid_es = {e["name"] for e in schema["entities"]}
                    valid_rs = {r["name"] for r in schema["relations"]}

                    for p in sc["patterns"]:
                        key = (p["head"], p["relation"], p["tail"])
                        if (p["head"] in valid_es and p["tail"] in valid_es and p["relation"] in valid_rs) and key not in seen_p:
                            schema["patterns"].append(p)
                            seen_p.add(key)

                try:
                    return GeneratedSchema(**schema)
                except Exception as e:
                    logger.info(f"Unable to parse generated schema - {e}: {schema}")
                    raise ValueError("Unable to parse generated schema.")


            json_schemas = await asyncio.gather(
                *[
                    generate_json_schema(idx, question)
                    for idx, question in enumerate(questions)
                ]
            )
            if json_schemas is None:
                raise ValueError("Failed to generate schemas.")
            generated_schema = merge_and_validate_schemas(
                json_schemas=[
                    schema for schema in json_schemas if schema is not None
                ]
            )
            service_end_time = time.time()
            service_duration = (
                service_end_time - service_start_time
            )
            logger.info(f"Schema generated in {service_duration:.4f} seconds")
            print(f"Schema generated in {service_duration:.4f} seconds")
            return generated_schema, errors

        except Exception as e:
            logger.error(f"Failed to generate schema: {e}")
            errors.append(
                ErrorDetails(
                    message="Failed to generate schema. Please try again or reformat your questions.",
                    level="critical",
                )
            )
            return (
                GeneratedSchema(entities=[], relations=[], patterns=[]),
                errors,
            )
