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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.common.core.domain.AjaxResult;
import org.opengauss.admin.plugin.dto.SqlToDbmlRequest;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.service.SqlImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoints for importing SQL DDL and translating it into DBML representations.
 *
 * @version 1.0.0
 * @since 2025-09-25
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/import")
public class ImportController {
    private final SqlImportService sqlImportService;

    /**
     * Import SQL (DDL only) and convert it to DBML.
     *
     * @param req request payload containing SQL text and dialect metadata
     * @return {@link AjaxResult} wrapping the generated DBML content
     */
    @PostMapping("/sql-to-dbml")
    public AjaxResult sqlToDbml(@RequestBody SqlToDbmlRequest req) {
        if (req == null || req.getSqlText() == null || req.getSqlText().isBlank()) {
            return AjaxResult.error("SQL text must not be empty.");
        }
        try {
            // Translate front-end dialect string to enum; allow AUTO for backend detection
            DatabaseDialect dialect = resolveDialect(req.getDialect(), req.getSqlText());
            boolean shouldSkipData = req.getSkipData() == null || req.getSkipData(); // Skip data by default

            String dbml = sqlImportService.sqlToDbml(req.getSqlText(), dialect, shouldSkipData);
            return AjaxResult.success(Map.of("dbmlString", dbml));
        } catch (IllegalArgumentException iae) {
            log.warn("SQL-to-DBML parameter error: {}", iae.getMessage());
            return AjaxResult.error("Invalid parameters: " + iae.getMessage());
        } catch (RuntimeException e) {
            log.error("SQL-to-DBML processing failed", e);
            return AjaxResult.error("SQL-to-DBML failed: " + e.getMessage());
        }
    }

    private DatabaseDialect resolveDialect(String atClient, String sql) {
        if (atClient != null && !"AUTO".equalsIgnoreCase(atClient.trim())) {
            return DatabaseDialect.fromString(atClient);
        }
        // AUTO mode: apply a lightweight heuristic detection
        String s = (sql == null ? "" : sql).toLowerCase();
        if (s.contains("`") || s.contains(" engine=") || s.contains(" unsigned")) {
            return DatabaseDialect.MYSQL;
        }
        // Treat openGauss as close to PostgreSQL
        return DatabaseDialect.POSTGRESQL;
    }
}
