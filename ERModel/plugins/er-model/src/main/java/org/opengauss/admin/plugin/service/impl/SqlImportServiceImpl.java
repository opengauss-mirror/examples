/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 *
 * openGauss is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 * http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.opengauss.admin.plugin.service.impl;

import lombok.extern.slf4j.Slf4j;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.Index.ColumnParams;
import org.opengauss.admin.plugin.domain.InlineReference;
import org.opengauss.admin.plugin.domain.ReferenceParseResult;
import org.opengauss.admin.plugin.dto.ColumnDTO;
import org.opengauss.admin.plugin.dto.ForeignKeyDTO;
import org.opengauss.admin.plugin.dto.IndexDTO;
import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.service.SqlImportService;
import org.opengauss.admin.plugin.translate.SqlToDbmlTranslator;
import org.opengauss.admin.plugin.util.SqlDumpCleaner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts SQL DDL text into DBML by parsing statements with JSqlParser and mapping them to DTOs.
 *
 * @since 2025-01-07
 */
@Slf4j
@Service
public class SqlImportServiceImpl implements SqlImportService {
    private static final Pattern ALTER_ADD_FOREIGN = Pattern.compile(
            "(?i)ADD\\s+(CONSTRAINT\\s+\\S+\\s+)?FOREIGN\\s+KEY\\s*\\(([^)]+)\\)\\s*REFERENCES\\s+([\\w\\.\\\"`]+)"
                    + "\\s*\\(([^)]+)\\)([^;]*)");
    private static final Pattern ALTER_ADD_PRIMARY = Pattern.compile(
            "(?i)ADD\\s+(CONSTRAINT\\s+\\S+\\s+)?PRIMARY\\s+KEY\\s*\\(([^)]+)\\)");
    private static final Pattern ALTER_ADD_UNIQUE = Pattern.compile(
            "(?i)ADD\\s+(CONSTRAINT\\s+\\S+\\s+)?UNIQUE\\s*(KEY\\s+\\S+\\s*)?\\(([^)]+)\\)");
    private static final Set<String> BOUNDARY_KEYWORDS = Set.of(
            "not", "null", "unique", "primary", "key", "references",
            "constraint", "check", "comment");
    private static final Pattern COLUMN_COMMENT_PATTERN = Pattern.compile(
            "(?is)COMMENT\\s+ON\\s+COLUMN\\s+((?:[\\w\\\"`]+\\.)?)([\\w\\\"`]+)\\.([\\w\\\"`]+)"
                    + "\\s+IS\\s+'((?:''|[^'])*)'");
    private static final Pattern FK_ON_DELETE_PATTERN = Pattern.compile(
            "(?i)ON\\s+DELETE\\s+(CASCADE|SET\\s+NULL|SET\\s+DEFAULT|RESTRICT|NO\\s+ACTION)");
    private static final Pattern FK_ON_UPDATE_PATTERN = Pattern.compile(
            "(?i)ON\\s+UPDATE\\s+(CASCADE|SET\\s+NULL|SET\\s+DEFAULT|RESTRICT|NO\\s+ACTION)");
    private static final Pattern INDEX_COLUMNS_PATTERN = Pattern.compile("\\((.+)\\)");
    private static final Pattern TABLE_COMMENT_PATTERN = Pattern.compile(
            "(?is)COMMENT\\s+ON\\s+TABLE\\s+([\\w\\.\\\"`]+)\\s+IS\\s+'((?:''|[^'])*)'");
    private static final Pattern TABLE_LEVEL_FK_PATTERN = Pattern.compile(
            "(?i)REFERENCES\\s+([\\w\\.\\\"`]+)\\s*\\(([^\\)]+)\\)([^;]*)");
    private static final List<Pattern> UNSUPPORTED_CREATE_PATTERNS = List.of(
            Pattern.compile("(?is)CREATE\\s+EXTENSION[\\s\\S]*?;"),
            Pattern.compile("(?is)CREATE\\s+TRIGGER[\\s\\S]*?;"),
            Pattern.compile("(?is)CREATE\\s+SCHEMA[\\s\\S]*?;"),
            Pattern.compile("(?is)CREATE\\s+SEQUENCE[\\s\\S]*?;"),
            Pattern.compile("(?is)CREATE\\s+TYPE[\\s\\S]*?;")
    );

