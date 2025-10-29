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

package org.opengauss.admin.plugin.sql.core;

import org.opengauss.admin.plugin.enums.DatabaseDialect;

/**
 * Utility class that generates dialect-specific SQL snippets for ensuring a schema or
 * database exists and is selected before running further DDL statements.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public final class SchemaPreambleBuilder {
    private SchemaPreambleBuilder() {
        // utility class
    }

    /**
     * Produce the SQL preamble that ensures the target schema exists and is active.
     *
     * @param dialect database dialect
     * @param schema  schema or database name
     * @return SQL preamble string (may be empty)
     */
    public static String build(DatabaseDialect dialect, String schema) {
        if (schema == null || schema.isBlank()) {
            return "";
        }

        String preamble;
        switch (dialect) {
            case MYSQL:
                // MySQL: schema equals database
                preamble = "CREATE DATABASE IF NOT EXISTS " + backtick(schema)
                        + ";\nUSE " + backtick(schema) + ";\n";
                break;
            case POSTGRESQL:
            case OPENGAUSS:
                // PostgreSQL/openGauss: schema within current database
                preamble = "CREATE SCHEMA IF NOT EXISTS " + doubleQuote(schema)
                        + ";\nSET search_path TO " + doubleQuote(schema) + ", public;\n";
                break;
            case MSSQL:
                // SQL Server: create database then ensure default schema
                preamble = "IF DB_ID('" + escapeSingle(schema) + "') IS NULL BEGIN\n"
                        + "    CREATE DATABASE " + bracket(schema) + "\nEND\n"
                        + "USE " + bracket(schema) + ";\n"
                        + "IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = '"
                        + escapeSingle(schema) + "') BEGIN\n"
                        + "    EXEC('CREATE SCHEMA " + doubleQuote(schema) + "');\nEND\n";
                break;
            default:
                preamble = "";
                break;
        }
        return preamble;
    }

    private static String doubleQuote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String backtick(String value) {
        return "`" + value.replace("`", "``") + "`";
    }

    private static String bracket(String value) {
        return "[" + value.replace("]", "]]") + "]";
    }

    private static String escapeSingle(String value) {
        return value.replace("'", "''");
    }
}
