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
import org.opengauss.admin.plugin.dto.ExportRequestDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.service.DbmlExportService;
import org.opengauss.admin.plugin.service.ExportService;
import org.opengauss.admin.plugin.service.SchemaIntrospectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

/**
 * REST endpoints that convert DBML definitions or live schemas into SQL and PlantUML exports.
 *
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@RestController
@RequestMapping("/export")
public class ExportController {
    private final ExportService exportService;
    private final DbmlExportService dbmlExportService;
    private final SchemaIntrospectionService schemaIntrospectionService;

    @Autowired
    public ExportController(ExportService exportService,
                            DbmlExportService dbmlExportService,
                            SchemaIntrospectionService schemaIntrospectionService) {
        this.exportService = exportService;
        this.dbmlExportService = dbmlExportService;
        this.schemaIntrospectionService = schemaIntrospectionService;
    }

    /**
     * Export SQL content based on the provided export request.
     *
     * @param dto export configuration including target dialect and tables
     * @return Base64-encoded SQL file payload wrapped in {@link AjaxResult}
     */
    @PostMapping("/sql")
    public AjaxResult exportSql(@RequestBody ExportRequestDTO dto) {
        try {
            File sqlFile = exportService.generateSqlFile(dto);
            byte[] fileBytes = Files.readAllBytes(sqlFile.toPath());
            // Encode the file bytes as Base64 (or adapt to whatever the front end expects)
            // Here we assume the front end can consume Base64 data
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            return AjaxResult.success("Export succeeded", base64Data);
        } catch (IOException e) {
            log.error("Failed to export SQL file", e);
            return AjaxResult.error("Failed to export SQL file: " + e.getMessage());
        }
    }

    /**
     * Export SQL by translating DBML content for the specified dialect.
     *
     * @param dbmlText DBML script uploaded by the client
     * @param dialect  target database dialect
     * @return generated SQL snippet wrapped in {@link AjaxResult}
     */
    @PostMapping(value = "/sql-from-dbml", consumes = "text/plain", produces = "application/json;charset=UTF-8")
    public AjaxResult exportSqlFromDbml(
            @RequestBody String dbmlText,
            @RequestParam("dialect") DatabaseDialect dialect) {
        if (dialect == null) {
            return AjaxResult.error("Database dialect must be provided.");
        }
        try {
            String generatedSql = dbmlExportService.generateSqlFromDbml(dbmlText, dialect);
            return AjaxResult.success(Map.of("sqlContent", generatedSql));
        } catch (IllegalArgumentException e) {
            return AjaxResult.error("Invalid parameter: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate SQL from DBML", e);
            return AjaxResult.error("Failed to generate SQL from DBML: " + e.getMessage());
        }
    }

    /**
     * Export PlantUML markup generated from DBML content used in the ER diagram editor.
     *
     * @param dbmlContent DBML source uploaded by the client
     * @return PlantUML markup wrapped in {@link AjaxResult}
     */
    @PostMapping("/plantuml-from-dbml")
    public AjaxResult exportPlantUMLFromDbml(@RequestBody String dbmlContent) {
        try {
            String plantUML = dbmlExportService.generatePlantUmlFromDbml(dbmlContent);
            return AjaxResult.success("success", plantUML);
        } catch (IllegalArgumentException iae) {
            log.warn("Invalid DBML content: {}", iae.getMessage());
            return AjaxResult.error("Invalid DBML content: " + iae.getMessage());
        } catch (Exception e) {
            log.error("Export PlantUML from DBML failed", e);
            return AjaxResult.error("Export PlantUML from DBML failed: " + e.getMessage());
        }
    }

    /**
     * Export PlantUML markup by introspecting a live database schema.
     *
     * @param clusterId cluster identifier
     * @param nodeId node identifier
     * @param databaseName database name to introspect
     * @param schemaName schema name to introspect
     * @return PlantUML markup wrapped in {@link AjaxResult}
     */
    @GetMapping("/plantuml-from-db")
    public AjaxResult exportPlantUMLFromDb(@RequestParam String clusterId,
                                           @RequestParam String nodeId,
                                           @RequestParam String databaseName,
                                           @RequestParam String schemaName) {
        try {
            String plantUML = schemaIntrospectionService.generatePlantUmlFromDb(
                    clusterId, nodeId, databaseName, schemaName);
            return AjaxResult.success("success", plantUML);
        } catch (IllegalArgumentException iae) {
            log.warn("Invalid params: {}", iae.getMessage());
            return AjaxResult.error("Invalid params: " + iae.getMessage());
        } catch (Exception e) {
            log.error("Export PlantUML from DB failed", e);
            return AjaxResult.error("Export PlantUML from DB failed: " + e.getMessage());
        }
    }
}
