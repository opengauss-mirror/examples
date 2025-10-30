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

package org.opengauss.admin.plugin.service.impl;

import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.plugin.dto.ColumnDTO;
import org.opengauss.admin.plugin.dto.ForeignKeyDTO;
import org.opengauss.admin.plugin.dto.IndexDTO;
import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.service.DataSourceService;
import org.opengauss.admin.plugin.service.SchemaIntrospectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Introspects database schemas across supported dialects and translates them into {@link TableDTO} structures.
 *
 * @since 2025-01-07
 */
@Slf4j
@Service
public class SchemaIntrospectionServiceImpl implements SchemaIntrospectionService {
    private final DataSourceService dataSourceService;

    @Autowired
    public SchemaIntrospectionServiceImpl(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @Override
    public List<TableDTO> getSchemaAsDTOs(
            String clusterId,
            String nodeId,
            String databaseName,
            String schemaName) throws SQLException, ClassNotFoundException {
        Optional<DatabaseDialect> dialectOpt = dataSourceService.getDialectForInstance(clusterId, nodeId);
        DatabaseDialect dialect = dialectOpt.orElseThrow(
                () -> new UnsupportedOperationException(
                        "无法确定实例的数据库方言: " + clusterId + "/" + nodeId));

        try (Connection connection = dataSourceService.getDatabaseConnection(clusterId, nodeId, databaseName)) {
            Map<String, TableDTO> tablesByName = new HashMap<>();

            switch (dialect) {
                case POSTGRESQL:
                case OPENGAUSS:
                    introspectPostgresSchema(connection, schemaName, tablesByName);
                    break;
                case MYSQL:
                    introspectMySqlSchema(connection, databaseName, tablesByName);
                    break;
                case MSSQL:
                    throw new UnsupportedOperationException("尚未实现对 MSSQL 的模式内省。");
                default:
                    throw new UnsupportedOperationException("不支持的数据库类型: " + dialect.name());
            }

            for (TableDTO table : tablesByName.values()) {
                table.getColumns()
                        .sort(Comparator.comparing((ColumnDTO column) ->
                                Boolean.TRUE.equals(column.getPrimaryKey())).reversed());
            }

            return new ArrayList<>(tablesByName.values());
        }
    }

    private void introspectPostgresSchema(
            Connection conn,
            String schema,
            Map<String, TableDTO> tablesMap) throws SQLException {
        queryPgTablesAndColumns(conn, schema, tablesMap);
        queryPgPrimaryKeysAndUniqueConstraints(conn, schema, tablesMap);
        queryPgForeignKeys(conn, schema, tablesMap);
        queryPgIndexes(conn, schema, tablesMap);
    }

    private void introspectMySqlSchema(
            Connection conn,
            String databaseName,
            Map<String, TableDTO> tablesMap) throws SQLException {
        queryMySqlTablesAndColumns(conn, databaseName, tablesMap);
        queryMySqlPrimaryKeysAndUniqueConstraints(conn, databaseName, tablesMap);
        queryMySqlForeignKeys(conn, databaseName, tablesMap);
        queryMySqlIndexes(conn, databaseName, tablesMap);
    }


    // --- PostgreSQL / openGauss introspection ---

    private void queryPgTablesAndColumns(
            Connection conn,
            String schema,
            Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    t.relname AS table_name,
                    tbl_d.description AS table_comment,
                    a.attname AS column_name,
                    format_type(a.atttypid, a.atttypmod) AS data_type,
                    NOT a.attnotnull AS is_nullable,
                    pg_get_expr(ad.adbin, ad.adrelid) AS column_default,
                    col_d.description AS column_comment
                FROM pg_class t
                    JOIN pg_namespace s ON t.relnamespace = s.oid
                    JOIN pg_attribute a ON a.attrelid = t.oid
                    LEFT JOIN pg_description tbl_d ON tbl_d.objoid = t.oid AND tbl_d.objsubid = 0
                    LEFT JOIN pg_description col_d ON col_d.objoid = a.attrelid AND col_d.objsubid = a.attnum
                    LEFT JOIN pg_attrdef ad ON ad.adrelid = t.oid AND ad.adnum = a.attnum
                WHERE t.relkind IN ('r', 'p')
                    AND a.attnum > 0
                    AND NOT a.attisdropped
                    AND s.nspname = ? -- schema
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schema);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    TableDTO table = tablesMap.computeIfAbsent(tableName, k -> new TableDTO());
                    table.setTableName(tableName);
                    table.setComment(rs.getString("table_comment"));
                    ColumnDTO column = new ColumnDTO();
                    column.setColumnName(rs.getString("column_name"));
                    column.setDataType(rs.getString("data_type"));
                    column.setNullable(rs.getBoolean("is_nullable"));
                    column.setDefaultValue(rs.getString("column_default"));
                    column.setComment(rs.getString("column_comment"));

                    table.getColumns().add(column);
                }
            }
        }
    }

    private void queryPgPrimaryKeysAndUniqueConstraints(
            Connection conn,
            String schema,
            Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    t.relname AS table_name,
                    c.contype AS constraint_type,
                    (
                        SELECT array_agg(a.attname)
                        FROM pg_attribute a
                        WHERE a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
                    ) AS columns
                FROM pg_constraint c
                    JOIN pg_class t ON c.conrelid = t.oid
                    JOIN pg_namespace s ON t.relnamespace = s.oid
                WHERE c.contype IN ('p', 'u')
                    AND s.nspname = ? -- schema
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schema);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    TableDTO table = tablesMap.get(tableName);
                    if (table == null) {
                        continue;
                    }

                    String constraintType = rs.getString("constraint_type");
                    Array columnsArray = rs.getArray("columns");
                    if (columnsArray == null) {
                        continue;
                    }
                    String[] columns = (String[]) columnsArray.getArray();

                    for (String colName : columns) {
                        table.getColumns().stream()
                                .filter(c -> c.getColumnName().equals(colName))
                                .findFirst()
                                .ifPresent(c -> {
                                    if ("p".equals(constraintType)) {
                                        c.setPrimaryKey(true);
                                    } else if ("u".equals(constraintType)) {
                                        c.setUnique(true);
                                    } else {
                                        log.debug(
                                                "Unsupported constraint type {} on table {}",
                                                constraintType,
                                                tableName);
                                    }
                                });
                    }
                }
            }
        }
    }

    private void queryPgForeignKeys(
            Connection conn,
            String schema,
            Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    (SELECT c.relname FROM pg_class c WHERE c.oid = con.conrelid) AS source_table,
                    (
                        SELECT array_agg(a.attname)
                        FROM pg_attribute a
                        WHERE a.attrelid = con.conrelid AND a.attnum = ANY(con.conkey)
                    ) AS source_columns,
                    (SELECT c.relname FROM pg_class c WHERE c.oid = con.confrelid) AS target_table,
                    (
                        SELECT array_agg(a.attname)
                        FROM pg_attribute a
                        WHERE a.attrelid = con.confrelid AND a.attnum = ANY(con.confkey)
                    ) AS target_columns
                FROM pg_constraint con
                    JOIN pg_namespace s ON con.connamespace = s.oid
                WHERE con.contype = 'f'
                    AND s.nspname = ? -- schema
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schema);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String sourceTable = rs.getString("source_table");
                    TableDTO table = tablesMap.get(sourceTable);
                    if (table == null) {
                        continue;
                    }

                    Array sourceColumnsArray = rs.getArray("source_columns");
                    Array targetColumnsArray = rs.getArray("target_columns");
                    if (sourceColumnsArray == null || targetColumnsArray == null) {
                        log.debug("Skipping FK on {} due to missing column metadata", sourceTable);
                        continue;
                    }
                    String[] srcCols = (String[]) sourceColumnsArray.getArray();
                    String tgtTable = rs.getString("target_table");
                    String[] tgtCols = (String[]) targetColumnsArray.getArray();

                    if (srcCols.length != tgtCols.length) {
                        log.debug("Skipping FK on {} due to column mismatch", sourceTable);
                        continue;
                    }

                    ForeignKeyDTO fk = new ForeignKeyDTO();
                    fk.setReferencedTable(tgtTable);
                    fk.setColumnNames(Arrays.asList(srcCols));
                    fk.setReferencedColumnNames(Arrays.asList(tgtCols));
                    table.getForeignKeys().add(fk);
                }
            }
        }
    }

    private void queryPgIndexes(Connection conn, String schema, Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    t.relname AS table_name,
                    i.relname AS index_name,
                    a.attname AS column_name,
                    ix.indisunique AS is_unique
                FROM pg_class t
                    JOIN pg_index ix ON t.oid = ix.indrelid
                    JOIN pg_class i ON i.oid = ix.indexrelid
                    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey)
                    JOIN pg_namespace s ON t.relnamespace = s.oid
                WHERE s.nspname = ? -- schema
                    AND ix.indisprimary = false
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schema);
            try (ResultSet rs = pstmt.executeQuery()) {
                Map<String, IndexDTO> indexesMap = new HashMap<>();
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    TableDTO table = tablesMap.get(tableName);
                    if (table == null) {
                        continue;
                    }

                    String indexName = rs.getString("index_name");
                    boolean isUniqueIndex = rs.getBoolean("is_unique");
                    String columnName = rs.getString("column_name");

                    IndexDTO index = indexesMap.computeIfAbsent(tableName + "." + indexName, key -> {
                        IndexDTO newIndex = new IndexDTO();
                        newIndex.setIndexName(indexName);
                        newIndex.setUnique(isUniqueIndex);
                        table.getIndexes().add(newIndex);
                        return newIndex;
                    });
                    index.getColumnNames().add(columnName);
                }
            }
        }
    }

    // --- MySQL introspection ---

    private void queryMySqlTablesAndColumns(
            Connection conn,
            String database,
            Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    t.TABLE_NAME,
                    t.TABLE_COMMENT,
                    c.COLUMN_NAME,
                    c.COLUMN_TYPE,
                    c.IS_NULLABLE,
                    c.COLUMN_DEFAULT,
                    c.COLUMN_COMMENT,
                    c.EXTRA
                FROM information_schema.TABLES t
                    JOIN information_schema.COLUMNS c
                        ON t.TABLE_SCHEMA = c.TABLE_SCHEMA
                        AND t.TABLE_NAME = c.TABLE_NAME
                WHERE t.TABLE_SCHEMA = ? -- database
                    AND t.TABLE_TYPE = 'BASE TABLE'
                ORDER BY c.ORDINAL_POSITION
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, database);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    TableDTO table = tablesMap.computeIfAbsent(tableName, k -> new TableDTO());
                    table.setTableName(tableName);
                    table.setComment(rs.getString("TABLE_COMMENT"));

                    ColumnDTO column = new ColumnDTO();
                    column.setColumnName(rs.getString("COLUMN_NAME"));
                    column.setDataType(rs.getString("COLUMN_TYPE"));
                    column.setNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                    column.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
                    column.setComment(rs.getString("COLUMN_COMMENT"));
                    if ("auto_increment".equalsIgnoreCase(rs.getString("EXTRA"))) {
                        column.setAutoIncrement(true);
                    }
                    table.getColumns().add(column);
                }
            }
        }
    }

    private void queryMySqlPrimaryKeysAndUniqueConstraints(
            Connection conn,
            String database,
            Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    kcu.TABLE_NAME,
                    kcu.COLUMN_NAME,
                    tc.CONSTRAINT_TYPE
                FROM information_schema.KEY_COLUMN_USAGE kcu
                    JOIN information_schema.TABLE_CONSTRAINTS tc
                        ON kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                        AND kcu.TABLE_SCHEMA = tc.TABLE_SCHEMA
                        AND kcu.TABLE_NAME = tc.TABLE_NAME
                WHERE tc.TABLE_SCHEMA = ? -- database
                    AND tc.CONSTRAINT_TYPE IN ('PRIMARY KEY', 'UNIQUE')
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, database);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    TableDTO table = tablesMap.get(tableName);
                    if (table == null) {
                        continue;
                    }

                    String colName = rs.getString("COLUMN_NAME");
                    String constraintType = rs.getString("CONSTRAINT_TYPE");

                    table.getColumns().stream()
                            .filter(c -> c.getColumnName().equals(colName))
                            .findFirst().ifPresent(c -> {
                                if ("PRIMARY KEY".equalsIgnoreCase(constraintType)) {
                                    c.setPrimaryKey(true);
                                } else if ("UNIQUE".equalsIgnoreCase(constraintType)) {
                                    c.setUnique(true);
                                } else {
                                    log.debug(
                                            "Unhandled MySQL constraint type {} on table {}",
                                            constraintType,
                                            tableName);
                                }
                            });
                }
            }
        }
    }

    private void queryMySqlForeignKeys(
            Connection conn,
            String database,
            Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    kcu.TABLE_NAME,
                    kcu.COLUMN_NAME,
                    kcu.REFERENCED_TABLE_NAME,
                    kcu.REFERENCED_COLUMN_NAME
                FROM information_schema.KEY_COLUMN_USAGE kcu
                WHERE kcu.TABLE_SCHEMA = ? -- database
                    AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
                """;

        Map<String, ForeignKeyDTO> cache = new HashMap<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, database);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    TableDTO table = tablesMap.get(tableName);
                    if (table == null) {
                        continue;
                    }

                    String referencedTable = rs.getString("REFERENCED_TABLE_NAME");

                    String key = tableName + "->" + referencedTable;
                    ForeignKeyDTO fk = cache.computeIfAbsent(key, cacheKey -> {
                        ForeignKeyDTO dto = new ForeignKeyDTO();
                        dto.setReferencedTable(referencedTable);
                        table.getForeignKeys().add(dto);
                        return dto;
                    });

                    fk.getColumnNames().add(rs.getString("COLUMN_NAME"));
                    fk.getReferencedColumnNames().add(rs.getString("REFERENCED_COLUMN_NAME"));
                }
            }
        }
    }

    private void queryMySqlIndexes(
            Connection conn,
            String database,
            Map<String, TableDTO> tablesMap) throws SQLException {
        String sql = """
                SELECT
                    TABLE_NAME,
                    INDEX_NAME,
                    COLUMN_NAME,
                    NON_UNIQUE
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = ? -- database
                    AND INDEX_NAME != 'PRIMARY'
                ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, database);
            try (ResultSet rs = pstmt.executeQuery()) {
                Map<String, IndexDTO> indexesMap = new HashMap<>();
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    boolean isUniqueIndex = rs.getInt("NON_UNIQUE") == 0;

                    TableDTO table = tablesMap.get(tableName);
                    if (table == null) {
                        continue;
                    }

                    boolean isLikelyUniqueConstraintIndex = isUniqueIndex && indexName.equals(columnName);
                    if (isLikelyUniqueConstraintIndex) {
                        continue;
                    }

                    IndexDTO index = indexesMap.computeIfAbsent(tableName + "." + indexName, key -> {
                        IndexDTO newIndex = new IndexDTO();
                        newIndex.setIndexName(indexName);
                        newIndex.setUnique(isUniqueIndex);
                        table.getIndexes().add(newIndex);
                        return newIndex;
                    });
                    index.getColumnNames().add(columnName);
                }
            }
        }
    }
}
