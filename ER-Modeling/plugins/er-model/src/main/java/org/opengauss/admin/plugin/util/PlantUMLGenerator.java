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

package org.opengauss.admin.plugin.util;

import org.opengauss.admin.plugin.dto.ColumnDTO;
import org.opengauss.admin.plugin.dto.ForeignKeyDTO;
import org.opengauss.admin.plugin.dto.IndexDTO;
import org.opengauss.admin.plugin.dto.TableDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders table metadata as a PlantUML ERD diagram using IE notation.
 *
 * @since 2025-01-07
 */
public final class PlantUMLGenerator {
    private record AliasContext(
            Map<String, String> aliasByKey,
            Map<String, Set<String>> primaryKeys,
            Map<String, Set<Set<String>>> uniqueSets,
            Map<String, Map<String, ColumnDTO>> columns,
            Map<String, Set<String>> foreignKeyColumns) {

        String aliasFor(String tableKey) {
            return aliasByKey.get(tableKey);
        }

        Set<String> primaryKeysFor(String tableKey) {
            return primaryKeys.getOrDefault(tableKey, Collections.emptySet());
        }

        Set<Set<String>> uniqueSetsFor(String tableKey) {
            return uniqueSets.getOrDefault(tableKey, Collections.emptySet());
        }

        Map<String, ColumnDTO> columnsFor(String tableKey) {
            return columns.getOrDefault(tableKey, Collections.emptyMap());
        }

        Set<String> foreignKeyColumnsFor(String tableKey) {
            return foreignKeyColumns.getOrDefault(tableKey, Collections.emptySet());
        }
    }

    /**
     * Generate PlantUML entity relationship diagram from table definitions.
     *
     * @param tables the list of table definitions
     * @return PlantUML source code
     */
    public static String generate(List<TableDTO> tables) {
        List<TableDTO> safeTables = tables == null ? List.of() : tables;
        AliasContext context = buildAliasContext(safeTables);

        StringBuilder sb = new StringBuilder(8192);
        appendHeader(sb);
        appendEntities(sb, safeTables, context);
        appendRelationships(sb, safeTables, context);
        sb.append("@enduml\n");
        return sb.toString();
    }

    private static void appendHeader(StringBuilder sb) {
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        sb.append("hide circle\n");
        sb.append("skinparam linetype ortho\n\n");
    }

    private static AliasContext buildAliasContext(List<TableDTO> tables) {
        Map<String, String> aliasByKey = new LinkedHashMap<>();
        int counter = 1;
        for (TableDTO table : tables) {
            String key = tableKey(table);
            if (!aliasByKey.containsKey(key)) {
                aliasByKey.put(key, "T" + counter++);
            }
        }

        Map<String, Set<String>> primaryKeys = new HashMap<>();
        Map<String, Set<Set<String>>> uniqueSets = new HashMap<>();
        Map<String, Map<String, ColumnDTO>> columnsByTable = new HashMap<>();
        Map<String, Set<String>> foreignKeyColumns = new HashMap<>();

        for (TableDTO table : tables) {
            String key = tableKey(table);
            primaryKeys.put(key, calcPrimaryKeyColumns(table));
            uniqueSets.put(key, calcUniqueSets(table));
            columnsByTable.put(key, toColumnMap(table));
            foreignKeyColumns.put(key, collectForeignKeyColumns(table));
        }

        return new AliasContext(aliasByKey, primaryKeys, uniqueSets, columnsByTable, foreignKeyColumns);
    }

    private static void appendEntities(StringBuilder sb, List<TableDTO> tables, AliasContext context) {
        for (TableDTO table : tables) {
            String key = tableKey(table);
            String alias = context.aliasFor(key);
            if (alias == null) {
                continue;
            }

            Set<String> primaryColumns = context.primaryKeysFor(key);
            Set<String> foreignColumns = context.foreignKeyColumnsFor(key);

            sb.append("entity \"").append(escape(key)).append("\" as ").append(alias).append(" {\n");
            for (ColumnDTO column : safe(table.getColumns())) {
                appendColumn(sb, column, primaryColumns, foreignColumns);
            }
            sb.append("}\n\n");
        }
    }

