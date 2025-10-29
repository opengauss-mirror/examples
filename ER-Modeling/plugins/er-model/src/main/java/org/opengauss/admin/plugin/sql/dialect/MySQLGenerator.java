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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL generator for the MySQL dialect (supports enum/set, precision/scale, unsigned columns, and self-referencing FKs).
 *
 * @since 2025-09-07
 */
public class MySQLGenerator implements SqlGenerator {
    private static final Set<String> GEOMETRY_TYPES =
            new HashSet<>(Arrays.asList("geometry", "point", "linestring", "polygon",
                    "multipoint", "multilinestring", "multipolygon", "geometrycollection"));

    /* -------- 0. Entry point -------- */
    @Override
    public String generate(List<TableDTO> tables) {
        StringBuilder ddl = new StringBuilder();

        for (TableDTO table : tables) {
            /* 0-A Resolve primary keys (column flags take precedence) */
            List<String> pkCols = resolvePrimaryKey(table);

            /* 1) CREATE TABLE header */
            ddl.append("CREATE TABLE `").append(table.getTableName()).append("` (\n");

            /* 2) Columns / PK / indexes / inline FK */
            List<String> defs = new ArrayList<>();
            table.getColumns().forEach(c -> defs.add(buildColumnDef(c)));

            if (!pkCols.isEmpty()) {
                String pk = pkCols.stream().map(this::wrapBacktick).collect(Collectors.joining(", "));
                defs.add("  PRIMARY KEY (" + pk + ")");
            }

            if (table.getIndexes() != null) {
                for (IndexDTO idx : table.getIndexes()) {
                    defs.add(buildIndexDef(table.getTableName(), idx));
                }
            }

            Set<String> sigSet = new HashSet<>();
            for (ForeignKeyDTO fk : table.getForeignKeys()) {
                if (!isValidForeignKey(table, fk)) {
                    continue;
                }
                String sig = fk.getColumnNames()
                        + "->"
                        + fk.getReferencedTable()
                        + "."
                        + fk.getReferencedColumnNames();
                if (!sigSet.add(sig)) {
                    continue;
                }
                defs.add(buildForeignKeyDef(table.getTableName(), fk));
            }

            ddl.append(String.join(",\n", defs)).append("\n)")
                    .append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            if (StringUtils.hasText(table.getComment())) {
                ddl.append(" COMMENT='").append(escapeSqlLiteral(table.getComment())).append("'");
            }
            ddl.append(";\n\n");
        }
        return ddl.toString();
    }

    /* -------- 1. Foreign key validation -------- */
    private boolean isValidForeignKey(TableDTO table, ForeignKeyDTO fk) {
        // Allow self-references; only verify that referenced columns exist locally
        return fk.getColumnNames().stream()
                .allMatch(cn -> table.getColumns().stream()
                        .anyMatch(col -> col.getColumnName().equals(cn)));
    }

    /* -------- 2. Column definitions -------- */
    private String buildColumnDef(ColumnDTO c) {
        StringBuilder sb = new StringBuilder("  `").append(c.getColumnName()).append("` ")
                .append(resolveDataType(c));

        if (Boolean.FALSE.equals(c.getNullable())) {
            sb.append(" NOT NULL");
        }
        if (Boolean.TRUE.equals(c.getAutoIncrement())) {
            sb.append(" AUTO_INCREMENT");
        }

        if (c.getDefaultValue() != null && !c.getDefaultValue().isBlank()) {
            sb.append(" DEFAULT ").append(formatDefaultValue(c.getDefaultValue()));
        } else if (Boolean.TRUE.equals(c.getNullable())) {
            sb.append(" DEFAULT NULL");
        } else {
            ; // No Default value needed for nullable columns
        }

        if (Boolean.TRUE.equals(c.getUnique())) {
            sb.append(" UNIQUE");
        }

        if (StringUtils.hasText(c.getComment())) {
            sb.append(" COMMENT '").append(escapeSqlLiteral(c.getComment())).append("'");
        }
        return sb.toString();
    }

    /* -- 2-A Type mapping (length / precision / scale / enum / unsigned) -- */
    private String resolveDataType(ColumnDTO column) {
        String type = column.getDataType().toLowerCase(Locale.ROOT);
        boolean isUnsigned = Boolean.TRUE.equals(column.getUnsigned());

        if ("enum".equals(type) || "set".equals(type)) {
            return buildEnumOrSetType(type, column);
        }
        if (isIntegerType(type)) {
            return withUnsigned(type.toUpperCase(Locale.ROOT), isUnsigned);
        }
        if (isDecimalType(type)) {
            return buildDecimalType(type, column, isUnsigned);
        }
        if (isFloatingType(type)) {
            return buildFloatingType(type, column, isUnsigned);
        }
        Optional<String> lengthBased = resolveLengthBasedType(type, column);
        if (lengthBased.isPresent()) {
            return lengthBased.get();
        }
        Optional<String> temporal = resolveTemporalType(type, column.getScale());
        if (temporal.isPresent()) {
            return temporal.get();
        }
        if (GEOMETRY_TYPES.contains(type)) {
            return type.toUpperCase(Locale.ROOT);
        }
        return "TEXT";
    }

