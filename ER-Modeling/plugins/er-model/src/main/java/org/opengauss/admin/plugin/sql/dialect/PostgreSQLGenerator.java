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

package org.opengauss.admin.plugin.sql.dialect;

import org.opengauss.admin.plugin.dto.ColumnDTO;
import org.opengauss.admin.plugin.dto.ForeignKeyDTO;
import org.opengauss.admin.plugin.dto.IndexDTO;
import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.sql.core.SqlGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL generator for PostgreSQL.
 * Mirrors openGauss behaviour but omits Dolphin-specific extensions and unsigned types.
 *
 * @version 1.0.0
 * @since 2025-09-07
 */
public class PostgreSQLGenerator implements SqlGenerator {
    @Override
    public String generate(List<TableDTO> tables) {
        List<TableDTO> safeTables = safeList(tables);

        StringBuilder enumBuilder = new StringBuilder();
        Set<String> generatedEnumTypes = new HashSet<>();

        for (TableDTO table : safeTables) {
            for (ColumnDTO column : safeList(table.getColumns())) {
                if (column.getEnumValues() == null || column.getEnumValues().isEmpty()) {
                    continue;
                }
                String typeName = buildEnumTypeName(table, column);
                if (generatedEnumTypes.add(typeName)) {
                    enumBuilder.append(genCreateEnumTypeSql(typeName, column.getEnumValues())).append('\n');
                }
            }
        }

        StringBuilder tableBuilder = new StringBuilder();
        StringBuilder fkBuilder = new StringBuilder();

        for (TableDTO table : safeTables) {
            appendCreateTableSql(table, tableBuilder);
            appendIndexes(table, tableBuilder);
            appendComments(table, tableBuilder);
            appendForeignKeys(table, fkBuilder);
        }

        return enumBuilder.append('\n').append(tableBuilder).append(fkBuilder).toString();
    }

    /* -------- Table definition -------- */

    private void appendCreateTableSql(TableDTO table, StringBuilder out) {
        List<String> pkCols = resolvePrimaryKey(table);

        out.append("CREATE TABLE ").append(escape(table.getTableName())).append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();
        for (ColumnDTO column : safeList(table.getColumns())) {
            columnDefinitions.add(buildColumnDefinition(table, column));
        }

        if (!pkCols.isEmpty()) {
            String pk = pkCols.stream().map(this::escape).collect(Collectors.joining(", "));
            columnDefinitions.add("  PRIMARY KEY (" + pk + ")");
        }

        out.append(String.join(",\n", columnDefinitions)).append("\n);\n\n");
    }

    private List<String> resolvePrimaryKey(TableDTO table) {
        List<String> pk = table.getColumns().stream()
                .filter(c -> Boolean.TRUE.equals(c.getPrimaryKey()))
                .map(ColumnDTO::getColumnName)
                .collect(Collectors.toList());

        if (pk.isEmpty() && table.getIndexes() != null) {
            for (Iterator<IndexDTO> iterator = table.getIndexes().iterator(); iterator.hasNext(); ) {
                IndexDTO index = iterator.next();
                if (Boolean.TRUE.equals(index.getPk())) {
                    pk.addAll(index.getColumnNames());
                    iterator.remove();
                    break;
                }
            }
        }
        return pk;
    }

    private String buildColumnDefinition(TableDTO table, ColumnDTO column) {
        StringBuilder builder = new StringBuilder("  ")
                .append(escape(column.getColumnName()))
                .append(' ')
                .append(resolveDataType(table, column));

        if (Boolean.FALSE.equals(column.getNullable())) {
            builder.append(" NOT NULL");
        }
        if (Boolean.TRUE.equals(column.getUnique())) {
            builder.append(" UNIQUE");
        }
        if (column.getDefaultValue() != null && !column.getDefaultValue().isBlank()) {
            builder.append(" DEFAULT ").append(formatDefaultValue(column.getDefaultValue()));
        }
        return builder.toString();
    }

    private String resolveDataType(TableDTO table, ColumnDTO column) {
        String originalType = column.getDataType();
        String normalizedType = originalType == null ? "" : originalType.toLowerCase(Locale.ROOT);

        if (Boolean.TRUE.equals(column.getAutoIncrement())) {
            return resolveAutoIncrementType(normalizedType);
        }

        return resolveSpecialType(table, column, normalizedType)
                .orElseGet(() -> resolvePrimitiveType(column, normalizedType, originalType));
    }

    private String resolveAutoIncrementType(String dataType) {
        switch (dataType) {
            case "smallint":
            case "tinyint":
                return "SMALLSERIAL";
            case "bigint":
                return "BIGSERIAL";
            default:
                return "SERIAL";
        }
    }

