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

import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.common.core.domain.AjaxResult;
import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.service.DataSourceService;
import org.opengauss.admin.plugin.service.SchemaIntrospectionService;
import org.opengauss.admin.plugin.translate.SqlToDbmlTranslator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for ER diagram features.
 * Performs reverse engineering to produce the data required for ER diagrams.
 *
 * @version 1.0.0
 * @since 2025-09-10
 */
@Slf4j
@RestController
@RequestMapping("/diagram")
public class DiagramController {
    private final SchemaIntrospectionService schemaIntrospectionService;
    private final DataSourceService dataSourceService;

    public DiagramController(SchemaIntrospectionService schemaIntrospectionService,
            DataSourceService dataSourceService) {
        this.schemaIntrospectionService = schemaIntrospectionService;
        this.dataSourceService = dataSourceService;
    }

    /**
     * Reverse-engineer the specified database/schema into a DBML string.
     *
     * @param clusterId    cluster identifier
     * @param nodeId       node identifier
     * @param databaseName database to introspect
     * @param schemaName   schema to introspect
     * @return AjaxResult containing DBML text and dialect information
     */
    @GetMapping("/get-dbml-schema")
    public AjaxResult getSchemaAsDbml(
            @RequestParam("clusterId") String clusterId,
            @RequestParam("nodeId") String nodeId,
            @RequestParam("databaseName") String databaseName,
            @RequestParam("schemaName") String schemaName) {
        log.info("Received DBML generation request -> clusterId={}, nodeId={}, databaseName={}, schema={}",
                clusterId, nodeId, databaseName, schemaName);
        try {
            // 1) Resolve the dialect
            Optional<DatabaseDialect> dialectOpt = dataSourceService.getDialectForInstance(clusterId, nodeId);
            if (dialectOpt.isEmpty()) {
                return AjaxResult.error("Unable to resolve database dialect for the instance.");
            }
            DatabaseDialect dialect = dialectOpt.get();

            // 2) Reverse-engineer DTOs
            List<TableDTO> tables = schemaIntrospectionService.getSchemaAsDTOs(
                    clusterId,
                    nodeId,
                    databaseName,
                    schemaName);
            if (tables == null || tables.isEmpty()) {
                log.warn("No tables found in schema: {}", schemaName);
                return AjaxResult.success(Map.of("dbmlString", "", "dialect", dialect.getDialectName()));
            }

            // 3) Generate DBML (with dialect info)
            String dbml = SqlToDbmlTranslator.generate(tables);
            return AjaxResult.success(Map.of(
                    "dbmlString", dbml,
                    "dialect", dialect.getDialectName()
            ));
        } catch (java.sql.SQLException | ClassNotFoundException ex) {
            log.error("Database access error during reverse engineering", ex);
            return AjaxResult.error("Database error: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid reverse engineering request: {}", ex.getMessage());
            return AjaxResult.error("Invalid request: " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Failed to generate DBML: clusterId={}, nodeId={}, schema={}", clusterId, nodeId, schemaName, ex);
            return AjaxResult.error("Failed to generate DBML: " + ex.getMessage());
        }
    }
}
