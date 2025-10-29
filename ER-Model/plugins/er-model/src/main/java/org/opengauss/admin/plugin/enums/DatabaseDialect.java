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

package org.opengauss.admin.plugin.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * Supported database dialects used when generating or importing SQL.
 *
 * @version 1.0.0
 * @since 2025-08-20
 */
@Getter
public enum DatabaseDialect {
    // Supported database dialects
    MYSQL("mysql"),
    POSTGRESQL("postgresql"),
    MSSQL("mssql"),
    OPENGAUSS("opengauss");

    private final String dialectName;

    /**
     * Constructor.
     *
     * @param dialectName lowercase representation of the dialect
     */
    DatabaseDialect(String dialectName) {
        this.dialectName = dialectName;
    }

    /**
     * Convert a string value into the corresponding enum constant.
     * This lookup is case-insensitive.
     *
     * @param text string representation of the dialect
     * @return matching {@link DatabaseDialect} constant
     * @throws IllegalArgumentException if no dialect matches the input
     */
    public static DatabaseDialect fromString(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("数据库方言字符串不能为空。");
        }
        return Arrays.stream(values())
                .filter(dialect -> dialect.dialectName.equalsIgnoreCase(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的数据库方言：" + text));
    }
}