    private String resolvePrimitiveType(ColumnDTO column, String dataType, String originalType) {
        return switch (dataType) {
            case "real", "float4" -> "REAL";
            case "double precision", "float8", "binary_double" -> "DOUBLE PRECISION";
            case "float" -> column.getPrecision() != null
                    ? "FLOAT(" + column.getPrecision() + ")"
                    : "FLOAT";
            case "boolean" -> "BOOLEAN";
            default -> {
                String resolved = resolveIntegerOrStringType(column, dataType);
                if (resolved != null) {
                    yield resolved;
                }
                yield resolveFallbackType(dataType, originalType);
            }
        };
    }

    private String resolveIntegerOrStringType(ColumnDTO column, String dataType) {
        return switch (dataType) {
            case "tinyint", "uint1", "smallint", "uint2" -> "SMALLINT";
            case "mediumint", "int", "integer", "binary_integer", "uint4" -> "INTEGER";
            case "bigint", "uint8", "int16" -> "BIGINT";
            case "numeric", "decimal", "dec", "number" -> formatWithPrecision("NUMERIC", column);
            case "char", "character", "nchar" -> "CHAR(" + (column.getLength() != null ? column.getLength() : 1) + ")";
            case "varchar", "character varying", "varchar2", "nvarchar2" ->
                    "VARCHAR(" + (column.getLength() != null ? column.getLength() : 255) + ")";
            case "text", "clob" -> "TEXT";
            case "name" -> "NAME";
            case "\"char\"" -> "\"char\"";
            case "binary", "varbinary", "bytea",
                    "byteawithoutorderwithequalcol", "byteawithoutordercol",
                    "_byteawithoutorderwithequalcol", "_byteawithoutordercol" -> "BYTEA";
            case "bit" -> "BIT(" + (column.getLength() != null ? column.getLength() : 1) + ")";
            case "bit varying", "varbit" -> {
                int len = (column.getLength() != null ? column.getLength() : 1);
                yield "BIT VARYING(" + len + ")";
            }
            default -> null;
        };
    }

    private String resolveFallbackType(String normalizedType, String originalType) {
        return switch (normalizedType) {
            case "xml", "json", "jsonb" -> normalizedType.toUpperCase(Locale.ROOT);
            case "uuid" -> "UUID";
            default -> deriveFallbackType(normalizedType, originalType);
        };
    }

    private String deriveFallbackType(String normalizedType, String originalType) {
        if (originalType != null && !originalType.isBlank()) {
            return originalType;
        }
        if (!normalizedType.isBlank()) {
            return normalizedType.toUpperCase(Locale.ROOT);
        }
        return "TEXT";
    }

    private Optional<String> resolveSpecialType(TableDTO table, ColumnDTO column, String dataType) {
        return switch (dataType) {
            case "date" -> Optional.of("DATE");
            case "time", "time without time zone" -> Optional.of(
                    appendScale("TIME", column).append(" WITHOUT TIME ZONE").toString());
            case "time with time zone", "timetz" -> Optional.of(
                    appendScale("TIME", column).append(" WITH TIME ZONE").toString());
            case "timestamp", "timestamp without time zone" -> Optional.of(
                    appendScale("TIMESTAMP", column).append(" WITHOUT TIME ZONE").toString());
            case "timestamp with time zone", "timestamptz" -> Optional.of(
                    appendScale("TIMESTAMP", column).append(" WITH TIME ZONE").toString());
            case "interval" -> Optional.of("INTERVAL");
            case "json", "jsonb", "uuid", "tsvector", "tsquery" ->
                    Optional.of(dataType.toUpperCase(Locale.ROOT));
            case "point", "lseg", "box", "path", "polygon", "circle", "cidr", "inet", "macaddr",
                    "int4range", "int8range", "numrange", "tsrange", "tstzrange", "daterange" ->
                    Optional.of(dataType.toUpperCase(Locale.ROOT));
            case "enum" -> Optional.of(escape(buildEnumTypeName(table, column)));
            case "set" -> Optional.of("TEXT");
            default -> Optional.empty();
        };
    }

