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

package org.opengauss.admin.plugin.controller;

import com.wn.dbml.compiler.DbmlParser;
import com.wn.dbml.compiler.ParsingException;
import com.wn.dbml.model.Database;

import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.common.core.domain.AjaxResult;
import org.opengauss.admin.plugin.dto.CreateSchemaRequest;
import org.opengauss.admin.plugin.dto.ExecuteSQLRequestDTO;
import org.opengauss.admin.plugin.dto.ManagedInstanceDTO;
import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.dto.TestConnectionRequest;
import org.opengauss.admin.plugin.service.DataSourceService;
import org.opengauss.admin.plugin.service.SqlExecutionService;
import org.opengauss.admin.plugin.sql.core.SchemaPreambleBuilder;
import org.opengauss.admin.plugin.sql.router.SqlGeneratorRouter;
import org.opengauss.admin.plugin.translate.DbmlToDtoTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * REST controller that exposes endpoints for executing DBML-generated SQL against managed instances.
 *
 * @version 1.0.0
 * @since 2025-09-15
 */
@Slf4j
@RestController
@RequestMapping("/execute")
public class ExecuteController {
    private final DataSourceService dataSourceService;
    private final SqlExecutionService sqlExecutionService;

    @Autowired
    public ExecuteController(DataSourceService dataSourceService, SqlExecutionService sqlExecutionService) {
        this.dataSourceService = dataSourceService;
        this.sqlExecutionService = sqlExecutionService;
    }

    /**
     * Validate whether the platform can obtain a JDBC connection to the requested node.
     *
     * @param request connection request payload
     * @return result of the connection attempt
     */
    @PostMapping("/test-connection")
    public AjaxResult testConnection(@RequestBody TestConnectionRequest request) {
        log.info("Received connection test request -> clusterId={}, nodeId={}",
                request.getClusterId(), request.getNodeId());
        try (Connection connection = dataSourceService.getConnection(request.getClusterId(), request.getNodeId())) {
            if (connection != null && !connection.isClosed()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String successMsg = String.format(
                        "Connection established. Product: %s, Version: %s, URL: %s",
                        metaData.getDatabaseProductName(),
                        metaData.getDatabaseProductVersion(),
                        metaData.getURL()
                );
                log.info("Connection test succeeded: {}", successMsg);
                return AjaxResult.success(Map.of("message", successMsg));
            } else {
                log.error("Connection test failed: connection is null or closed.");
                return AjaxResult.error("Obtained connection is null or already closed.");
            }
        } catch (SQLException | ClassNotFoundException e) {
            log.error("Connection test error -> clusterId={}, nodeId={}",
                    request.getClusterId(), request.getNodeId(), e);
            return AjaxResult.error("Failed to acquire connection: " + e.getMessage());
        }
    }

    /**
     * List managed database instances available for execution.
     *
     * @return managed instances
     */
    @GetMapping("/list-instances")
    public AjaxResult listInstances() {
        List<ManagedInstanceDTO> instances = dataSourceService.listManagedInstances();
        return AjaxResult.success(Map.of("instances", instances));
    }

