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

import org.opengauss.admin.plugin.enums.DatabaseDialect;

/**
 * Service responsible for converting SQL text into DBML by parsing DDL and intermediate DTOs.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public interface SqlImportService {
    /**
     * Convert SQL text into DBML following the pipeline: SQL → cleaned DDL → DTO → DBML.
     *
     * @param sqlText raw SQL provided by the client
     * @param dialect target database dialect for parsing
     * @param shouldSkipData whether INSERT data statements should be ignored
     * @return generated DBML content
     */
    String sqlToDbml(String sqlText, DatabaseDialect dialect, boolean shouldSkipData);
}
