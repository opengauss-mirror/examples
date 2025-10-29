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
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.opengauss.admin.plugin.dto;

import lombok.Data;

import org.opengauss.admin.plugin.enums.DatabaseDialect;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload describing which tables to export and the target SQL dialect.
 *
 * @version 1.0.0
 * @since 2025-08-30
 */
@Data
public class ExportRequestDTO {
    /** Target dialect (openGauss / mysql / postgre / mssql). */
    private DatabaseDialect dialect;

    /** Table definitions including columns, types, primary keys, and foreign keys. */
    private List<TableDTO> tables = new ArrayList<>();
}
