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

import com.gitee.starblues.bootstrap.annotation.AutowiredType;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.common.core.domain.model.ops.OpsClusterNodeVO;
import org.opengauss.admin.common.core.domain.model.ops.OpsClusterVO;
import org.opengauss.admin.common.core.domain.model.ops.jdbc.JdbcDbClusterNodeVO;
import org.opengauss.admin.common.core.domain.model.ops.jdbc.JdbcDbClusterVO;
import org.opengauss.admin.plugin.domain.ConnectionRequest;
import org.opengauss.admin.plugin.domain.JdbcNodeRef;
import org.opengauss.admin.plugin.dto.ManagedInstanceDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.service.DataSourceService;
import org.opengauss.admin.system.plugin.facade.JdbcDbClusterFacade;
import org.opengauss.admin.system.plugin.facade.OpsFacade;
import org.opengauss.admin.system.service.ops.impl.EncryptionUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;

/**
 * Retrieves connection information for managed database instances and exposes utility operations.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
@Slf4j
@Service
public class DataSourceServiceImpl implements DataSourceService {
    private static final String DEFAULT_SCHEMA = "public";
    private static final Set<String> MYSQL_SYSTEM_DATABASES = Set.of(
            "information_schema", "mysql", "performance_schema", "sys");

    @Resource
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private OpsFacade opsFacade;

    @Resource
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private JdbcDbClusterFacade jdbcDbClusterFacade;

    @Resource
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private EncryptionUtils encryptionUtils;

    @Override
    public Connection getConnection(String clusterId, String nodeId)
            throws SQLException, ClassNotFoundException {
        return getConnection(clusterId, nodeId, null);
    }

    @Override
    public Connection getConnection(String clusterId, String nodeId, String schemaName)
            throws SQLException, ClassNotFoundException {
        ConnectionRequest request = findConnectionRequest(clusterId, nodeId)
                .map(details -> details.withSchema(schemaName))
                .orElseThrow(() -> new SQLException(String.format(Locale.ROOT,
                        "Unable to locate database instance for clusterId='%s', nodeId='%s'.",
                        clusterId, nodeId)));
        return openConnection(request);
    }

    @Override
    public Connection getDatabaseConnection(String clusterId, String nodeId, String databaseName)
            throws SQLException, ClassNotFoundException {
        ConnectionRequest request = findConnectionRequest(clusterId, nodeId)
                .map(details -> details.withDatabase(databaseName).withSchema(DEFAULT_SCHEMA))
                .orElseThrow(() -> new SQLException(String.format(Locale.ROOT,
                        "Unable to locate database instance for clusterId='%s', nodeId='%s'.",
                        clusterId, nodeId)));
        return openConnection(request);
    }

    @Override
    public List<ManagedInstanceDTO> listManagedInstances() {
        List<ManagedInstanceDTO> instances = new ArrayList<>();
        instances.addAll(buildManagedInstancesFromOps());
        instances.addAll(buildManagedInstancesFromJdbc());
        return instances;
    }

    @Override
    public List<String> listSchemas(String clusterId, String nodeId, String databaseName)
            throws SQLException, ClassNotFoundException {
        DatabaseDialect dialect = getDialectForInstance(clusterId, nodeId)
                .orElseThrow(() -> new SQLException(String.format(Locale.ROOT,
                        "Unable to determine dialect for clusterId='%s', nodeId='%s'.",
                        clusterId, nodeId)));
        String query = buildSchemaQuery(dialect);
        if (query == null) {
            log.warn("Schema listing is not implemented for dialect '{}'.", dialect.name());
            return Collections.emptyList();
        }
        try (Connection connection = getDatabaseConnection(clusterId, nodeId, databaseName);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            List<String> schemas = new ArrayList<>();
            while (resultSet.next()) {
                schemas.add(resultSet.getString(1));
            }
            if (dialect == DatabaseDialect.MYSQL) {
                schemas.removeIf(MYSQL_SYSTEM_DATABASES::contains);
            }
            if ((dialect == DatabaseDialect.POSTGRESQL || dialect == DatabaseDialect.OPENGAUSS)
                    && schemas.isEmpty()) {
                schemas.add(DEFAULT_SCHEMA);
            }
            return schemas;
        }
    }

    @Override
    public void createSchemaIfAbsent(String clusterId, String nodeId, String databaseName, String schemaName)
            throws SQLException, ClassNotFoundException {
        String normalized = normalizeSchemaName(schemaName);
        if (normalized == null) {
            throw new IllegalArgumentException("Schema name must not be empty.");
        }
        if (!normalized.matches("[A-Za-z_][A-Za-z0-9_\\$]*")) {
            throw new IllegalArgumentException("Schema name must start with a letter or underscore and contain only alphanumeric characters, underscores, or '$'.");
        }
        DatabaseDialect dialect = getDialectForInstance(clusterId, nodeId)
                .orElseThrow(() -> new SQLException(String.format(Locale.ROOT,
                        "Unable to determine dialect for clusterId='%s', nodeId='%s'.",
                        clusterId, nodeId)));
        String sql = buildCreateSchemaSql(dialect, normalized);
        if (sql == null || sql.isBlank()) {
            throw new SQLException(String.format(Locale.ROOT,
                    "Schema creation is not supported for dialect '%s'.", dialect.name()));
        }
        try (Connection connection = getDatabaseConnection(clusterId, nodeId, databaseName);
             Statement statement = connection.createStatement()) {
            log.info("Creating schema if absent: clusterId='{}', nodeId='{}', database='{}', schema='{}'",
                    clusterId, nodeId, databaseName, normalized);
            statement.execute(sql);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DatabaseDialect> getDialectForInstance(String clusterId, String nodeId) {
        Optional<DatabaseDialect> opsDialect = locateOpsNode(clusterId, nodeId)
                .map(node -> DatabaseDialect.OPENGAUSS);
        if (opsDialect.isPresent()) {
            return opsDialect;
        }

        for (DatabaseDialect dialect : DatabaseDialect.values()) {
            boolean isMatch = jdbcDbClusterFacade.listAll(dialect.getDialectName()).stream()
                    .filter(cluster -> matchesCluster(cluster, clusterId))
                    .flatMap(cluster -> safeList(cluster.getNodes()).stream())
                    .anyMatch(node -> nodeId.equals(node.getClusterNodeId()));
            if (isMatch) {
                return Optional.of(dialect);
            }
        }
        return Optional.empty();
    }

    private Optional<ConnectionRequest> findConnectionRequest(String clusterId, String nodeId) {
        Optional<ConnectionRequest> opsRequest = locateOpsNode(clusterId, nodeId)
                .map(node -> ConnectionRequest.builder()
                        .dialect(DatabaseDialect.OPENGAUSS)
                        .host(node.getPublicIp())
                        .port(node.getDbPort())
                        .database(node.getDbName())
                        .username(node.getDbUser())
                        .password(encryptionUtils.decrypt(node.getDbUserPassword()))
                        .schema(DEFAULT_SCHEMA)
                        .build());
        if (opsRequest.isPresent()) {
            return opsRequest;
        }

        for (DatabaseDialect dialect : DatabaseDialect.values()) {
            Optional<ConnectionRequest> jdbcMatch = locateJdbcNode(dialect, clusterId, nodeId)
                    .flatMap(node -> buildJdbcConnectionRequest(dialect, node));
            if (jdbcMatch.isPresent()) {
                return jdbcMatch;
            }
        }
        return Optional.empty();
    }

    private Optional<OpsClusterNodeVO> locateOpsNode(String clusterId, String nodeId) {
        return opsFacade.listCluster().stream()
                .filter(cluster -> clusterId.equals(cluster.getClusterId()))
                .map(OpsClusterVO::getClusterNodes)
                .filter(nodes -> nodes != null)
                .flatMap(Collection::stream)
                .filter(node -> nodeId.equals(node.getNodeId()))
                .findFirst();
    }

    private Optional<JdbcNodeRef> locateJdbcNode(DatabaseDialect dialect, String clusterId, String nodeId) {
        return jdbcDbClusterFacade.listAll(dialect.getDialectName()).stream()
                .filter(cluster -> matchesCluster(cluster, clusterId))
                .flatMap(cluster -> safeList(cluster.getNodes()).stream()
                        .filter(node -> nodeId.equals(node.getClusterNodeId()))
                        .map(node -> new JdbcNodeRef(cluster, node)))
                .findFirst();
    }

    private boolean matchesCluster(JdbcDbClusterVO cluster, String clusterId) {
        String identifier = cluster.getClusterId() != null ? cluster.getClusterId() : cluster.getName();
        return clusterId.equals(identifier) || clusterId.equals(cluster.getName());
    }

    private Optional<ConnectionRequest> buildJdbcConnectionRequest(DatabaseDialect dialect, JdbcNodeRef ref) {
        JdbcDbClusterNodeVO node = ref.node();
        String rawPort = node.getPort();
        if (rawPort == null || rawPort.isBlank()) {
            log.warn("Port is missing for JDBC node '{}' in cluster '{}'.",
                    node.getClusterNodeId(), ref.cluster().getName());
            return Optional.empty();
        }
        int port;
        try {
            port = Integer.parseInt(rawPort);
        } catch (NumberFormatException ex) {
            log.warn("Port '{}' is not numeric for JDBC node '{}' in cluster '{}'.",
                    rawPort, node.getClusterNodeId(), ref.cluster().getName(), ex);
            return Optional.empty();
        }
        String database = extractDatabaseName(node.getUrl()).orElse(DEFAULT_SCHEMA);
        String decryptedPassword = encryptionUtils.decrypt(node.getPassword());
        return Optional.of(ConnectionRequest.builder()
                .dialect(dialect)
                .host(node.getIp())
                .port(port)
                .database(database)
                .username(node.getUsername())
                .password(decryptedPassword)
                .schema(DEFAULT_SCHEMA)
                .build());
    }

    private Connection openConnection(ConnectionRequest request)
            throws SQLException, ClassNotFoundException {
        String driverClass = getDriverClassName(request.dialect());
        String jdbcUrl = buildJdbcUrl(request);
        log.debug("Opening JDBC connection using URL {}", jdbcUrl);
        Class.forName(driverClass);
        return DriverManager.getConnection(jdbcUrl, request.username(), request.password());
    }

    private String getDriverClassName(DatabaseDialect dialect) {
        return switch (dialect) {
            case OPENGAUSS, POSTGRESQL -> "org.postgresql.Driver";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MSSQL -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        };
    }

    private String buildJdbcUrl(ConnectionRequest request) {
        String schema = Optional.ofNullable(request.schema())
                .filter(s -> !s.isBlank())
                .orElse(DEFAULT_SCHEMA);
        return switch (request.dialect()) {
            case OPENGAUSS, POSTGRESQL -> String.format(Locale.ROOT,
                    "jdbc:postgresql://%s:%d/%s?currentSchema=%s",
                    request.host(), request.port(), request.database(), schema);
            case MYSQL -> String.format(Locale.ROOT,
                    "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    request.host(), request.port(), request.database());
            case MSSQL -> String.format(Locale.ROOT,
                    "jdbc:sqlserver://%s:%d;databaseName=%s",
                    request.host(), request.port(), request.database());
        };
    }

    private List<ManagedInstanceDTO> buildManagedInstancesFromOps() {
        List<ManagedInstanceDTO> instances = new ArrayList<>();
        for (OpsClusterVO cluster : safeList(opsFacade.listCluster())) {
            for (OpsClusterNodeVO node : safeList(cluster.getClusterNodes())) {
                appendOpsInstances(instances, cluster, node);
            }
        }
        return instances;
    }

    private List<ManagedInstanceDTO> buildManagedInstancesFromJdbc() {
        List<ManagedInstanceDTO> instances = new ArrayList<>();
        for (DatabaseDialect dialect : DatabaseDialect.values()) {
            List<JdbcDbClusterVO> clusters = jdbcDbClusterFacade.listAll(dialect.getDialectName());
            for (JdbcDbClusterVO cluster : safeList(clusters)) {
                String identifier = cluster.getClusterId() != null ? cluster.getClusterId() : cluster.getName();
                for (JdbcDbClusterNodeVO node : safeList(cluster.getNodes())) {
                    appendJdbcInstances(instances, dialect, cluster, identifier, node);
                }
            }
        }
        return instances;
    }

    private void appendOpsInstances(List<ManagedInstanceDTO> instances, OpsClusterVO cluster, OpsClusterNodeVO node) {
        List<String> databases = listDatabasesForNode(cluster.getClusterId(), node.getNodeId());
        if (databases.isEmpty()) {
            String displayName = String.format(
                    Locale.ROOT,
                    "%s - %s (%s) [node]",
                    cluster.getClusterName(),
                    node.getHostname(),
                    node.getPublicIp());
            instances.add(new ManagedInstanceDTO(
                    cluster.getClusterId(),
                    node.getNodeId(),
                    displayName,
                    DatabaseDialect.OPENGAUSS.getDialectName(),
                    node.getDbName()));
            return;
        }
        for (String database : databases) {
            String displayName = String.format(
                    Locale.ROOT,
                    "%s - %s / %s",
                    cluster.getClusterName(),
                    node.getHostname(),
                    database);
            instances.add(new ManagedInstanceDTO(
                    cluster.getClusterId(),
                    node.getNodeId(),
                    displayName,
                    DatabaseDialect.OPENGAUSS.getDialectName(),
                    database));
        }
    }

    private void appendJdbcInstances(
            List<ManagedInstanceDTO> instances,
            DatabaseDialect dialect,
            JdbcDbClusterVO cluster,
            String identifier,
            JdbcDbClusterNodeVO node) {
        List<String> databases = listDatabasesForNode(identifier, node.getClusterNodeId());
        if (databases.isEmpty()) {
            String displayName = String.format(Locale.ROOT, "%s (%s) [node]", cluster.getName(), node.getIp());
            String database = extractDatabaseName(node.getUrl()).orElse(DEFAULT_SCHEMA);
            instances.add(new ManagedInstanceDTO(
                    identifier,
                    node.getClusterNodeId(),
                    displayName,
                    dialect.getDialectName(),
                    database));
            return;
        }
        for (String database : databases) {
            String displayName = String.format(
                    Locale.ROOT,
                    "%s (%s) / %s",
                    cluster.getName(),
                    node.getIp(),
                    database);
            instances.add(new ManagedInstanceDTO(
                    identifier,
                    node.getClusterNodeId(),
                    displayName,
                    dialect.getDialectName(),
                    database));
        }
    }

    private List<String> listDatabasesForNode(String clusterId, String nodeId) {
        Optional<DatabaseDialect> dialectOpt = getDialectForInstance(clusterId, nodeId);
        if (dialectOpt.isEmpty()) {
            log.warn("Unable to determine dialect for clusterId='{}', nodeId='{}'.", clusterId, nodeId);
            return Collections.emptyList();
        }
        DatabaseDialect dialect = dialectOpt.get();
        String query = buildDatabaseQuery(dialect);
        if (query == null) {
            log.warn("Database listing is not implemented for dialect '{}'.", dialect.name());
            return Collections.emptyList();
        }
        try (Connection connection = getConnection(clusterId, nodeId);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            List<String> databases = new ArrayList<>();
            while (resultSet.next()) {
                databases.add(resultSet.getString(1));
            }
            if (dialect == DatabaseDialect.MYSQL) {
                databases.removeIf(MYSQL_SYSTEM_DATABASES::contains);
            }
            getDefaultDatabaseForNode(clusterId, nodeId)
                    .filter(db -> !databases.contains(db))
                    .ifPresent(db -> databases.add(0, db));
            return databases;
        } catch (SQLException | ClassNotFoundException ex) {
            log.warn("Failed to obtain database list for clusterId='{}', nodeId='{}'.", clusterId, nodeId, ex);
            return Collections.emptyList();
        }
    }

    private String buildDatabaseQuery(DatabaseDialect dialect) {
        return switch (dialect) {
            case MYSQL -> "SHOW DATABASES;";
            case POSTGRESQL, OPENGAUSS ->
                    "SELECT datname FROM pg_database WHERE datistemplate = false AND datname <> 'postgres';";
            case MSSQL -> "SELECT name FROM sys.databases;";
        };
    }

    private String buildSchemaQuery(DatabaseDialect dialect) {
        return switch (dialect) {
            case MYSQL -> "SHOW DATABASES;";
            case POSTGRESQL, OPENGAUSS ->
                    "SELECT nspname FROM pg_namespace "
                            + "WHERE nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') "
                            + "AND nspname NOT LIKE 'pg_temp_%' AND nspname NOT LIKE 'pg_toast_temp_%' "
                            + "ORDER BY nspname;";
            case MSSQL -> "SELECT name FROM sys.schemas WHERE principal_id = 1;";
        };
    }

    private Optional<String> getDefaultDatabaseForNode(String clusterId, String nodeId) {
        Optional<String> opsDefault = locateOpsNode(clusterId, nodeId)
                .map(OpsClusterNodeVO::getDbName);
        if (opsDefault.isPresent()) {
            return opsDefault;
        }
        for (DatabaseDialect dialect : DatabaseDialect.values()) {
            Optional<String> jdbcDefault = locateJdbcNode(dialect, clusterId, nodeId)
                    .flatMap(ref -> extractDatabaseName(ref.node().getUrl()));
            if (jdbcDefault.isPresent()) {
                return jdbcDefault;
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractDatabaseName(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        int databaseNameMarker = url.indexOf(";databaseName=");
        if (databaseNameMarker >= 0) {
            int start = databaseNameMarker + 15;
            if (start < url.length()) {
                int end = url.indexOf(';', start);
                String candidate = end == -1 ? url.substring(start) : url.substring(start, end);
                return candidate.isBlank() ? Optional.empty() : Optional.of(candidate);
            }
            return Optional.empty();
        }
        int slash = url.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < url.length()) {
            int queryIndex = url.indexOf('?', slash);
            String candidate = queryIndex == -1 ? url.substring(slash + 1) : url.substring(slash + 1, queryIndex);
            return candidate.isBlank() ? Optional.empty() : Optional.of(candidate);
        }
        return Optional.empty();
    }

    private String normalizeSchemaName(String schemaName) {
        if (schemaName == null) {
            return null;
        }
        String trimmed = schemaName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildCreateSchemaSql(DatabaseDialect dialect, String schemaName) {
        return switch (dialect) {
            case POSTGRESQL, OPENGAUSS -> "CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(schemaName);
            case MYSQL -> "CREATE SCHEMA IF NOT EXISTS `" + schemaName.replace("`", "``") + "`";
            case MSSQL -> """
                    IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = '%s')
                    BEGIN
                        EXEC('CREATE SCHEMA [%s]');
                    END
                    """.formatted(schemaName.replace("'", "''"), schemaName.replace("]", "]]"));
        };
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