    private static void appendColumn(
            StringBuilder sb,
            ColumnDTO column,
            Set<String> primaryColumns,
            Set<String> foreignColumns) {
        boolean isPrimary = primaryColumns.contains(column.getColumnName())
                || Boolean.TRUE.equals(column.getPrimaryKey());
        boolean isRequired = isPrimary || Boolean.FALSE.equals(column.getNullable());
        boolean isUniqueColumn = Boolean.TRUE.equals(column.getUnique()) && !isPrimary;
        boolean isForeign = foreignColumns.contains(column.getColumnName());

        sb.append("  ").append(isRequired ? "*" : " ");
        sb.append(escape(column.getColumnName())).append(" : ").append(formatType(column));

        List<String> stereotypes = new ArrayList<>();
        if (isPrimary) {
            stereotypes.add("PK");
        }
        if (isUniqueColumn) {
            stereotypes.add("UQ");
        }
        if (isForeign) {
            stereotypes.add("FK");
        }
        if (!stereotypes.isEmpty()) {
            sb.append(" <<").append(String.join(",", stereotypes)).append(">>");
        }

        if (column.getDefaultValue() != null && !column.getDefaultValue().isBlank()) {
            sb.append(" = ").append(formatDefault(column));
        }
        if (column.getComment() != null && !column.getComment().isBlank()) {
            sb.append(" // ").append(escapeInline(column.getComment()));
        }
        sb.append('\n');
    }

    private static void appendRelationships(StringBuilder sb, List<TableDTO> tables, AliasContext context) {
        for (TableDTO child : tables) {
            String childKey = tableKey(child);
            String childAlias = context.aliasFor(childKey);
            if (childAlias == null) {
                continue;
            }

            Map<String, ColumnDTO> childColumns = context.columnsFor(childKey);
            Set<Set<String>> childUniqueSets = context.uniqueSetsFor(childKey);
            Set<String> childPrimary = context.primaryKeysFor(childKey);

            for (ForeignKeyDTO fk : safe(child.getForeignKeys())) {
                List<String> fkColumns = safe(fk.getColumnNames());
                if (fkColumns.isEmpty()) {
                    continue;
                }

                String parentKey = normalizeName(nullToEmpty(fk.getReferencedTable()));
                String parentAlias = context.aliasFor(parentKey);
                if (parentAlias == null) {
                    continue;
                }

                boolean isChildNullable = isAnyNullable(childColumns, fkColumns, childPrimary);
                boolean isChildUnique = isUniqueForeignKey(childUniqueSets, childPrimary, childColumns, fkColumns);

                String leftEndpoint = "||";
                String rightEndpoint = calcRightEndpoint(isChildUnique, isChildNullable);

                sb.append(parentAlias).append(' ')
                        .append(leftEndpoint).append("--").append(rightEndpoint).append(' ')
                        .append(childAlias);

                String tail = buildForeignKeyTail(fk);
                if (!tail.isBlank()) {
                    sb.append(" : \"").append(escape(tail)).append('"');
                }
                sb.append('\n');
            }
        }
    }

    private static String tableKey(TableDTO table) {
        return normalizeName(nullToEmpty(table.getTableName()));
    }

    private static String normalizeName(String value) {
        return nullToEmpty(value).trim();
    }

    private static Map<String, ColumnDTO> toColumnMap(TableDTO table) {
        Map<String, ColumnDTO> columns = new HashMap<>();
        for (ColumnDTO column : safe(table.getColumns())) {
            columns.put(column.getColumnName(), column);
        }
        return columns;
    }

    private static Set<String> calcPrimaryKeyColumns(TableDTO table) {
        Set<String> primary = new LinkedHashSet<>();
        for (List<String> group : safe(table.getPrimaryKeys())) {
            primary.addAll(group);
        }
        for (IndexDTO index : safe(table.getIndexes())) {
            if (Boolean.TRUE.equals(index.getPk())) {
                primary.addAll(safe(index.getColumnNames()));
            }
        }
        for (ColumnDTO column : safe(table.getColumns())) {
            if (Boolean.TRUE.equals(column.getPrimaryKey())) {
                primary.add(column.getColumnName());
            }
        }
        return primary;
    }

    private static Set<Set<String>> calcUniqueSets(TableDTO table) {
        Set<Set<String>> sets = new HashSet<>();
        for (IndexDTO index : safe(table.getIndexes())) {
            if (Boolean.TRUE.equals(index.getUnique()) || Boolean.TRUE.equals(index.getPk())) {
                sets.add(new LinkedHashSet<>(safe(index.getColumnNames())));
            }
        }
        for (List<String> group : safe(table.getPrimaryKeys())) {
            if (!group.isEmpty()) {
                sets.add(new LinkedHashSet<>(group));
            }
        }
        return sets;
    }

    private static Set<String> collectForeignKeyColumns(TableDTO table) {
        Set<String> columns = new HashSet<>();
        for (ForeignKeyDTO fk : safe(table.getForeignKeys())) {
            columns.addAll(safe(fk.getColumnNames()));
        }
        return columns;
    }