    /**
     * Execute SQL generated from DBML content on the target instance.
     *
     * @param request execution request containing DBML payload and target details
     * @return success or error outcome
     */
    @PostMapping("/from-dbml")
    public AjaxResult executeSqlFromDbml(@RequestBody ExecuteSQLRequestDTO request) {
        try {
            if (request.getDbmlText() == null || request.getDbmlText().isBlank()) {
                return AjaxResult.error("Invalid request. Field 'dbmlText' must be provided.");
            }
            if (request.getDialect() == null) {
                return AjaxResult.error("Database dialect must be provided.");
            }
            if (request.getTargetSchema() == null || request.getTargetSchema().isBlank()) {
                return AjaxResult.error("Target schema name (targetSchema) must be provided.");
            }

            log.info("Received DBML execution request -> clusterId={}, nodeId={}, dialect={}, schema={}",
                    request.getClusterId(), request.getNodeId(),
                    request.getDialect().getDialectName(), request.getTargetSchema());

            Database database = DbmlParser.parse(request.getDbmlText());
            List<TableDTO> tables = DbmlToDtoTranslator.map(database);

            // Build the preamble that creates and switches to the target schema/database
            String preamble = SchemaPreambleBuilder.build(request.getDialect(), request.getTargetSchema());

            // Generate the DDL; it can omit schema prefixes (see MSSQL note)
            String ddl = SqlGeneratorRouter.generateSQL(tables, request.getDialect());

            // Combine into the final script
            String sqlToExecute = preamble + ddl;

            log.info("Generated SQL based on DBML:\n{}", sqlToExecute);

            sqlExecutionService.executeSql(request.getClusterId(), request.getNodeId(), sqlToExecute);

            log.info("SQL executed successfully -> clusterId={}, nodeId={}, schema={}",
                    request.getClusterId(), request.getNodeId(), request.getTargetSchema());
            return AjaxResult.success("SQL executed successfully.");
        } catch (ParsingException e) {
            log.error("Failed to execute SQL: DBML parsing error", e);
            return AjaxResult.error("Failed to parse DBML: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Failed to execute SQL: invalid argument", e);
            return AjaxResult.error(e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            log.error("Failed to execute SQL -> clusterId={}, nodeId={}",
                    request.getClusterId(), request.getNodeId(), e);
            return AjaxResult.error("SQL execution failed: " + e.getMessage());
        }
    }

    /**
     * Create a schema on the specified database if it does not yet exist.
     *
     * @param request schema creation payload
     * @return success or error outcome
     */
    @PostMapping("/create-schema")
    public AjaxResult createSchema(@RequestBody CreateSchemaRequest request) {
        if (request == null) {
            return AjaxResult.error("Request body must be provided.");
        }
        String clusterId = request.getClusterId();
        String nodeId = request.getNodeId();
        String databaseName = request.getDatabaseName();
        String schemaName = request.getSchemaName();

        if (clusterId == null || clusterId.isBlank()
                || nodeId == null || nodeId.isBlank()
                || databaseName == null || databaseName.isBlank()
                || schemaName == null || schemaName.isBlank()) {
            return AjaxResult.error("clusterId, nodeId, databaseName, and schemaName are required.");
        }

        log.info("Received schema creation request -> clusterId={}, nodeId={}, database={}, schema={}",
                clusterId, nodeId, databaseName, schemaName);
        try {
            dataSourceService.createSchemaIfAbsent(clusterId, nodeId, databaseName, schemaName);
            return AjaxResult.success("Schema created or already exists.");
        } catch (IllegalArgumentException ex) {
            return AjaxResult.error(ex.getMessage());
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Failed to create schema -> clusterId={}, nodeId={}, database={}, schema={}",
                    clusterId, nodeId, databaseName, schemaName, ex);
            return AjaxResult.error("Failed to create schema: " + ex.getMessage());
        }
    }

    /**
     * Retrieve schema names for a given database on a managed instance.
     *
     * @param clusterId cluster identifier
     * @param nodeId node identifier
     * @param databaseName database name within the instance
     * @return available schema names
     */
    @GetMapping("/list-schemas")
    public AjaxResult listSchemas(@RequestParam("clusterId") String clusterId,
                                  @RequestParam("nodeId") String nodeId,
                                  @RequestParam("databaseName") String databaseName) {
        log.info("Received schema list request -> clusterId={}, nodeId={}, database={}",
                clusterId, nodeId, databaseName);
        try {
            List<String> schemas = dataSourceService.listSchemas(clusterId, nodeId, databaseName);
            return AjaxResult.success(Map.of("schemas", schemas));
        } catch (SQLException | ClassNotFoundException e) {
            log.error("Failed to list schemas -> clusterId={}, nodeId={}, database={}",
                    clusterId, nodeId, databaseName, e);
            return AjaxResult.error("Failed to list schemas: " + e.getMessage());
        }
    }
}
