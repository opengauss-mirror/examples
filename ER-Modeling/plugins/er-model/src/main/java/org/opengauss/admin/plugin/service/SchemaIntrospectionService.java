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

import org.opengauss.admin.plugin.dto.TableDTO;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface for database schema introspection.
 * Responsible for reverse-engineering table definitions from a given database and schema.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public interface SchemaIntrospectionService {
    /**
     * Fetch complete table metadata for the specified database and schema.
     *
     * @param clusterId    cluster identifier
     * @param nodeId       node identifier
     * @param databaseName target database name
     * @param schemaName   schema name
     * @return list of {@link TableDTO} containing table metadata
     * @throws SQLException when database access fails
     * @throws ClassNotFoundException when the JDBC driver cannot be located
     */
    List<TableDTO> getSchemaAsDTOs(String clusterId, String nodeId,
                                   String databaseName, String schemaName)
            throws SQLException, ClassNotFoundException;

    /**
     * Convenience helper that exports PlantUML directly from the live database schema.
     *
     * @param clusterId cluster identifier
     * @param nodeId node identifier
     * @param databaseName target database name
     * @param schemaName schema name
     * @return generated PlantUML markup for the schema
     * @throws SQLException when database access fails
     * @throws ClassNotFoundException when the JDBC driver cannot be located
     */
    default String generatePlantUmlFromDb(String clusterId, String nodeId,
                                          String databaseName, String schemaName)
            throws SQLException, ClassNotFoundException {
        List<TableDTO> tables = getSchemaAsDTOs(clusterId, nodeId, databaseName, schemaName);
        return org.opengauss.admin.plugin.util.PlantUMLGenerator.generate(tables);
    }
}
