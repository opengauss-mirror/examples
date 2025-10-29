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

import org.opengauss.admin.plugin.dto.ColumnDTO;
import org.opengauss.admin.plugin.dto.ForeignKeyDTO;
import org.opengauss.admin.plugin.dto.IndexDTO;
import org.opengauss.admin.plugin.dto.TableDTO;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Convert dialect-agnostic DTOs into DBML.
 * Highlights:
 * - Table and column names use bare identifiers (no double quotes)
 * - Single-column PK (e.g., serial/bigserial) -> column-level [pk, increment]
 * - Composite PK -> indexes { (a, b) [pk] }
 * - Index rules:
 *   - Single column: col [name: 'idx']
 *   - Unique: ("col") [unique, name: '...']
 *   - Expression: (`lower(title)`) [name: '...']
 * - Comments: table note: '...'; column [note: '...']
 * - Default values: numbers literal; strings quoted; expressions wrapped in backticks.
 *
 * @since 2025-01-07
 */
public class SqlToDbmlTranslator {
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    /**
     * Transform the provided table definitions into DBML text.
     *
     * @param tables logical schema description to render
     * @return DBML string for the supplied tables
     */
    public static String generate(List<TableDTO> tables) {
        StringBuilder sb = new StringBuilder(8192);
        List<String> relationships = new ArrayList<>();

        for (TableDTO table : safeList(tables)) {
            appendTableDefinition(sb, relationships, table);
        }

        appendRelationships(sb, relationships);
        return sb.toString();
    }

    private static void appendTableDefinition(StringBuilder sb, List<String> relationships, TableDTO table) {
        String tableName = bare(table.getTableName());
        sb.append("Table ").append(tableName).append(" {\n");

        List<List<String>> primaryKeySets = collectPrimaryKeySets(table);
        Set<String> singleColumnPks = extractSingleColumnPkNames(primaryKeySets);

        appendColumns(sb, table, singleColumnPks);
        appendTableNote(sb, table);
        appendIndexSection(sb, table, primaryKeySets);

        sb.append("}\n\n");

        relationships.addAll(buildRelationships(tableName, table));
    }

    private static List<List<String>> collectPrimaryKeySets(TableDTO table) {
        List<List<String>> primaryKeys = new ArrayList<>();
        for (List<String> pk : safeList(table.getPrimaryKeys())) {
            List<String> normalized = new ArrayList<>();
            for (String column : safeList(pk)) {
                String columnName = bare(column);
                if (!columnName.isEmpty()) {
                    normalized.add(columnName);
                }
            }
            if (!normalized.isEmpty()) {
                primaryKeys.add(normalized);
            }
        }
        return primaryKeys;
    }

    private static Set<String> extractSingleColumnPkNames(List<List<String>> primaryKeySets) {
        Set<String> singleColumnPks = new HashSet<>();
        for (List<String> pk : primaryKeySets) {
            if (pk.size() == 1) {
                singleColumnPks.add(pk.get(0));
            }
        }
        return singleColumnPks;
    }

    private static void appendColumns(StringBuilder sb, TableDTO table, Set<String> singleColumnPks) {
        for (ColumnDTO column : safeList(table.getColumns())) {
            String columnName = bare(column.getColumnName());
            String dataType = formatDataType(column);
            List<String> attributes = buildColumnAttributes(column, columnName, singleColumnPks);

            sb.append("  ")
                    .append(colQuotedIfNeeded(columnName))
                    .append(" ")
                    .append(dataType);

            if (!attributes.isEmpty()) {
                sb.append(" [").append(String.join(", ", attributes)).append("]");
            }
            sb.append("\n");
        }
    }

    private static List<String> buildColumnAttributes(
            ColumnDTO column,
            String columnName,
            Set<String> singleColumnPks) {
        List<String> attributes = new ArrayList<>();
        boolean isColumnPk = Boolean.TRUE.equals(column.getPrimaryKey()) || singleColumnPks.contains(columnName);
        if (isColumnPk) {
            attributes.add("pk");
        }
        if (Boolean.TRUE.equals(column.getAutoIncrement())) {
            attributes.add("increment");
        }
        if (Boolean.TRUE.equals(column.getUnique())) {
            attributes.add("unique");
        }
        if (Boolean.FALSE.equals(column.getNullable())) {
            attributes.add("not null");
        }
        if (StringUtils.hasText(column.getDefaultValue())) {
            String normalizedDefault = normalizeDbmlDefault(column.getDefaultValue());
            if (!normalizedDefault.isEmpty()) {
                attributes.add("default: " + normalizedDefault);
            }
        }
        if (StringUtils.hasText(column.getComment())) {
            attributes.add("note: '" + escapeSingle(column.getComment()) + "'");
        }
        return attributes;
    }

