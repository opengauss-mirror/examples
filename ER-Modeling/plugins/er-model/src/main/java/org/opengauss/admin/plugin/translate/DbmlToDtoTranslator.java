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
 * MERCHANTABILITY OR FITFOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.opengauss.admin.plugin.translate;

import com.wn.dbml.model.Column;
import com.wn.dbml.model.ColumnSetting;
import com.wn.dbml.model.Database;
import com.wn.dbml.model.Index;
import com.wn.dbml.model.IndexSetting;
import com.wn.dbml.model.Relationship;
import com.wn.dbml.model.RelationshipSetting;
import com.wn.dbml.model.Table;

import org.opengauss.admin.plugin.dto.ColumnDTO;
import org.opengauss.admin.plugin.dto.ForeignKeyDTO;
import org.opengauss.admin.plugin.dto.IndexDTO;
import org.opengauss.admin.plugin.dto.TableDTO;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Map dbml-java models to internal DTOs so dialect-specific SQL can be generated later.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public class DbmlToDtoTranslator {
    /* ----- 1. Constants required for parsing ----- */
    private static final Pattern TYPE_WITH_PARAMS_PATTERN =
            Pattern.compile("([a-zA-Z ]+)\\s*\\(\\s*(\\d+)\\s*(?:,\\s*(\\d+))?\\s*\\)");

    /** MySQL keywords that imply unsigned semantics */
    private static final Set<String> UNSIGNED_FLAGS = Set.of("unsigned", "zerofill");

    /** Normalise MySQL variations to canonical names for easier downstream processing */
    private static final Map<String, String> MYSQL_CANONICAL = Map.ofEntries(
            Map.entry("character varying", "varchar"),
            Map.entry("double precision", "double"),
            Map.entry("timestamp without time zone", "timestamp"),
            Map.entry("timestamp with time zone", "timestamptz"),
            Map.entry("integer", "int"), // DBML may emit integer
            Map.entry("mediumint", "mediumint") // Keep as-is; dialect generators adjust later
    );

    /* ----- 2. Entry point ----- */

    /**
     * Translate a DBML database model into a list of table DTOs ready for SQL generation.
     *
     * @param database Database parsed by dbml-java
     * @return list of TableDTO instances
     */
    public static List<TableDTO> map(Database database) {
        // 1) Map every table to a TableDTO
        List<TableDTO> tableDTOs = database.getSchemas().stream()
                .flatMap(schema -> schema.getTables().stream())
                .map(DbmlToDtoTranslator::mapTable)
                .collect(Collectors.toList());

        // 2) Build a lookup map to attach foreign keys later
        Map<String, TableDTO> tableDtoMap = tableDTOs.stream()
                .collect(Collectors.toMap(TableDTO::getTableName, Function.identity()));

        // 3) Process foreign keys
        mapForeignKeys(database, tableDtoMap);

        return tableDTOs;
    }

    /* ----- 3. Table / Column / Index mapping ----- */

    private static TableDTO mapTable(Table table) {
        TableDTO dto = new TableDTO(table.getName());

        if (table.getNote() != null) {
            dto.setComment(table.getNote().getValue());
        }

        table.getColumns().forEach(col -> dto.getColumns().add(mapColumn(col)));
        List<String> inlinePrimaryKeys = table.getColumns().stream()
                .filter(col -> col.getSettings().containsKey(ColumnSetting.PRIMARY_KEY))
                .map(Column::getName)
                .collect(Collectors.toList());
        if (!inlinePrimaryKeys.isEmpty()) {
            dto.getPrimaryKeys().add(inlinePrimaryKeys);
        }
        table.getIndexes().forEach(idx -> dto.getIndexes().add(mapIndex(idx)));

        return dto;
    }

    private static ColumnDTO mapColumn(Column column) {
        ColumnDTO dto = new ColumnDTO();
        dto.setColumnName(column.getName());

        // Parse data type, unsigned flag, and enum information
        parseDataType(column.getType(), dto);

        /* Constraints */
        dto.setPrimaryKey(column.getSettings().containsKey(ColumnSetting.PRIMARY_KEY));
        dto.setUnique(column.getSettings().containsKey(ColumnSetting.UNIQUE));
        dto.setNullable(!column.getSettings().containsKey(ColumnSetting.NOT_NULL));
        dto.setAutoIncrement(column.getSettings().containsKey(ColumnSetting.INCREMENT));
        dto.setDefaultValue(column.getSettings().get(ColumnSetting.DEFAULT));

        if (column.getNote() != null) {
            dto.setComment(column.getNote().getValue());
        }
        return dto;
    }

    private static IndexDTO mapIndex(Index index) {
        IndexDTO dto = new IndexDTO();
        dto.setIndexName(index.getSettings().get(IndexSetting.NAME));
        dto.setPk(index.getSettings().containsKey(IndexSetting.PK));
        dto.setUnique(dto.getPk() || index.getSettings().containsKey(IndexSetting.UNIQUE));
        dto.setIndexType(index.getSettings().get(IndexSetting.TYPE));

        if (index.getNote() != null) {
            dto.setComment(index.getNote().getValue());
        }

        List<String> cols = index.getColumns().keySet().stream()
                .map(n -> n.replace("`", ""))
                .collect(Collectors.toList());
        dto.setColumnNames(cols);
        return dto;
    }

    /* ----- 4. Foreign keys ----- */

    private static void mapForeignKeys(Database database, Map<String, TableDTO> tableDtoMap) {
        for (Relationship rel : database.getRelationships()) {
            if (rel.getFrom().isEmpty() || rel.getTo().isEmpty()) {
                continue;
            }
            Table fromTableModel = rel.getFrom().get(0).getTable();
            Table toTableModel = rel.getTo().get(0).getTable();

            TableDTO fromDto = tableDtoMap.get(fromTableModel.getName());
            if (fromDto == null) {
                continue;
            }

            ForeignKeyDTO fk = new ForeignKeyDTO();
            fk.setReferencedTable(toTableModel.getName());

            fk.setColumnNames(rel.getFrom().stream().map(Column::getName).collect(Collectors.toList()));
            fk.setReferencedColumnNames(rel.getTo().stream().map(Column::getName).collect(Collectors.toList()));

            fk.setOnUpdateAction(rel.getSettings().get(RelationshipSetting.UPDATE));
            fk.setOnDeleteAction(rel.getSettings().get(RelationshipSetting.DELETE));

            fromDto.getForeignKeys().add(fk);
        }
    }

    /* ----- 5. Core: type parsing ----- */

    private static void parseDataType(String rawType, ColumnDTO dto) {
        /* 0) Normalise format: lowercase and collapse extra spaces */
        String lower = rawType.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s{2,}", " ");

        /* 1) Detect unsigned / zerofill flags */
        for (String flag : UNSIGNED_FLAGS) {
            if (lower.contains(flag)) {
                dto.setUnsigned(true);
                lower = lower.replace(flag, ""); // Remove keyword before parsing
            }
        }
        lower = lower.trim();

        /* 2) Handle ENUM / SET first */
        if (lower.startsWith("enum(") || lower.startsWith("set(")) {
            dto.setDataType(lower.startsWith("enum(") ? "enum" : "set");

            int l = lower.indexOf('(');
            int r = lower.lastIndexOf(')');
            if (l > 0 && r > l) {
                String inside = lower.substring(l + 1, r);
                List<String> vals = Arrays.stream(inside.split(","))
                        .map(s -> s.trim().replaceAll("^'|'$", "")) // Strip surrounding quotes
                        .collect(Collectors.toList());
                dto.setEnumValues(vals);
            }
            return; // Enum type handled
        }

        /* 3) Parse parameterised types such as varchar(255) / decimal(10,2) */
        Matcher m = TYPE_WITH_PARAMS_PATTERN.matcher(lower);
        if (m.find()) {
            String typeName = m.group(1).trim();
            int p = Integer.parseInt(m.group(2));
            Integer s = m.group(3) != null ? Integer.parseInt(m.group(3)) : null;

            // Decide between length or precision based on the type
            if ("char".equals(typeName) || typeName.contains("char")
                    || "binary".equals(typeName) || "varbinary".equals(typeName)
                    || "bit".equals(typeName)) {
                dto.setLength(p);
            } else { // Treat as numeric
                dto.setPrecision(p);
                dto.setScale(s);
            }

            lower = typeName; // Remove (p,s) before canonicalisation
        }

        /* 4) Convert synonyms to canonical names */
        String canonical = MYSQL_CANONICAL.getOrDefault(lower, lower);
        dto.setDataType(canonical);

        /* 5) float precision thresholds: real vs double */
        if ("float".equals(canonical) && dto.getScale() == null && dto.getPrecision() != null) {
            int p = dto.getPrecision();
            if (p >= 25) {
                dto.setDataType("double");
            } else {
                dto.setDataType("real");
            }
        }
    }
}