    private String formatWithPrecision(String base, ColumnDTO column) {
        if (column.getPrecision() == null) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base).append('(').append(column.getPrecision());
        if (column.getScale() != null) {
            sb.append(',').append(column.getScale());
        }
        return sb.append(')').toString();
    }

    private StringBuilder appendScale(String base, ColumnDTO column) {
        StringBuilder sb = new StringBuilder(base);
        if (column.getScale() != null) {
            sb.append('(').append(column.getScale()).append(')');
        }
        return sb;
    }

    private String buildEnumTypeName(TableDTO table, ColumnDTO column) {
        return "enum_" + table.getTableName() + "_" + column.getColumnName();
    }

    private String genCreateEnumTypeSql(String typeName, List<String> values) {
        String vals = values.stream()
                .map(v -> "'" + v.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        return "CREATE TYPE " + escape(typeName) + " AS ENUM (" + vals + ");";
    }

    /* -------- Indexes -------- */

    private void appendIndexes(TableDTO table, StringBuilder out) {
        if (table.getIndexes() == null) {
            return;
        }

        String tableEscaped = escape(table.getTableName());

        for (IndexDTO index : table.getIndexes()) {
            String indexName = index.getIndexName();
            if (indexName == null || indexName.isBlank()) {
                String colsForName = String.join("_", index.getColumnNames()).replaceAll("[`()\\s]", "");
                indexName = "idx_" + table.getTableName() + "_" + colsForName;
            }

            out.append("CREATE ");
            if (Boolean.TRUE.equals(index.getUnique())) {
                out.append("UNIQUE ");
            }
            out.append("INDEX ").append(escape(indexName));

            String cols = index.getColumnNames().stream()
                    .map(this::renderIndexKey)
                    .collect(Collectors.joining(", "));

            if (index.getIndexType() != null && !index.getIndexType().isBlank()) {
                out.append(" ON ").append(tableEscaped)
                        .append(" USING ").append(index.getIndexType().toUpperCase(Locale.ROOT))
                        .append(" (").append(cols).append(");\n");
            } else {
                out.append(" ON ").append(tableEscaped)
                        .append(" (").append(cols).append(");\n");
            }
        }
        out.append('\n');
    }

    /* -------- Comments -------- */

    private void appendComments(TableDTO table, StringBuilder out) {
        String tableEscaped = escape(table.getTableName());

        if (table.getComment() != null && !table.getComment().isBlank()) {
            out.append("COMMENT ON TABLE ").append(tableEscaped)
                    .append(" IS '").append(escapeSqlLiteral(table.getComment())).append("';\n");
        }

        for (ColumnDTO column : safeList(table.getColumns())) {
            if (column.getComment() == null || column.getComment().isBlank()) {
                continue;
            }
            out.append("COMMENT ON COLUMN ").append(tableEscaped).append('.')
                    .append(escape(column.getColumnName()))
                    .append(" IS '").append(escapeSqlLiteral(column.getComment())).append("';\n");
        }
        out.append('\n');
    }

    /* -------- Foreign keys -------- */

    private void appendForeignKeys(TableDTO table, StringBuilder out) {
        String tableEscaped = escape(table.getTableName());
        Set<String> signatures = new HashSet<>();

        for (ForeignKeyDTO fk : safeList(table.getForeignKeys())) {
            boolean isForeignKeyValid = fk.getColumnNames().stream()
                    .allMatch(cn -> table.getColumns().stream()
                            .anyMatch(col -> col.getColumnName().equals(cn)));
            if (!isForeignKeyValid) {
                continue;
            }

            String signature = fk.getColumnNames()
                    + "->"
                    + fk.getReferencedTable()
                    + "."
                    + fk.getReferencedColumnNames();
            if (!signatures.add(signature)) {
                continue;
            }

            String fkName = "fk_" + table.getTableName() + "_" + fk.getColumnNames().get(0)
                    + "_" + fk.getReferencedTable();

            String localCols = fk.getColumnNames().stream().map(this::escape).collect(Collectors.joining(", "));
            String refCols = fk.getReferencedColumnNames().stream().map(this::escape).collect(Collectors.joining(", "));

            out.append("ALTER TABLE ").append(tableEscaped)
                    .append(" ADD CONSTRAINT ").append(escape(fkName))
                    .append(" FOREIGN KEY (").append(localCols).append(")")
                    .append(" REFERENCES ").append(escape(fk.getReferencedTable()))
                    .append("(").append(refCols).append(")");

            if (fk.getOnUpdateAction() != null) {
                out.append(" ON UPDATE ").append(fk.getOnUpdateAction());
            }
            if (fk.getOnDeleteAction() != null) {
                out.append(" ON DELETE ").append(fk.getOnDeleteAction());
            }

            out.append(";\n");
        }
    }

    /* -------- Helpers -------- */

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String escape(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String escapeSqlLiteral(String text) {
        return text.replace("'", "''");
    }

    private String formatDefaultValue(String value) {
        if (value == null) {
            return "NULL";
        }
        if (value.matches("^[a-zA-Z_]+\\(.*\\)$")) {
            return value;
        }
        try {
            Double.parseDouble(value);
            return value;
        } catch (NumberFormatException ignore) {
            // fall through
        }
        return "'" + escapeSqlLiteral(value) + "'";
    }

    private boolean isExpression(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() >= 2) {
            return true;
        }
        return trimmed.contains("(") || trimmed.contains(")") || trimmed.contains(" ")
                || trimmed.contains("+") || trimmed.contains("-") || trimmed.contains("::");
    }

    private String renderIndexKey(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.startsWith("`") && value.endsWith("`") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return isExpression(value) ? value : escape(value);
    }
}