    private static final class InlineReferenceBuilder {
        private final String referencedTable;
        private final List<String> referencedColumns;
        private Optional<String> onDelete = Optional.empty();
        private Optional<String> onUpdate = Optional.empty();

        InlineReferenceBuilder(String referencedTable, List<String> referencedColumns) {
            this.referencedTable = referencedTable;
            this.referencedColumns = referencedColumns;
        }

        void onDelete(Optional<String> action) {
            this.onDelete = action;
        }

        void onUpdate(Optional<String> action) {
            this.onUpdate = action;
        }

        InlineReference build() {
            return new InlineReference(referencedTable, referencedColumns, onDelete, onUpdate);
        }
    }

    private static final class CommentBucket {
        private final Map<String, String> tableNotes = new HashMap<>();
        private final Map<String, Map<String, String>> columnNotes = new HashMap<>();
    }

    @Override
    public String sqlToDbml(String sqlText, DatabaseDialect dialect, boolean shouldSkipData) {
        String ddlOnly = shouldSkipData ? SqlDumpCleaner.keepDdlOnly(sqlText, dialect) : sqlText;
        String sanitized = removeUnsupportedCreateStatements(preprocessPostgresCasts(ddlOnly));

        CommentBucket comments = extractComments(sanitized);
        Map<String, TableDTO> tablesByName = new LinkedHashMap<>();
        parseStatements(sanitized, dialect, tablesByName);
        applyComments(comments, tablesByName);

        return SqlToDbmlTranslator.generate(new ArrayList<>(tablesByName.values()));
    }

