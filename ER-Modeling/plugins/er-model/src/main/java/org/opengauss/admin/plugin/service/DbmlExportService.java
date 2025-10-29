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
 * Service contract for converting DBML inputs into SQL or PlantUML outputs.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public interface DbmlExportService {
    /**
     * Generate SQL for the given DBML text and target dialect.
     *
     * @param dbmlText DBML definition payload
     * @param dialect target database dialect (e.g., "mysql", "postgresql", "opengauss")
     * @return SQL script generated from the DBML
     */
    String generateSqlFromDbml(String dbmlText, DatabaseDialect dialect);

    /**
     * Export PlantUML markup directly from DBML content.
     *
     * @param dbmlText DBML definition payload
     * @return PlantUML text built from the DBML
     */
    String generatePlantUmlFromDbml(String dbmlText);
}
