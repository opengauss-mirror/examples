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

package org.opengauss.admin.plugin.service;

import org.opengauss.admin.plugin.dto.ManagedInstanceDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contract for obtaining database connections and metadata from the managed platform.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public interface DataSourceService {
    /**
     * Obtain a database connection from the base platform using cluster and node identifiers.
     *
     * @param clusterId cluster identifier
     * @param nodeId    node identifier
     * @return ready-to-use {@link Connection}
     * @throws SQLException           when database access fails or the instance cannot be located
     * @throws ClassNotFoundException when the required JDBC driver is missing
     */
    Connection getConnection(String clusterId, String nodeId) throws SQLException, ClassNotFoundException;

    /**
     * Obtain a connection scoped to the given schema.
     *
     * @param clusterId cluster identifier
     * @param nodeId node identifier
     * @param schemaName schema to target
     * @return ready-to-use {@link Connection}
     * @throws SQLException when database access fails or the instance cannot be located
     * @throws ClassNotFoundException when the required JDBC driver is missing
     */
    Connection getConnection(String clusterId, String nodeId, String schemaName)
            throws SQLException, ClassNotFoundException;

    /**
     * Obtain a connection scoped to the given database.
     *
     * @param clusterId cluster identifier
     * @param nodeId node identifier
     * @param databaseName database to target
     * @return ready-to-use {@link Connection}
     * @throws SQLException when database access fails or the instance cannot be located
     * @throws ClassNotFoundException when the required JDBC driver is missing
     */
    Connection getDatabaseConnection(String clusterId, String nodeId, String databaseName)
            throws SQLException, ClassNotFoundException;

    /**
     * List every database instance that the base platform manages.
     *
     * @return managed instance metadata
     */
    List<ManagedInstanceDTO> listManagedInstances();

    /**
     * List schemas available for the specified cluster and node.
     *
     * @param clusterId cluster identifier
     * @param nodeId    node identifier
     * @param databaseName database name to query
     * @return schema names
     * @throws SQLException when database access fails
     * @throws ClassNotFoundException when the JDBC driver is not present
     */
    List<String> listSchemas(String clusterId, String nodeId, String databaseName)
            throws SQLException, ClassNotFoundException;

    /**
     * Create a schema on the specified database if it does not already exist.
     *
     * @param clusterId cluster identifier
     * @param nodeId node identifier
     * @param databaseName database within the instance
     * @param schemaName schema name to create
     * @throws SQLException when database access fails
     * @throws ClassNotFoundException when the JDBC driver is not present
     */
    void createSchemaIfAbsent(String clusterId, String nodeId, String databaseName, String schemaName)
            throws SQLException, ClassNotFoundException;

    /**
     * Resolve the dialect associated with a managed cluster node.
     *
     * @param clusterId cluster identifier
     * @param nodeId node identifier
     * @return {@link Optional} containing the dialect when available
     */
    Optional<DatabaseDialect> getDialectForInstance(String clusterId, String nodeId);
}