    private void parseStatements(String ddl, DatabaseDialect dialect, Map<String, TableDTO> tablesByName) {
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(ddl);
            for (Statement statement : statements.getStatements()) {
                if (statement instanceof CreateTable) {
                    handleCreateTable((CreateTable) statement, dialect, tablesByName);
                } else if (statement instanceof CreateIndex) {
                    handleCreateIndex((CreateIndex) statement, tablesByName);
                } else if (statement instanceof Alter) {
                    handleAlterFallback((Alter) statement, tablesByName);
                } else {
                    log.debug("Skipping unsupported statement: {}", statement);
                }
            }
        } catch (JSQLParserException ex) {
            throw new IllegalArgumentException("SQL 解析失败：" + ex.getMessage(), ex);
        }
    }

    private void handleCreateTable(
            CreateTable createTable,
            DatabaseDialect dialect,
            Map<String, TableDTO> tablesByName) {
        String tableName = unquote(createTable.getTable());
        TableDTO table = tablesByName.computeIfAbsent(tableName, TableDTO::new);
        table.setTableName(tableName);

        for (ColumnDefinition definition : safe(createTable.getColumnDefinitions())) {
            ColumnDTO column = buildColumn(definition, dialect);
            table.getColumns().add(column);
            parseInlineReferences(definition, table);
        }

        for (Index index : safe(createTable.getIndexes())) {
            applyTableConstraint(table, index);
        }
    }

    private ColumnDTO buildColumn(ColumnDefinition definition, DatabaseDialect dialect) {
        ColumnDTO column = new ColumnDTO();
        column.setColumnName(unquote(definition.getColumnName()));

        ColDataType type = definition.getColDataType();
        String rawType = type == null || type.getDataType() == null ? "varchar" : type.getDataType();
        List<String> args = type == null || type.getArgumentsStringList() == null
                ? List.of()
                : type.getArgumentsStringList();
        normalizeTypeAndLength(rawType, args, dialect, column);

        parseColumnSpecs(definition.getColumnSpecs(), dialect, column);
        return column;
    }

    private void normalizeTypeAndLength(
            String rawType,
            List<String> args,
            DatabaseDialect dialect,
            ColumnDTO column) {
        String normalized = Optional.ofNullable(rawType)
                .orElse("varchar")
                .trim()
                .toLowerCase(Locale.ROOT);

        if ("serial".equals(normalized) || "bigserial".equals(normalized) || "smallserial".equals(normalized)) {
            column.setAutoIncrement(true);
            if ("serial".equals(normalized)) {
                column.setDataType("int");
            } else if ("bigserial".equals(normalized)) {
                column.setDataType("bigint");
            } else {
                column.setDataType("smallint");
            }
            return;
        }

        if ("integer".equals(normalized)) {
            normalized = "int";
        }
        if (normalized.startsWith("timestamp")) {
            normalized = "timestamp";
        }

        boolean isUnsigned = normalized.contains("unsigned");
        if (isUnsigned) {
            normalized = normalized.replace("unsigned", "").trim();
        }
        column.setUnsigned(isUnsigned);
        column.setDataType(normalized);

        if (args.isEmpty()) {
            return;
        }
        if (args.size() == 1) {
            parseIntSafe(args.get(0)).ifPresent(value -> column.setLength(value));
            return;
        }
        if (args.size() >= 2) {
            parseIntSafe(args.get(0)).ifPresent(value -> column.setPrecision(value));
            parseIntSafe(args.get(1)).ifPresent(value -> column.setScale(value));
        }
    }

    private OptionalInt parseIntSafe(String text) {
        if (text == null) {
            return OptionalInt.empty();
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(t));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    private void applyTableConstraint(TableDTO table, Index index) {
        String type = Optional.ofNullable(index.getType()).orElse("").toUpperCase(Locale.ROOT);
        List<String> columns = index.getColumnsNames() == null
                ? Collections.emptyList()
                : index.getColumnsNames();

        if (type.contains("PRIMARY")) {
            if (!columns.isEmpty()) {
                table.getPrimaryKeys().add(new ArrayList<>(stripQuotes(columns)));
            }
            return;
        }

        if (type.contains("UNIQUE")) {
            IndexDTO dto = new IndexDTO();
            dto.setIndexName(unquote(index.getName()));
            dto.setUnique(true);
            dto.getColumnNames().addAll(stripQuotes(columns));
            table.getIndexes().add(dto);
            return;
        }

        if (type.contains("FOREIGN")) {
            parseTableLevelForeignKey(index)
                    .ifPresent(fk -> {
                        if (fk.getColumnNames().isEmpty()) {
                            fk.setColumnNames(new ArrayList<>(stripQuotes(columns)));
                        }
                        table.getForeignKeys().add(fk);
                    });
            return;
        }

        if (!columns.isEmpty()) {
            IndexDTO dto = new IndexDTO();
            dto.setIndexName(unquote(index.getName()));
            dto.setUnique(false);
            dto.getColumnNames().addAll(stripQuotes(columns));
            table.getIndexes().add(dto);
        }
    }

    private void handleCreateIndex(CreateIndex createIndex, Map<String, TableDTO> tablesByName) {
        Table tableRef = createIndex.getTable();
        if (tableRef == null) {
            return;
        }
        String tableName = unquote(tableRef.getName());
        TableDTO table = tablesByName.computeIfAbsent(tableName, TableDTO::new);
        table.setTableName(tableName);

        Index index = createIndex.getIndex();
        if (index == null) {
            return;
        }

        IndexDTO dto = new IndexDTO();
        dto.setIndexName(unquote(index.getName()));
        dto.setUnique(isUniqueIndex(createIndex, index));
        dto.getColumnNames().addAll(stripQuotes(extractIndexColumns(createIndex, index)));
        table.getIndexes().add(dto);
    }

    private boolean isUniqueIndex(CreateIndex createIndex, Index index) {
        String type = Optional.ofNullable(index.getType()).orElse("");
        if (type.toUpperCase(Locale.ROOT).contains("UNIQUE")) {
            return true;
        }
        return createIndex.toString().toUpperCase(Locale.ROOT).contains("CREATE UNIQUE INDEX");
    }

    private List<String> extractIndexColumns(CreateIndex createIndex, Index index) {
        if (index.getColumnsNames() != null && !index.getColumnsNames().isEmpty()) {
            return index.getColumnsNames();
        }
        List<ColumnParams> columnParams = index.getColumns();
        if (columnParams != null && !columnParams.isEmpty()) {
            List<String> columns = new ArrayList<>(columnParams.size());
            for (ColumnParams params : columnParams) {
                columns.add(params.getColumnName());
            }
            return columns;
        }
        Matcher matcher = INDEX_COLUMNS_PATTERN.matcher(createIndex.toString());
        if (!matcher.find()) {
            return List.of();
        }
        return splitColumns(matcher.group(1));
    }

    private void handleAlterFallback(Alter alter, Map<String, TableDTO> tablesByName) {
        Table tableRef = alter.getTable();
        if (tableRef == null) {
            return;
        }
        String tableName = unquote(tableRef.getName());
        TableDTO table = tablesByName.computeIfAbsent(tableName, TableDTO::new);
        table.setTableName(tableName);

        String alterSql = alter.toString();
        applyPrimaryKeyRegex(table, alterSql);
        applyUniqueRegex(table, alterSql);
        applyForeignKeyRegex(table, alterSql);
    }

    private void applyPrimaryKeyRegex(TableDTO table, String alterSql) {
        Matcher matcher = ALTER_ADD_PRIMARY.matcher(alterSql);
        while (matcher.find()) {
            List<String> columns = stripQuotes(splitColumns(matcher.group(2)));
            if (!columns.isEmpty()) {
                table.getPrimaryKeys().add(columns);
            }
        }
    }

    private void applyUniqueRegex(TableDTO table, String alterSql) {
        Matcher matcher = ALTER_ADD_UNIQUE.matcher(alterSql);
        while (matcher.find()) {
            List<String> columns = stripQuotes(splitColumns(matcher.group(3)));
            if (columns.isEmpty()) {
                continue;
            }
            IndexDTO dto = new IndexDTO();
            dto.setUnique(true);
            dto.getColumnNames().addAll(columns);
            table.getIndexes().add(dto);
        }
    }

    private void applyForeignKeyRegex(TableDTO table, String alterSql) {
        Matcher matcher = ALTER_ADD_FOREIGN.matcher(alterSql);
        while (matcher.find()) {
            List<String> sourceColumns = stripQuotes(splitColumns(matcher.group(2)));
            String referencedTable = unquote(matcher.group(3));
            List<String> targetColumns = stripQuotes(splitColumns(matcher.group(4)));
            String tail = matcher.group(5);

            if (sourceColumns.isEmpty() || targetColumns.isEmpty()) {
                continue;
            }

            ForeignKeyDTO fk = new ForeignKeyDTO();
            fk.setColumnNames(sourceColumns);
            fk.setReferencedTable(referencedTable);
            fk.setReferencedColumnNames(targetColumns);
            findMatch(FK_ON_DELETE_PATTERN, tail).ifPresent(fk::setOnDeleteAction);
            findMatch(FK_ON_UPDATE_PATTERN, tail).ifPresent(fk::setOnUpdateAction);
            table.getForeignKeys().add(fk);
        }
    }

    private void parseColumnSpecs(List<String> specs, DatabaseDialect dialect, ColumnDTO column) {
        List<String> tokens = safe(specs);
        List<String> lowerTokens = toLowerCase(tokens);
        for (int i = 0; i < lowerTokens.size(); i++) {
            String lower = lowerTokens.get(i);
            switch (lower) {
                case "not":
                    if (i + 1 < lowerTokens.size() && "null".equals(lowerTokens.get(i + 1))) {
                        column.setNullable(false);
                    }
                    break;
                case "null":
                    column.setNullable(true);
                    break;
                case "unique":
                    column.setUnique(true);
                    break;
                case "primary":
                    if (i + 1 < lowerTokens.size() && "key".equals(lowerTokens.get(i + 1))) {
                        column.setPrimaryKey(true);
                    }
                    break;
                case "auto_increment":
                case "auto":
                    column.setAutoIncrement(true);
                    break;
                case "unsigned":
                    column.setUnsigned(true);
                    break;
                case "default":
                    collectDefault(tokens, lowerTokens, i)
                            .map(this::normalizeDefaultValue)
                            .ifPresent(column::setDefaultValue);
                    break;
                case "comment":
                    collectDefault(tokens, lowerTokens, i)
                            .ifPresent(column::setComment);
                    break;
                default:
                    log.trace("Unsupported column spec '{}' for {}", tokens.get(i), column.getColumnName());
            }
        }
    }

    private Optional<String> collectDefault(
            List<String> tokens,
            List<String> lowerTokens,
            int index) {
        StringBuilder builder = new StringBuilder();
        for (int i = index + 1; i < lowerTokens.size(); i++) {
            String lower = lowerTokens.get(i);
            if (isBoundaryKeyword(lower)) {
                break;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens.get(i));
        }
        return builder.length() == 0 ? Optional.empty() : Optional.of(builder.toString());
    }

    private List<String> splitColumns(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(s -> s.replace("(`", "(").replace("`)", ")"))
                .map(s -> s.replace("`", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void parseInlineReferences(ColumnDefinition definition, TableDTO table) {
        List<String> specs = safe(definition.getColumnSpecs());
        List<String> lower = toLowerCase(specs);
        for (int i = 0; i < lower.size(); i++) {
            if (!"references".equals(lower.get(i))) {
                continue;
            }
            InlineReference reference = extractInlineReference(specs, i);
            if (reference.referencedTable().isEmpty() || reference.referencedColumns().isEmpty()) {
                continue;
            }
            ForeignKeyDTO fk = new ForeignKeyDTO();
            fk.setReferencedTable(reference.referencedTable());
            fk.setColumnNames(List.of(unquote(definition.getColumnName())));
            fk.setReferencedColumnNames(reference.referencedColumns());
            fk.setOnDeleteAction(reference.onDeleteAction().orElse(null));
            fk.setOnUpdateAction(reference.onUpdateAction().orElse(null));
            table.getForeignKeys().add(fk);
        }
    }

    private InlineReference extractInlineReference(List<String> specs, int startIndex) {
        InlineReferenceBuilder builder = new InlineReferenceBuilder(
                startIndex + 1 < specs.size() ? unquote(specs.get(startIndex + 1)) : "",
                parseParenthesizedColumns(specs, startIndex + 2));

        for (int i = startIndex + 2; i < specs.size(); i++) {
            if (!"on".equalsIgnoreCase(specs.get(i)) || i + 2 >= specs.size()) {
                continue;
            }
            String actionType = specs.get(i + 1).toLowerCase(Locale.ROOT);
            String action = specs.get(i + 2);
            switch (actionType) {
                case "delete" -> builder.onDelete(Optional.ofNullable(action));
                case "update" -> builder.onUpdate(Optional.ofNullable(action));
                default -> log.debug("Unsupported inline reference action '{}'", actionType);
            }
        }
        return builder.build();
    }

    private List<String> parseParenthesizedColumns(List<String> tokens, int startIndex) {
        StringBuilder builder = new StringBuilder();
        int depth = 0;
        boolean hasStarted = false;
        for (int i = startIndex; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.contains("(")) {
                depth++;
                hasStarted = true;
            }
            if (hasStarted) {
                builder.append(token).append(' ');
            }
            if (token.contains(")")) {
                depth--;
                if (depth <= 0) {
                    break;
                }
            }
        }
        if (builder.length() == 0) {
            return List.of();
        }
        return stripQuotes(splitColumns(builder.toString()));
    }

    private Optional<ForeignKeyDTO> parseTableLevelForeignKey(Index index) {
        ReferenceParseResult result = parseReferenceTokens(safe(index.getIndexSpec()));
        if (result.referencedTable() == null || result.referencedColumns().isEmpty()) {
            Matcher matcher = TABLE_LEVEL_FK_PATTERN.matcher(index.toString());
            if (matcher.find()) {
                String referencedTable = unquote(matcher.group(1));
                List<String> referencedColumns = stripQuotes(splitColumns(matcher.group(2)));
                String tail = matcher.group(3);
                Optional<String> onDelete = findMatch(FK_ON_DELETE_PATTERN, tail);
                Optional<String> onUpdate = findMatch(FK_ON_UPDATE_PATTERN, tail);
                result = new ReferenceParseResult(referencedTable, referencedColumns, onDelete, onUpdate);
            }
        }

        if (result.referencedTable() == null || result.referencedColumns().isEmpty()) {
            return Optional.empty();
        }

        ForeignKeyDTO fk = new ForeignKeyDTO();
        List<String> sourceColumns = index.getColumnsNames() == null
                ? Collections.emptyList()
                : index.getColumnsNames();
        fk.setColumnNames(stripQuotes(sourceColumns));
        fk.setReferencedTable(result.referencedTable());
        fk.setReferencedColumnNames(result.referencedColumns());
        fk.setOnDeleteAction(result.onDelete().orElse(null));
        fk.setOnUpdateAction(result.onUpdate().orElse(null));
        return Optional.of(fk);
    }

    private ReferenceParseResult parseReferenceTokens(List<String> tokens) {
        String referencedTable = null;
        List<String> referencedColumns = new ArrayList<>();
        Optional<String> onDelete = Optional.empty();
        Optional<String> onUpdate = Optional.empty();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
            if ("references".equals(lower) && i + 1 < tokens.size()) {
                referencedTable = unquote(tokens.get(i + 1));
                referencedColumns = stripQuotes(parseParenthesizedColumns(tokens, i + 2));
            } else if ("on".equals(lower) && i + 2 < tokens.size()) {
                String actionType = tokens.get(i + 1).toLowerCase(Locale.ROOT);
                String action = tokens.get(i + 2);
                switch (actionType) {
                    case "delete" -> onDelete = Optional.ofNullable(action);
                    case "update" -> onUpdate = Optional.ofNullable(action);
                    default -> log.debug("Unsupported reference action '{}'", actionType);
                }
            } else {
                continue;
            }
        }
        return new ReferenceParseResult(referencedTable, referencedColumns, onDelete, onUpdate);
    }

    private List<String> stripQuotes(List<String> values) {
        List<String> cleaned = new ArrayList<>(values.size());
        for (String value : values) {
            cleaned.add(unquote(value));
        }
        return cleaned;
    }

    private String normalizeDefaultValue(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        while (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            return "'" + value.substring(1, value.length() - 1).replace("'", "''") + "'";
        }
        if (value.matches("[-+]?\\d+(\\.\\d+)?")) {
            return value;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return value.toLowerCase(Locale.ROOT);
        }
        if (value.equalsIgnoreCase("null")) {
            return "NULL";
        }
        value = value.replaceAll("\\s*\\(\\s*", "(").replaceAll("\\s*\\)\\s*", ")");
        return "`" + value + "`";
    }

    private String preprocessPostgresCasts(String sql) {
        if (sql == null) {
            return "";
        }
        String sanitized = sql.replaceAll("'([^']+)'::regclass", "'$1'");
        return sanitized.replaceAll("::[a-zA-Z0-9_ ]+", "");
    }

    private String removeUnsupportedCreateStatements(String sql) {
        if (sql == null) {
            return "";
        }
        String sanitized = sql;
        for (Pattern pattern : UNSUPPORTED_CREATE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }
        return sanitized;
    }

    private CommentBucket extractComments(String sql) {
        CommentBucket bucket = new CommentBucket();

        Matcher tableMatcher = TABLE_COMMENT_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            String tableName = unquote(tableMatcher.group(1));
            String note = unescapeSqlString(tableMatcher.group(2));
            bucket.tableNotes.put(tableName, note);
        }

        Matcher columnMatcher = COLUMN_COMMENT_PATTERN.matcher(sql);
        while (columnMatcher.find()) {
            String tableName = unquote(columnMatcher.group(2));
            String columnName = unquote(columnMatcher.group(3));
            String note = unescapeSqlString(columnMatcher.group(4));
            bucket.columnNotes
                    .computeIfAbsent(tableName, key -> new HashMap<>())
                    .put(columnName, note);
        }
        return bucket;
    }

    private void applyComments(CommentBucket comments, Map<String, TableDTO> tablesByName) {
        for (Map.Entry<String, String> entry : comments.tableNotes.entrySet()) {
            TableDTO table = tablesByName.get(entry.getKey());
            if (table != null && (table.getComment() == null || table.getComment().isBlank())) {
                table.setComment(entry.getValue());
            }
        }
        for (Map.Entry<String, Map<String, String>> entry : comments.columnNotes.entrySet()) {
            TableDTO table = tablesByName.get(entry.getKey());
            if (table == null) {
                continue;
            }
            Map<String, String> columnNotes = entry.getValue();
            for (ColumnDTO column : table.getColumns()) {
                String note = columnNotes.get(column.getColumnName());
                if (note != null && (column.getComment() == null || column.getComment().isBlank())) {
                    column.setComment(note);
                }
            }
        }
    }

    private String unquote(Table table) {
        return table == null ? "" : unquote(table.getName());
    }

    private String unquote(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("`") && trimmed.endsWith("`"))
                || (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String unescapeSqlString(String value) {
        return value.replace("''", "'");
    }

    private Optional<String> findMatch(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? Optional.ofNullable(matcher.group(1)) : Optional.empty();
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private List<String> toLowerCase(List<String> tokens) {
        List<String> lower = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            lower.add(token == null ? "" : token.toLowerCase(Locale.ROOT));
        }
        return lower;
    }

    private boolean isBoundaryKeyword(String token) {
        return BOUNDARY_KEYWORDS.contains(token);
    }
}