    private boolean isIntegerType(String type) {
        switch (type) {
            case "tinyint":
            case "smallint":
            case "mediumint":
            case "int":
            case "bigint":
                return true;
            default:
                return false;
        }
    }

    private boolean isDecimalType(String type) {
        return "decimal".equals(type) || "numeric".equals(type);
    }

    private boolean isFloatingType(String type) {
        return "float".equals(type) || "double".equals(type);
    }

    private String buildDecimalType(String type, ColumnDTO column, boolean isUnsigned) {
        String keyword = "decimal".equals(type) ? "DECIMAL" : "NUMERIC";
        return withUnsigned(buildNumericType(keyword, column.getPrecision(), column.getScale()), isUnsigned);
    }

    private String buildFloatingType(String type, ColumnDTO column, boolean isUnsigned) {
        String keyword = type.toUpperCase(Locale.ROOT);
        return withUnsigned(buildNumericType(keyword, column.getPrecision(), column.getScale()), isUnsigned);
    }

    private Optional<String> resolveLengthBasedType(String type, ColumnDTO column) {
        switch (type) {
            case "char":
                return Optional.of(buildLengthType("CHAR", column.getLength(), 1));
            case "varchar":
                return Optional.of(buildLengthType("VARCHAR", column.getLength(), 255));
            case "binary":
                return Optional.of(buildLengthType("BINARY", column.getLength(), 1));
            case "varbinary":
                return Optional.of(buildLengthType("VARBINARY", column.getLength(), 255));
            case "bit":
                return Optional.of(buildLengthType("BIT", column.getLength(), 1));
            default:
                return Optional.empty();
        }
    }

    private Optional<String> resolveTemporalType(String type, Integer scale) {
        switch (type) {
            case "date":
                return Optional.of("DATE");
            case "year":
                return Optional.of("YEAR");
            case "time":
                return Optional.of(buildTemporalType("TIME", scale));
            case "datetime":
                return Optional.of(buildTemporalType("DATETIME", scale));
            case "timestamp":
                return Optional.of(buildTemporalType("TIMESTAMP", scale));
            default:
                return Optional.empty();
        }
    }

    private String buildEnumOrSetType(String type, ColumnDTO column) {
        List<String> values = column.getEnumValues();
        if (values == null || values.isEmpty()) {
            return type.toUpperCase(Locale.ROOT);
        }
        String joined = values.stream()
                .map(value -> "'" + escapeSqlLiteral(value) + "'")
                .collect(Collectors.joining(","));
        return type.toUpperCase(Locale.ROOT) + "(" + joined + ")";
    }

    private String withUnsigned(String sqlType, boolean isUnsigned) {
        if (isUnsigned) {
            return sqlType + " UNSIGNED";
        }
        return sqlType;
    }

