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

package org.opengauss.admin.plugin.sql.dialect;

import org.opengauss.admin.plugin.dto.ColumnDTO;
import org.opengauss.admin.plugin.dto.ForeignKeyDTO;
import org.opengauss.admin.plugin.dto.IndexDTO;
import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.sql.core.SqlGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL generator for the openGauss dialect (supports Dolphin unsigned/enum/self-referencing FK features).
 *
 * @version 1.0.0
 * @since 2025-09-07
 */
public class OpenGaussSqlGenerator implements SqlGenerator {
    private static final Pattern PRECISION_PATTERN = Pattern.compile("\\((\\d+)\\)");
    private static final Set<String> INTEGER_TYPES = Set.of(
            "tinyint", "uint1", "smallint", "uint2", "mediumint", "int", "integer", "binary_integer", "uint4",
            "bigint", "uint8", "int16");
    private static final Set<String> NUMERIC_TYPES = Set.of("numeric", "decimal", "dec", "number");
    private static final Set<String> REAL_TYPES = Set.of("real", "float4");
    private static final Set<String> DOUBLE_TYPES = Set.of("double precision", "float8", "binary_double");
    private static final Set<String> CHAR_TYPES = Set.of("char", "character", "nchar");
    private static final Set<String> VARCHAR_TYPES = Set.of("varchar", "character varying", "varchar2", "nvarchar2");
    private static final Set<String> TEXT_TYPES = Set.of("text", "clob");
    private static final Set<String> BINARY_TYPES = Set.of("binary", "varbinary", "bytea");
    private static final Set<String> BIT_VARYING_TYPES = Set.of("bit varying", "varbit");
    private static final Set<String> DIRECT_UPPERCASE_TYPES = Set.of(
            "json", "jsonb", "uuid", "tsvector", "tsquery", "hll",
            "point", "lseg", "box", "path", "polygon", "circle",
            "cidr", "inet", "macaddr",
            "int4range", "int8range", "numrange", "tsrange", "tstzrange", "daterange");

    @Override
    public String generate(List<TableDTO> tables) {
        StringBuilder builder = new StringBuilder();
        builder.append(buildEnumTypeSection(tables));
        for (TableDTO table : safeList(tables)) {
            builder.append(buildTableSection(table));
        }
        return builder.toString();
    }

    private String buildEnumTypeSection(List<TableDTO> tables) {
        Set<String> emitted = new LinkedHashSet<>();
        StringBuilder builder = new StringBuilder();
        for (TableDTO table : safeList(tables)) {
            for (ColumnDTO column : safeList(table.getColumns())) {
                List<String> enumValues = column.getEnumValues();
                if (enumValues == null || enumValues.isEmpty()) {
                    continue;
                }
                String typeName = buildEnumTypeName(table, column);
                if (emitted.add(typeName)) {
                    String values = enumValues.stream()
                            .map(v -> "'" + v.replace("'", "''") + "'")
                            .collect(Collectors.joining(", "));
                    builder.append("DO $$ BEGIN\n")
                            .append("    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = '")
                            .append(typeName)
                            .append("') THEN\n")
                            .append("        CREATE TYPE ").append(escape(typeName))
                            .append(" AS ENUM (").append(values).append(");\n")
                            .append("    END IF;\n")
                            .append("END $$;\n\n");
                }
            }
        }
        return builder.toString();
    }

    private String buildTableSection(TableDTO table) {
        StringBuilder builder = new StringBuilder();
        String tableName = escape(table.getTableName());

        List<List<String>> primaryKeys = collectPrimaryKeys(table);
        Set<String> pkColumnNames = extractPrimaryKeyColumns(primaryKeys);
        builder.append("CREATE TABLE ").append(tableName).append(" (\n");
        builder.append(String.join(",\n", buildColumnDefinitions(table, pkColumnNames)));
        appendPrimaryKey(builder, primaryKeys, table.getTableName());
        builder.append("\n);\n\n");
        appendIndexes(builder, table);
        appendComments(builder, tableName, table);
        appendForeignKeys(builder, tableName, table);
        return builder.toString();
    }

