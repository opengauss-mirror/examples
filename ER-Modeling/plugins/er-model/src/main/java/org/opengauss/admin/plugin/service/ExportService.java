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

import org.opengauss.admin.plugin.dto.ExportRequestDTO;

import java.io.File;
import java.io.IOException;

/**
 * Service contract for exporting database structures to SQL files.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public interface ExportService {
    /**
     * Generate an SQL file based on the provided export request.
     *
     * @param dto export configuration containing target dialect and tables
     * @return generated SQL file
     * @throws IOException when file generation fails
     */
    File generateSqlFile(ExportRequestDTO dto) throws IOException;
}