    private String buildNumericType(String keyword, Integer precision, Integer scale) {
        StringBuilder sb = new StringBuilder(keyword);
        if (precision != null) {
            sb.append("(").append(precision);
            if (scale != null) {
                sb.append(",").append(scale);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    private String buildLengthType(String keyword, Integer length, int defaultLength) {
        int size = length != null ? length.intValue() : defaultLength;
        return keyword + "(" + size + ")";
    }

    private String buildTemporalType(String keyword, Integer scale) {
        if (scale == null) {
            return keyword;
        }
        return keyword + "(" + scale + ")";
    }

    /* -------- 3. Index definitions -------- */
    private String buildIndexDef(String table, IndexDTO idx) {
        String idxName = idx.getIndexName();
        if (!StringUtils.hasText(idxName)) {
            String cols = String.join("_", idx.getColumnNames()).replaceAll("[`()\\s]", "");
            idxName = "idx_" + table + "_" + cols;
        }

        StringBuilder sb = new StringBuilder("  ");
        if (Boolean.TRUE.equals(idx.getUnique())) {
            sb.append("UNIQUE ");
        }
        sb.append("KEY `").append(idxName).append("` (");

        String colsExpr = idx.getColumnNames().stream()
                .map(this::renderIndexKey)
                .collect(Collectors.joining(", "));
        sb.append(colsExpr).append(")");

        if (StringUtils.hasText(idx.getIndexType())) {
            sb.append(" USING ").append(idx.getIndexType().toUpperCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /* -------- 4. Foreign key definitions -------- */
    private String buildForeignKeyDef(String table, ForeignKeyDTO fk) {
        String fkName = "fk_" + table + "_" + fk.getColumnNames().get(0) + "_" + fk.getReferencedTable();

        String localCols = fk.getColumnNames().stream()
                .map(this::wrapBacktick).collect(Collectors.joining(", "));
        String refCols = fk.getReferencedColumnNames().stream()
                .map(this::wrapBacktick).collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder("  CONSTRAINT ")
                .append(wrapBacktick(fkName))
                .append(" FOREIGN KEY (").append(localCols).append(")")
                .append(" REFERENCES `").append(fk.getReferencedTable()).append("`(").append(refCols).append(")");

        if (fk.getOnUpdateAction() != null) {
            sb.append(" ON UPDATE ").append(fk.getOnUpdateAction());
        }
        if (fk.getOnDeleteAction() != null) {
            sb.append(" ON DELETE ").append(fk.getOnDeleteAction());
        }

        return sb.toString();
    }

    /* -------- 5. Helper methods -------- */
    private List<String> resolvePrimaryKey(TableDTO table) {
        List<String> pk = table.getColumns().stream()
                .filter(c -> Boolean.TRUE.equals(c.getPrimaryKey()))
                .map(ColumnDTO::getColumnName).collect(Collectors.toList());

        if (pk.isEmpty() && table.getIndexes() != null) {
            for (Iterator<IndexDTO> it = table.getIndexes().iterator(); it.hasNext(); ) {
                IndexDTO idx = it.next();
                if (Boolean.TRUE.equals(idx.getPk())) {
                    pk.addAll(idx.getColumnNames());
                    it.remove();    // PK already handled; avoid duplication
                    break;
                }
            }
        }
        return pk;
    }

    private String wrapBacktick(String id) {
        return "`" + id + "`";
    }

    private String escapeSqlLiteral(String txt) {
        return txt.replace("'", "''");
    }

    private String formatDefaultValue(String v) {
        if (v == null) {
            return "NULL";
        }
        if (v.matches("^[a-zA-Z_]+\\(.*\\)$")) {
            return v;
        }
        if (isNumericLiteral(v)) {
            return v;
        }
        return "'" + escapeSqlLiteral(v) + "'";
    }

    private boolean isNumericLiteral(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    // Determine whether the value is an expression (or a DBML backtick-wrapped expression)
    // Also exclude MySQL prefix-length indexes such as column(10)
    private boolean isExpression(String s) {
        if (s == null) {
            return false;
        }
        String x = s.trim();

        // Backticks wrap expressions in DBML conventions: `lower(title)`, `date(created_at)`, etc.
        if (x.length() >= 2 && x.startsWith("`") && x.endsWith("`")) {
            return true;
        }

        // Column prefix notation: col(10) indicates a column, not an expression
        if (x.matches("^[A-Za-z_][A-Za-z0-9_]*\\(\\d+\\)$")) {
            return false;
        }

        // Heuristically treat parentheses, whitespace, operators, JSON arrows, or casts as expressions
        return x.startsWith("(") || x.endsWith(")")
                || x.contains("(") || x.contains(")") || x.contains(" ")
                || x.contains("+") || x.contains("-") || x.contains("*") || x.contains("/")
                || x.contains("->") || x.contains("->>") || x.contains("::") || x.contains(",");
    }

    // MySQL 8 requires function/expression index key parts to be wrapped in parentheses: (expr)
    // Do not add another pair if it already exists
    private String ensureParenWrapped(String expr) {
        String x = expr.trim();
        if (x.startsWith("(") && x.endsWith(")")) {
            return x;
        }
        return "(" + x + ")";
    }

    // Render index keys: keep expression parentheses, backtick identifiers, preserve prefix length `col`(len)
    private String renderIndexKey(String s) {
        if (s == null) {
            return "";
        }
        String x = s.trim();

        // Remove DBML backticks used to mark expressions
        if (x.length() >= 2 && x.startsWith("`") && x.endsWith("`")) {
            x = x.substring(1, x.length() - 1);
        }

        // Prefix length: name(20)
        if (x.matches("^([A-Za-z_][A-Za-z0-9_]*)\\((\\d+)\\)$")) {
            String col = x.replaceAll("^([A-Za-z_][A-Za-z0-9_]*)\\((\\d+)\\)$", "$1");
            String len = x.replaceAll("^([A-Za-z_][A-Za-z0-9_]*)\\((\\d+)\\)$", "$2");
            return "`" + col + "`(" + len + ")";
        }

        if (isExpression(x)) {
            // Expressions: MySQL 8 requires (expr)
            return ensureParenWrapped(x);
        }
        // Plain column name
        return "`" + x + "`";
    }
}
