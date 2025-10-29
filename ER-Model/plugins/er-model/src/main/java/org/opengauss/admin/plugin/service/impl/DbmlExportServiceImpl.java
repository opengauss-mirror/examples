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

import com.wn.dbml.compiler.DbmlParser;
import com.wn.dbml.compiler.ParsingException;
import com.wn.dbml.model.Database;

import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.exception.DbmlProcessingException;
import org.opengauss.admin.plugin.service.DbmlExportService;
import org.opengauss.admin.plugin.sql.router.SqlGeneratorRouter;
import org.opengauss.admin.plugin.translate.DbmlToDtoTranslator;
import org.opengauss.admin.plugin.util.PlantUMLGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation that converts DBML inputs into SQL or PlantUML outputs.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
@Service
public class DbmlExportServiceImpl implements DbmlExportService {
    @Override
    public String generateSqlFromDbml(String dbmlText, DatabaseDialect dialect) {
        if (dbmlText == null || dbmlText.isBlank()) {
            throw new IllegalArgumentException("DBML content cannot be empty.");
        }

        try {
            // 1. Parse the DBML text via the dbml-java library
            Database database = DbmlParser.parse(dbmlText);

            // 2. Map dbml-java models into the project's DTOs
            List<TableDTO> tableDTOs = DbmlToDtoTranslator.map(database);

            // 3. Use the existing generator to produce SQL
            return SqlGeneratorRouter.generateSQL(tableDTOs, dialect);
        } catch (ParsingException e) {
            // Convert library-specific exceptions into a generic runtime failure
            throw new DbmlProcessingException("Failed to parse DBML: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePlantUmlFromDbml(String dbmlText) {
        if (dbmlText == null || dbmlText.isBlank()) {
            throw new IllegalArgumentException("DBML content cannot be empty.");
        }
        Database database = DbmlParser.parse(dbmlText);
        List<TableDTO> tableDTOs = DbmlToDtoTranslator.map(database);
        return PlantUMLGenerator.generate(tableDTOs);
    }
}