    private static String bare(String s) {
        if (s == null) {
            return "";
        }
        String value = s.trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("`") && value.endsWith("`"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void appendTableNote(StringBuilder sb, TableDTO table) {
        if (StringUtils.hasText(table.getComment())) {
            sb.append("\n  note: '").append(escapeSingle(table.getComment())).append("'\n");
        }
    }

    private static void appendIndexSection(StringBuilder sb, TableDTO table, List<List<String>> primaryKeySets) {
        List<List<String>> compositePks = new ArrayList<>();
        for (List<String> pk : primaryKeySets) {
            if (pk.size() > 1) {
                compositePks.add(pk);
            }
        }
        List<String> indexLines = buildIndexLines(table, compositePks);
        if (indexLines.isEmpty()) {
            return;
        }
        sb.append("\n  indexes {\n");
        for (String line : indexLines) {
            sb.append("    ").append(line).append("\n");
        }
        sb.append("  }\n");
    }

    private static List<String> buildIndexLines(TableDTO table, List<List<String>> compositePks) {
        List<String> indexLines = new ArrayList<>();
        for (List<String> pk : compositePks) {
            indexLines.add("(" + String.join(", ", quoteColumns(pk)) + ") [pk]");
        }
        for (IndexDTO index : safeList(table.getIndexes())) {
            List<String> columns = safeList(index.getColumnNames());
            if (columns.isEmpty()) {
                continue;
            }
            List<String> renderedColumns = renderIndexColumns(columns);
            if (renderedColumns.isEmpty()) {
                continue;
            }
            if (Boolean.TRUE.equals(index.getUnique())) {
                indexLines.add(buildUniqueIndexLine(renderedColumns, index));
            } else {
                indexLines.add(buildRegularIndexLine(renderedColumns, index));
            }
        }
        return indexLines;
    }

    private static List<String> quoteColumns(List<String> columns) {
        List<String> quoted = new ArrayList<>();
        for (String column : columns) {
            quoted.add(colQuotedIfNeeded(column));
        }
        return quoted;
    }

    private static List<String> renderIndexColumns(List<String> columns) {
        List<String> rendered = new ArrayList<>();
        for (String raw : columns) {
            String normalized = raw == null ? "" : raw.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (IDENTIFIER.matcher(normalized).matches()) {
                rendered.add(colQuotedIfNeeded(bare(normalized)));
            } else {
                String tightened = normalized.replaceAll("\\s*\\(\\s*", "(")
                        .replaceAll("\\s*\\)\\s*", ")");
                rendered.add("`" + tightened + "`");
            }
        }
        return rendered;
    }

    private static String buildUniqueIndexLine(List<String> renderedColumns, IndexDTO index) {
        StringBuilder suffix = new StringBuilder("unique");
        String name = stripQuotes(index.getIndexName());
        if (StringUtils.hasText(name)) {
            suffix.append(", name: '").append(escapeSingle(name)).append("'");
        }
        if (renderedColumns.size() == 1 && isPlainIdentifier(renderedColumns.get(0))) {
            return renderedColumns.get(0) + " [" + suffix + "]";
        }
        return "(" + String.join(", ", renderedColumns) + ") [" + suffix + "]";
    }

    private static String buildRegularIndexLine(List<String> renderedColumns, IndexDTO index) {
        String name = escapeSingle(stripQuotes(index.getIndexName()));
        String suffix = "[name: '" + name + "']";
        if (renderedColumns.size() == 1 && isPlainIdentifier(renderedColumns.get(0))) {
            return renderedColumns.get(0) + " " + suffix;
        }
        return "(" + String.join(", ", renderedColumns) + ") " + suffix;
    }

    private static boolean isPlainIdentifier(String value) {
        String candidate = stripBackticks(value);
        return IDENTIFIER.matcher(candidate).matches();
    }

    private static void appendRelationships(StringBuilder sb, List<String> relationships) {
        if (relationships.isEmpty()) {
            return;
        }
        for (String relationship : relationships) {
            sb.append(relationship).append("\n");
        }
    }

    private static List<String> buildRelationships(String tableName, TableDTO table) {
        List<String> relationships = new ArrayList<>();
        for (ForeignKeyDTO foreignKey : safeList(table.getForeignKeys())) {
            List<String> sourceColumns = toBareNames(foreignKey.getColumnNames());
            List<String> targetColumns = toBareNames(foreignKey.getReferencedColumnNames());
            String referencedTable = bare(foreignKey.getReferencedTable());
            if (sourceColumns.isEmpty() || targetColumns.isEmpty() || !StringUtils.hasText(referencedTable)) {
                continue;
            }
            String sourcePart = formatRelationshipSide(tableName, sourceColumns);
            String targetPart = formatRelationshipSide(referencedTable, targetColumns);
            relationships.add("Ref: " + sourcePart + " > " + targetPart);
        }
        return relationships;
    }

    private static List<String> toBareNames(List<String> names) {
        List<String> results = new ArrayList<>();
        for (String name : safeList(names)) {
            String value = bare(name);
            if (!value.isEmpty()) {
                results.add(value);
            }
        }
        return results;
    }

    private static String formatRelationshipSide(String tableName, List<String> columns) {
        if (columns.size() == 1) {
            return tableName + "." + columns.get(0);
        }
        return tableName + ".(" + String.join(", ", columns) + ")";
    }

    private static <T> List<T> safeList(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    private static String colQuotedIfNeeded(String ident) {
        return ident;
    }

    private static String stripQuotes(String s) {
        if (s == null) {
            return "";
        }
        String value = s.trim();
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("`") && value.endsWith("`"))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String stripBackticks(String s) {
        if (s == null) {
            return "";
        }
        String value = s.trim();
        if (value.startsWith("`") && value.endsWith("`")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String escapeSingle(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "\\'");
    }

    private static String formatDataType(ColumnDTO column) {
        String rawType = column.getDataType() == null ? "varchar" : column.getDataType();
        String baseType = toDbmlType(rawType);
        if (column.getEnumValues() != null && !column.getEnumValues().isEmpty()) {
            String enums = column.getEnumValues().stream()
                    .map(value -> "'" + value.replace("'", "\\'") + "'")
                    .collect(Collectors.joining(", "));
            return baseType + "(" + enums + ")";
        }
        if (column.getPrecision() != null) {
            if (column.getScale() != null) {
                return baseType + "(" + column.getPrecision() + "," + column.getScale() + ")";
            }
            return baseType + "(" + column.getPrecision() + ")";
        }
        if (column.getLength() != null) {
            return baseType + "(" + column.getLength() + ")";
        }
        return baseType;
    }

    private static String normalizeDbmlDefault(String dv) {
        if (dv == null) {
            return "";
        }
        String value = dv.trim();
        if (value.isEmpty()) {
            return value;
        }
        if (value.startsWith("'") && value.endsWith("'")) {
            return value;
        }
        if (value.matches("^-?\\d+(\\.\\d+)?$")) {
            return value;
        }
        if (value.startsWith("`") && value.endsWith("`")) {
            return value;
        }
        String tightened = value.replaceAll("\\s*\\(\\s*", "(")
                .replaceAll("\\s*\\)\\s*", ")");
        return "`" + tightened + "`";
    }

    private static String toDbmlType(String raw) {
        if (raw == null) {
            return "text";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        if (normalized.matches("^character varying\\s*\\(\\s*\\d+\\s*\\)$")) {
            return normalized.replaceFirst("^character varying", "varchar").replace(" ", "");
        }
        if (normalized.matches("^character\\s*\\(\\s*\\d+\\s*\\)$")) {
            return normalized.replaceFirst("^character", "char").replace(" ", "");
        }
        String mapped = mapExactType(normalized);
        if (mapped != null) {
            return mapped;
        }
        if (normalized.startsWith("timestamp")) {
            return "timestamp";
        }
        if (normalized.startsWith("time")) {
            return "time";
        }
        if (normalized.startsWith("numeric") || normalized.startsWith("decimal")) {
            return normalized.replace(" ", "");
        }
        if (normalized.startsWith("varchar") || normalized.startsWith("char")) {
            return normalized.replace(" ", "");
        }
        return normalized.replace(" ", "");
    }

    private static String mapExactType(String normalized) {
        return switch (normalized) {
            case "character varying" -> "varchar";
            case "character" -> "char";
            case "integer", "int4" -> "int";
            case "bigint", "int8" -> "bigint";
            case "smallint", "int2" -> "int";
            case "double precision" -> "double";
            case "real" -> "float";
            case "boolean", "bool" -> "bool";
            case "text" -> "text";
            case "date" -> "date";
            case "json", "jsonb" -> "json";
            default -> null;
        };
    }
}
