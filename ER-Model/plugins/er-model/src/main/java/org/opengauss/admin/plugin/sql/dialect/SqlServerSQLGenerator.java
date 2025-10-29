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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL Server implementation of {@link SqlGenerator}.
 *
 * @version 1.0.0
 * @since 2025-09-07
 */
public class SqlServerSQLGenerator implements SqlGenerator {
    private static final int MAX_IDENTIFIER_LENGTH = 128;
    private static final Pattern PRECISION_PATTERN = Pattern.compile("\\((\\d+)\\)");
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String COLUMN_INDENT = "    ";

    @Override
    public String generate(List<TableDTO> tables) {
        StringBuilder builder = new StringBuilder();
        for (TableDTO table : safeList(tables)) {
            builder.append(buildTableSql(table));
        }
        return builder.toString();
    }

    private String buildTableSql(TableDTO table) {
        StringBuilder builder = new StringBuilder();
        String escapedTableName = escapeIdentifier(table.getTableName());

        List<List<String>> primaryKeys = collectPrimaryKeys(table);
        Set<String> pkColumns = extractPrimaryKeyColumns(primaryKeys);
        List<String> extendedProperties = new ArrayList<>();

        builder.append("CREATE TABLE ")
                .append(escapedTableName)
                .append(" (")
                .append(LINE_SEPARATOR)
                .append(String.join("," + LINE_SEPARATOR,
                        buildColumnDefinitions(table, pkColumns, escapedTableName, extendedProperties)));
        appendPrimaryKey(builder, primaryKeys, table.getTableName());
        builder.append(LINE_SEPARATOR)
                .append(");")
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR);

        appendIndexes(builder, table, escapedTableName);
        appendTableComment(table, escapedTableName, extendedProperties);
        appendForeignKeys(builder, table, escapedTableName);
        appendExtendedProperties(builder, extendedProperties);

