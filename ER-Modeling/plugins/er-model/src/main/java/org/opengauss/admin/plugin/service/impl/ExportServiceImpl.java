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

package org.opengauss.admin.plugin.service.impl;

import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.plugin.dto.ExportRequestDTO;
import org.opengauss.admin.plugin.service.ExportService;
import org.opengauss.admin.plugin.sql.router.SqlGeneratorRouter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Default implementation for generating SQL exports based on table metadata.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
@Slf4j
@Service
public class ExportServiceImpl implements ExportService {
    @Override
    public File generateSqlFile(ExportRequestDTO dto) throws IOException {
        String sql = SqlGeneratorRouter.generateSQL(dto.getTables(), dto.getDialect());
        File file = File.createTempFile("export-", ".sql");
        Files.write(file.toPath(), sql.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