    private List<String> buildColumnDefinitions(TableDTO table, Set<String> pkColumnNames) {
        List<String> lines = new ArrayList<>();
        for (ColumnDTO column : safeList(table.getColumns())) {
            StringBuilder builder = new StringBuilder("  ")
                    .append(escape(column.getColumnName()))
                    .append(' ')
                    .append(resolveDataType(table, column));
            if (pkColumnNames.contains(column.getColumnName())
                    || Boolean.FALSE.equals(column.getNullable())) {
                builder.append(" NOT NULL");
            } else {
                builder.append(" NULL");
            }
            if (Boolean.TRUE.equals(column.getAutoIncrement())) {
                builder.append(" GENERATED BY DEFAULT AS IDENTITY");
            }
            if (column.getDefaultValue() != null && !column.getDefaultValue().isBlank()) {
                builder.append(" DEFAULT ").append(formatDefaultValue(column.getDefaultValue()));
            }
            if (Boolean.TRUE.equals(column.getUnique())) {
                builder.append(" UNIQUE");
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    private void appendPrimaryKey(StringBuilder builder, List<List<String>> primaryKeys, String tableName) {
        if (primaryKeys.isEmpty()) {
            return;
        }
        List<String> keyColumns = primaryKeys.get(0);
        String joinedColumns = keyColumns.stream()
                .map(this::expressOrEscape)
                .collect(Collectors.joining(", "));
        builder.append(",\n  CONSTRAINT ")
                .append(escape("pk_" + tableName))
                .append(" PRIMARY KEY (")
                .append(joinedColumns)
                .append(')');
    }

    private void appendIndexes(StringBuilder builder, TableDTO table) {
        for (IndexDTO index : safeList(table.getIndexes())) {
            if (Boolean.TRUE.equals(index.getPk())) {
                continue;
            }
            String name = index.getIndexName();
            if (name == null || name.isBlank()) {
                String columns = String.join("_", safeList(index.getColumnNames()))
                        .replaceAll("[`()\\s]", "");
                name = "idx_" + table.getTableName() + "_" + columns;
            }
            String columnsClause = safeList(index.getColumnNames()).stream()
                    .map(this::expressOrEscape)
                    .collect(Collectors.joining(", "));
            builder.append("CREATE ");
            if (Boolean.TRUE.equals(index.getUnique())) {
                builder.append("UNIQUE ");
            }
            builder.append("INDEX ")
                    .append(escape(name))
                    .append(" ON ")
                    .append(escape(table.getTableName()))
                    .append(" (")
                    .append(columnsClause)
                    .append(");\n");
        }
        if (!safeList(table.getIndexes()).isEmpty()) {
            builder.append('\n');
        }
    }

    private void appendComments(StringBuilder builder, String tableName, TableDTO table) {
        if (table.getComment() != null && !table.getComment().isBlank()) {
            builder.append("COMMENT ON TABLE ")
                    .append(tableName)
                    .append(" IS '")
                    .append(escapeSqlLiteral(table.getComment()))
                    .append("';\n");
        }
        for (ColumnDTO column : safeList(table.getColumns())) {
            if (column.getComment() == null || column.getComment().isBlank()) {
                continue;
            }
            builder.append("COMMENT ON COLUMN ")
                    .append(tableName).append('.')
                    .append(escape(column.getColumnName()))
                    .append(" IS '")
                    .append(escapeSqlLiteral(column.getComment()))
                    .append("';\n");
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
    }

    private void appendForeignKeys(StringBuilder builder, String tableName, TableDTO table) {
        Set<String> signatures = new HashSet<>();
        for (ForeignKeyDTO fk : safeList(table.getForeignKeys())) {
            if (!isValidForeignKey(table, fk)) {
                continue;
            }
            String signature = fk.getColumnNames()
                    + "->" + fk.getReferencedTable()
                    + "." + fk.getReferencedColumnNames();
            if (!signatures.add(signature)) {
                continue;
            }
            String constraintName = "fk_" + table.getTableName() + "_" + fk.getReferencedTable();
            String localColumns = safeList(fk.getColumnNames()).stream()
                    .map(this::expressOrEscape)
                    .collect(Collectors.joining(", "));
            String referencedColumns = safeList(fk.getReferencedColumnNames()).stream()
                    .map(this::expressOrEscape)
                    .collect(Collectors.joining(", "));
            builder.append("ALTER TABLE ")
                    .append(tableName)
                    .append(" ADD CONSTRAINT ")
                    .append(escape(constraintName))
                    .append(" FOREIGN KEY (")
                    .append(localColumns)
                    .append(") REFERENCES ")
                    .append(escape(fk.getReferencedTable()))
                    .append(" (")
                    .append(referencedColumns)
                    .append(')');
            if (fk.getOnUpdateAction() != null) {
                builder.append(" ON UPDATE ").append(fk.getOnUpdateAction());
            }
            if (fk.getOnDeleteAction() != null) {
                builder.append(" ON DELETE ").append(fk.getOnDeleteAction());
            }
            builder.append(";\n\n");
        }
    }

    private List<List<String>> collectPrimaryKeys(TableDTO table) {
        List<List<String>> keys = new ArrayList<>();
        if (table.getPrimaryKeys() != null) {
            keys.addAll(table.getPrimaryKeys());
        }
        for (IndexDTO index : safeList(table.getIndexes())) {
            if (Boolean.TRUE.equals(index.getPk())) {
                keys.add(new ArrayList<>(safeList(index.getColumnNames())));
            }
        }
        return keys;
    }

    private Set<String> extractPrimaryKeyColumns(List<List<String>> primaryKeys) {
        Set<String> columns = new HashSet<>();
        for (List<String> group : primaryKeys) {
            for (String value : group) {
                if (!value.contains("`")) {
                    columns.add(value);
                }
            }
        }
        return columns;
    }

    private boolean isValidForeignKey(TableDTO table, ForeignKeyDTO fk) {
        if (fk == null
                || safeList(fk.getColumnNames()).isEmpty()
                || safeList(fk.getReferencedColumnNames()).isEmpty()) {
            return false;
        }
        Set<String> columnNames = safeList(table.getColumns()).stream()
                .map(ColumnDTO::getColumnName)
                .collect(Collectors.toSet());
        return columnNames.containsAll(fk.getColumnNames());
    }

    private String buildEnumTypeName(TableDTO table, ColumnDTO column) {
        return "enum_" + table.getTableName() + "_" + column.getColumnName();
    }

    private String expressOrEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.startsWith("`") && value.endsWith("`")) {
            return value.substring(1, value.length() - 1);
        }
        return escape(value);
    }

    private String escape(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String escapeSqlLiteral(String text) {
        return text.replace("'", "''");
    }

    private String resolveDataType(TableDTO table, ColumnDTO column) {
        String type = column.getDataType().toLowerCase(Locale.ROOT);
        if (Boolean.TRUE.equals(column.getAutoIncrement())) {
            return resolveIdentityBaseType(type);
        }

        Optional<String> scalarType = resolveScalarType(type, column);
        if (scalarType.isPresent()) {
            return scalarType.get();
        }

        Optional<String> temporalType = resolveTemporalType(type, column);
        if (temporalType.isPresent()) {
            return temporalType.get();
        }

        if (DIRECT_UPPERCASE_TYPES.contains(type)) {
            return type.toUpperCase(Locale.ROOT);
        }
        if ("enum".equals(type)) {
            return escape(buildEnumTypeName(table, column));
        }
        if ("set".equals(type)) {
            return "TEXT";
        }
        return "TEXT";
    }

    private Optional<String> resolveScalarType(String type, ColumnDTO column) {
        boolean isUnsigned = Boolean.TRUE.equals(column.getUnsigned());
        if (isIntegerType(type)) {
            return Optional.of(mapIntegerType(type));
        }
        if (NUMERIC_TYPES.contains(type)) {
            return Optional.of(formatNumericType(column));
        }
        if ("float".equals(type)) {
            return Optional.of(formatFloatType(column, isUnsigned));
        }
        if (REAL_TYPES.contains(type)) {
            return Optional.of("REAL");
        }
        if (DOUBLE_TYPES.contains(type)) {
            return Optional.of("DOUBLE PRECISION");
        }
        if ("boolean".equals(type)) {
            return Optional.of("BOOLEAN");
        }
        if (CHAR_TYPES.contains(type)) {
            return Optional.of(formatCharacterType(column, "CHAR"));
        }
        if (VARCHAR_TYPES.contains(type)) {
            return Optional.of(formatCharacterType(column, "VARCHAR"));
        }
        if (TEXT_TYPES.contains(type)) {
            return Optional.of("TEXT");
        }
        if (BINARY_TYPES.contains(type)) {
            return Optional.of("BYTEA");
        }
        if ("bit".equals(type)) {
            return Optional.of(formatBitType(column, false));
        }
        if (BIT_VARYING_TYPES.contains(type)) {
            return Optional.of(formatBitType(column, true));
        }
        return Optional.empty();
    }

    private boolean isIntegerType(String type) {
        return INTEGER_TYPES.contains(type);
    }

    private String mapIntegerType(String type) {
        switch (type) {
            case "tinyint":
            case "uint1":
            case "smallint":
            case "uint2":
                return "SMALLINT";
            case "mediumint":
            case "int":
            case "integer":
            case "binary_integer":
            case "uint4":
                return "INTEGER";
            case "bigint":
            case "uint8":
            case "int16":
                return "BIGINT";
            default:
                return "INTEGER";
        }
    }

    private String resolveIdentityBaseType(String type) {
        return switch (type) {
            case "tinyint", "smallint" -> "SMALLINT";
            case "bigint" -> "BIGINT";
            default -> "INTEGER";
        };
    }

    private Optional<String> resolveTemporalType(String type, ColumnDTO column) {
        switch (type) {
            case "date":
                return Optional.of("DATE");
            case "time":
            case "time without time zone":
                return Optional.of(formatTemporalType("TIME", column, "WITHOUT TIME ZONE"));
            case "time with time zone":
            case "timetz":
                return Optional.of(formatTemporalType("TIME", column, "WITH TIME ZONE"));
            case "timestamp":
            case "timestamp without time zone":
                return Optional.of(formatTemporalType("TIMESTAMP", column, "WITHOUT TIME ZONE"));
            case "timestamp with time zone":
            case "timestamptz":
                return Optional.of(formatTemporalType("TIMESTAMP", column, "WITH TIME ZONE"));
            default:
                return Optional.empty();
        }
    }

    private String formatNumericType(ColumnDTO column) {
        if (column.getPrecision() == null) {
            return "NUMERIC";
        }
        StringBuilder builder = new StringBuilder("NUMERIC(")
                .append(column.getPrecision());
        if (column.getScale() != null) {
            builder.append(", ").append(column.getScale());
        }
        builder.append(')');
        return builder.toString();
    }

    private String formatFloatType(ColumnDTO column, boolean isUnsigned) {
        StringBuilder builder = new StringBuilder("FLOAT");
        if (column.getPrecision() != null) {
            builder.append("(").append(column.getPrecision()).append(")");
        }
        if (isUnsigned) {
            builder.append(" UNSIGNED");
        }
        return builder.toString();
    }

    private String formatCharacterType(ColumnDTO column, String base) {
        Integer length = column.getLength();
        if (length == null || length <= 0) {
            length = 1;
        }
        return base + "(" + length + ")";
    }

    private String formatBitType(ColumnDTO column, boolean isVarying) {
        Integer length = column.getLength();
        if (length == null || length <= 0) {
            length = 1;
        }
        return isVarying ? "BIT VARYING(" + length + ")" : "BIT(" + length + ")";
    }

    private String formatTemporalType(String base, ColumnDTO column, String zoneClause) {
        StringBuilder builder = new StringBuilder(base);
        if (column.getScale() != null) {
            builder.append("(").append(column.getScale()).append(")");
        }
        builder.append(' ').append(zoneClause);
        return builder.toString();
    }

    private String parsePrecisionType(String targetType, String originalType) {
        if (originalType == null) {
            return targetType;
        }
        Matcher matcher = PRECISION_PATTERN.matcher(originalType);
        if (matcher.find()) {
            return targetType + "(" + matcher.group(1) + ")";
        }
        return targetType;
    }

    private String formatDefaultValue(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "NULL";
        }
        String trimmed = value.trim();
        if (trimmed.matches("^[a-zA-Z_]+\\(.*\\)$")) {
            return trimmed;
        }
        try {
            Double.parseDouble(trimmed);
            return trimmed;
        } catch (NumberFormatException ex) {
            return "'" + escapeSqlLiteral(trimmed) + "'";
        }
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
