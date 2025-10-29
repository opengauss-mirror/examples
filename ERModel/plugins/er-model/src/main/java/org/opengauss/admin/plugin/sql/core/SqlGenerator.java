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

import org.opengauss.admin.plugin.dto.TableDTO;

import java.util.List;

/**
 * Defines the contract for generating dialect-specific SQL from table metadata.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public interface SqlGenerator {
    /**
     * Generate SQL statements that create or alter the provided tables.
     *
     * @param tables table definitions
     * @return SQL text
     */
    String generate(List<TableDTO> tables);
}
