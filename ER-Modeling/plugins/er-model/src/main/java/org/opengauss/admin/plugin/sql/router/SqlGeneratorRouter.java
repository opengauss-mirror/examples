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

package org.opengauss.admin.plugin.sql.router;

import org.opengauss.admin.plugin.dto.TableDTO;
import org.opengauss.admin.plugin.enums.DatabaseDialect;
import org.opengauss.admin.plugin.sql.core.SqlGenerator;
import org.opengauss.admin.plugin.sql.dialect.MySQLGenerator;
import org.opengauss.admin.plugin.sql.dialect.OpenGaussSqlGenerator;
import org.opengauss.admin.plugin.sql.dialect.PostgreSQLGenerator;
import org.opengauss.admin.plugin.sql.dialect.SqlServerSQLGenerator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central dispatcher that selects the proper SQL generator based on the database dialect.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public class SqlGeneratorRouter {
    // Map keyed by DatabaseDialect; EnumMap is optimized for enum keys
    private static final Map<DatabaseDialect, SqlGenerator> GENERATOR_MAP = new EnumMap<>(DatabaseDialect.class);

    static {
        GENERATOR_MAP.put(DatabaseDialect.MYSQL, new MySQLGenerator());
        GENERATOR_MAP.put(DatabaseDialect.POSTGRESQL, new PostgreSQLGenerator());
        GENERATOR_MAP.put(DatabaseDialect.MSSQL, new SqlServerSQLGenerator());
        GENERATOR_MAP.put(DatabaseDialect.OPENGAUSS, new OpenGaussSqlGenerator());
    }

    /**
     * Generate SQL statements using the generator that matches the dialect.
     *
     * @param tables  table structure definitions
     * @param dialect database dialect
     * @return SQL string produced by the dialect-specific generator
     */
    public static String generateSQL(List<TableDTO> tables, DatabaseDialect dialect) {
        SqlGenerator generator = GENERATOR_MAP.get(dialect);

        if (generator == null) {
            // This should not occur if every dialect is initialized above, but keep the guard for new enums.
            throw new UnsupportedOperationException(
                    "No SQL generator is configured for dialect " + dialect.name() + '.');
        }

        return generator.generate(tables);
    }
}