        return builder.toString();
    }

    private List<String> buildColumnDefinitions(TableDTO table, Set<String> pkColumns, String escapedTableName,
                                                List<String> extendedProperties) {
        List<String> definitions = new ArrayList<>();
        for (ColumnDTO column : safeList(table.getColumns())) {
            definitions.add(buildColumnDefinition(column, pkColumns));
            if (column.getComment() != null && !column.getComment().isBlank()) {
                extendedProperties.add(addExtendedProperty(escapedTableName, column.getComment(),
                        "COLUMN", escapeIdentifier(column.getColumnName())));
            }
        }
        return definitions;
    }

    private String buildColumnDefinition(ColumnDTO column, Set<String> pkColumns) {
        StringBuilder builder = new StringBuilder();
        String columnName = escapeIdentifier(column.getColumnName());
        builder.append(COLUMN_INDENT)
                .append(columnName)
                .append(' ')
                .append(buildSqlServerDataType(column));

        if (pkColumns.contains(column.getColumnName()) || Boolean.FALSE.equals(column.getNullable())) {
            builder.append(" NOT NULL");
        } else {
            builder.append(" NULL");
        }

        if (Boolean.TRUE.equals(column.getAutoIncrement())) {
            builder.append(" IDENTITY(1,1)");
        }

        if (column.getDefaultValue() != null && !column.getDefaultValue().isBlank()) {
            builder.append(" DEFAULT ")
                    .append(formatSqlServerDefaultValue(column.getDefaultValue(), column.getDataType()));
        }

        return builder.toString();
    }

    private void appendPrimaryKey(StringBuilder builder, List<List<String>> primaryKeys, String tableName) {
        if (primaryKeys.isEmpty()) {
            return;
        }
        List<String> keyGroup = primaryKeys.get(0);
        String pkColumns = keyGroup.stream()
                .map(this::expressOrEscape)
                .collect(Collectors.joining(", "));
        String pkName = normalizeConstraintName("PK_" + tableName);
        builder.append(LINE_SEPARATOR)
                .append(",")
                .append(LINE_SEPARATOR)
                .append(COLUMN_INDENT)
                .append("CONSTRAINT ")
                .append(pkName)
                .append(" PRIMARY KEY (")
                .append(pkColumns)
                .append(")");
    }

    private void appendIndexes(StringBuilder builder, TableDTO table, String escapedTableName) {
        for (IndexDTO index : safeList(table.getIndexes())) {
            if (Boolean.TRUE.equals(index.getPk())) {
                continue;
            }
            String indexNamePart = index.getIndexName();
            if (indexNamePart == null || indexNamePart.isBlank()) {
                indexNamePart = String.join("_", safeList(index.getColumnNames()));
            }
            String indexName = normalizeConstraintName("IDX_" + table.getTableName() + "_" + indexNamePart);
            String columns = safeList(index.getColumnNames()).stream()
                    .map(this::expressOrEscape)
                    .collect(Collectors.joining(", "));

            builder.append("CREATE ");
            if (Boolean.TRUE.equals(index.getUnique())) {
                builder.append("UNIQUE ");
            }
            builder.append("INDEX ")
                    .append(indexName)
                    .append(" ON ")
                    .append(escapedTableName)
                    .append(" (")
                    .append(columns)
                    .append(");")
                    .append(LINE_SEPARATOR);

            if (index.getComment() != null && !index.getComment().isBlank()) {
                builder.append(addExtendedProperty(escapedTableName, index.getComment(), "INDEX", indexName))
                        .append(LINE_SEPARATOR);
            }
            builder.append(LINE_SEPARATOR);
        }
    }

    private void appendTableComment(TableDTO table, String escapedTableName, List<String> extendedProperties) {
        if (table.getComment() != null && !table.getComment().isBlank()) {
            extendedProperties.add(addExtendedProperty(escapedTableName, table.getComment(), null, null));
        }
    }

    private void appendExtendedProperties(StringBuilder builder, List<String> extendedProperties) {
        for (String property : extendedProperties) {
            builder.append(property)
                    .append(LINE_SEPARATOR);
        }
        if (!extendedProperties.isEmpty()) {
            builder.append(LINE_SEPARATOR);
        }
    }

    private void appendForeignKeys(StringBuilder builder, TableDTO table, String escapedTableName) {
        for (ForeignKeyDTO fk : safeList(table.getForeignKeys())) {
            if (!isValidForeignKey(table, fk)) {
                continue;
            }
            String fkNameSeed = "FK_" + table.getTableName() + "_" + fk.getReferencedTable();
            String constraintName = normalizeConstraintName(fkNameSeed);
            String localColumns = safeList(fk.getColumnNames()).stream()
                    .map(this::expressOrEscape)
                    .collect(Collectors.joining(", "));
            String referencedColumns = safeList(fk.getReferencedColumnNames()).stream()
                    .map(this::expressOrEscape)
                    .collect(Collectors.joining(", "));

            builder.append("ALTER TABLE ")
                    .append(escapedTableName)
                    .append(" ADD CONSTRAINT ")
                    .append(constraintName)
                    .append(" FOREIGN KEY (")
                    .append(localColumns)
                    .append(") REFERENCES ")
                    .append(escapeIdentifier(fk.getReferencedTable()))
                    .append(" (")
                    .append(referencedColumns)
                    .append(")");

            if (fk.getOnUpdateAction() != null) {
                builder.append(" ON UPDATE ")
                        .append(mapSqlServerReferentialAction(fk.getOnUpdateAction()));
            }
            if (fk.getOnDeleteAction() != null) {
                builder.append(" ON DELETE ")
                        .append(mapSqlServerReferentialAction(fk.getOnDeleteAction()));
            }
            builder.append(";")
                    .append(LINE_SEPARATOR)
                    .append(LINE_SEPARATOR);
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

    private String expressOrEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.startsWith("`") && value.endsWith("`")) {
            return value.substring(1, value.length() - 1);
        }
        return escapeIdentifier(value);
    }

    private static String addExtendedProperty(String tableName, String comment, String level2Type, String level2Name) {
        String unescapedTableName = tableName.replace("[", "").replace("]", "");
        String unescapedLevel2Name = level2Name != null ? level2Name.replace("[", "").replace("]", "") : null;
        String baseSql = "EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'%s', "
                + "@level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', "
                + "@level1name = N'%s'%s;";
        String level2Part = "";
        if (level2Type != null && unescapedLevel2Name != null) {
            level2Part = String.format(", @level2type = N'%s', @level2name = N'%s'", level2Type, unescapedLevel2Name);
        }
        return String.format(baseSql, comment.replace("'", "''"), unescapedTableName, level2Part);
    }

    private String normalizeConstraintName(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.replaceAll("[^a-zA-Z0-9_]", "_");
        if (normalized.length() > MAX_IDENTIFIER_LENGTH) {
            normalized = normalized.substring(0, MAX_IDENTIFIER_LENGTH);
        }
        return escapeIdentifier(normalized);
    }

    private String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return "[]";
        }
        return "[" + identifier.replace("]", "]]") + "]";
    }

    private String buildSqlServerDataType(ColumnDTO column) {
        String type = column.getDataType().toLowerCase(Locale.ROOT);
        if ("timestamp".equals(type)) {
            return parsePrecisionType("datetime2", column.getDataType());
        }
        if ("double".equals(type)) {
            return "FLOAT";
        }
        if (type.contains("bool")) {
            return "BIT";
        }
        String charType = mapCharOrText(type, column).orElse(null);
        if (charType != null) {
            return charType;
        }
        String intType = mapInteger(type).orElse(null);
        if (intType != null) {
            return intType;
        }
        String numeric = mapNumeric(type, column).orElse(null);
        if (numeric != null) {
            return numeric;
        }
        String temporal = mapTemporal(type, column).orElse(null);
        if (temporal != null) {
            return temporal;
        }
        return type.toUpperCase(Locale.ROOT);
    }

    private Optional<String> mapCharOrText(String type, ColumnDTO column) {
        if (type.contains("varchar") || type.contains("char")) {
            Integer length = column.getLength();
            if (length == null || length <= 0 || length > 8000) {
                return Optional.of("VARCHAR(MAX)");
            }
            return Optional.of("VARCHAR(" + length + ")");
        }
        if (type.contains("text")) {
            return Optional.of("VARCHAR(MAX)");
        }
        return Optional.empty();
    }

    private Optional<String> mapInteger(String type) {
        if (type.contains("int")) {
            if (type.contains("bigint")) {
                return Optional.of("BIGINT");
            }
            if (type.contains("smallint")) {
                return Optional.of("SMALLINT");
            }
            if (type.contains("tinyint")) {
                return Optional.of("TINYINT");
            }
            return Optional.of("INT");
        }
        return Optional.empty();
    }

    private Optional<String> mapNumeric(String type, ColumnDTO column) {
        if (type.matches("^(decimal|numeric|float|real)$")) {
            StringBuilder numeric = new StringBuilder(type.toUpperCase(Locale.ROOT));
            if (column.getLength() != null && column.getPrecision() != null) {
                numeric.append('(')
                        .append(column.getLength())
                        .append(", ")
                        .append(column.getPrecision())
                        .append(')');
            }
            return Optional.of(numeric.toString());
        }
        return Optional.empty();
    }

    private Optional<String> mapTemporal(String type, ColumnDTO column) {
        if (type.contains("date")) {
            return Optional.of("DATE");
        }
        if (type.contains("time")) {
            return Optional.of(parsePrecisionType("time", column.getDataType()));
        }
        if (type.contains("datetime")) {
            return Optional.of(parsePrecisionType("datetime2", column.getDataType()));
        }
        return Optional.empty();
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

    private String formatSqlServerDefaultValue(String value, String dataType) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "NULL";
        }
        String lowerValue = value.toLowerCase(Locale.ROOT);
        if (lowerValue.startsWith("`") && lowerValue.endsWith("`")) {
            return "(" + lowerValue.substring(1, lowerValue.length() - 1) + ")";
        }
        String lowerType = dataType.toLowerCase(Locale.ROOT);
        if ("current_timestamp".equals(lowerValue) || "now()".equals(lowerValue)) {
            return "(GETDATE())";
        }
        if (lowerType.contains("bool")) {
            return "true".equals(lowerValue) || "1".equals(lowerValue) ? "1" : "0";
        }
        if (lowerType.matches("^(int|bigint|smallint|tinyint|decimal|numeric|float|double|real)")) {
            if (value.matches("^[-+]?\\d*\\.?\\d+$")) {
                return value;
            }
        }
        return "N'" + value.replace("'", "''") + "'";
    }

    private String mapSqlServerReferentialAction(String action) {
        if (action == null) {
            return "NO ACTION";
        }
        return switch (action.toUpperCase(Locale.ROOT)) {
            case "CASCADE" -> "CASCADE";
            case "SET NULL" -> "SET NULL";
            case "SET DEFAULT" -> "SET DEFAULT";
            case "RESTRICT" -> "NO ACTION";
            default -> "NO ACTION";
        };
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