    /**
     * Determine whether any child-side foreign key column is nullable.
     *
     * @param columns       metadata keyed by column name
     * @param fkColumns     the columns participating in the foreign key
     * @param childPkColumns child-side primary key columns
     * @return {@code true} if at least one column allows {@code NULL}; {@code false} otherwise
     */
    private static boolean isAnyNullable(
            Map<String, ColumnDTO> columns,
            List<String> fkColumns,
            Set<String> childPkColumns) {
        for (String name : fkColumns) {
            ColumnDTO column = columns.get(name);
            if (column == null) {
                return true;
            }
            if (Boolean.TRUE.equals(column.getPrimaryKey())
                    || (childPkColumns != null && childPkColumns.contains(name))) {
                continue;
            }
            if (!Boolean.FALSE.equals(column.getNullable())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUniqueForeignKey(
            Set<Set<String>> uniqueSets,
            Set<String> pkColumns,
            Map<String, ColumnDTO> columns,
            List<String> fkColumns) {
        Set<String> set = new LinkedHashSet<>(fkColumns);
        if (!pkColumns.isEmpty() && pkColumns.equals(set)) {
            return true;
        }
        if (uniqueSets.contains(set)) {
            return true;
        }
        if (set.size() == 1) {
            ColumnDTO column = columns.get(set.iterator().next());
            return column != null && Boolean.TRUE.equals(column.getUnique());
        }
        return false;
    }

    private static String calcRightEndpoint(boolean isUnique, boolean isNullable) {
        if (isUnique) {
            return isNullable ? "o|" : "||";
        }
        return isNullable ? "o{" : "|{";
    }

    private static String buildForeignKeyTail(ForeignKeyDTO fk) {
        List<String> parts = new ArrayList<>();
        if (fk.getOnDeleteAction() != null && !fk.getOnDeleteAction().isBlank()) {
            parts.add("ON DELETE " + fk.getOnDeleteAction());
        }
        if (fk.getOnUpdateAction() != null && !fk.getOnUpdateAction().isBlank()) {
            parts.add("ON UPDATE " + fk.getOnUpdateAction());
        }
        return String.join(", ", parts);
    }

    private static String formatType(ColumnDTO column) {
        String type = nullToEmpty(column.getDataType());
        String upperType = type.toUpperCase(Locale.ROOT);

        if (column.getEnumValues() != null && !column.getEnumValues().isEmpty()) {
            String enums = column.getEnumValues().stream()
                    .map(value -> "'" + value.replace("'", "''") + "'")
                    .collect(Collectors.joining(","));
            type = upperType + "(" + enums + ")";
        } else if (column.getPrecision() != null) {
            if (column.getScale() != null) {
                type = upperType + "(" + column.getPrecision() + "," + column.getScale() + ")";
            } else {
                type = upperType + "(" + column.getPrecision() + ")";
            }
        } else if (column.getLength() != null) {
            type = upperType + "(" + column.getLength() + ")";
        } else {
            type = upperType;
        }

        if (Boolean.TRUE.equals(column.getUnsigned())) {
            type = type + " UNSIGNED";
        }
        return type;
    }

    /**
     * Format default values, adding quotes for string types while leaving expressions untouched.
     *
     * @param column column metadata
     * @return formatted default value literal
     */
    private static String formatDefault(ColumnDTO column) {
        String defaultValue = column.getDefaultValue();
        if (defaultValue == null) {
            return "";
        }

        String trimmed = defaultValue.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        if (trimmed.length() >= 2 && trimmed.startsWith("`") && trimmed.endsWith("`")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        if (isQuoted(trimmed)
                || looksFunctionCall(trimmed)
                || looksNumeric(trimmed)
                || isBooleanLiteral(trimmed)
                || isNullLiteral(trimmed)) {
            return trimmed;
        }

        if (isStringLike(column.getDataType())) {
            return "'" + trimmed.replace("'", "''") + "'";
        }

        return trimmed;
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        return nullToEmpty(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeInline(String value) {
        return escape(value).replace("\n", " ");
    }

    private static boolean isQuoted(String value) {
        return (value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\""));
    }

    private static boolean looksFunctionCall(String value) {
        return value.matches("(?i)[A-Z_][A-Z0-9_]*\\s*\\(.*\\)");
    }

    private static boolean looksNumeric(String value) {
        return value.matches("[-+]?\\d+(\\.\\d+)?");
    }

    private static boolean isBooleanLiteral(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isNullLiteral(String value) {
        return "null".equalsIgnoreCase(value);
    }

    private static boolean isStringLike(String type) {
        if (type == null) {
            return false;
        }
        String upper = type.toUpperCase(Locale.ROOT);
        return upper.contains("CHAR")
                || upper.contains("TEXT")
                || upper.contains("CLOB")
                || upper.contains("STRING")
                || upper.contains("JSON")
                || upper.contains("UUID");
    }
}
