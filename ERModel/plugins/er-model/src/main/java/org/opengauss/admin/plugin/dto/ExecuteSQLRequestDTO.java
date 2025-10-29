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

package org.opengauss.admin.plugin.dto;

import lombok.Data;

import org.opengauss.admin.plugin.enums.DatabaseDialect;

/**
 * Request payload for executing SQL generated from DBML against a target instance.
 *
 * @version 1.0.0
 * @since 2025-10-15
 */
@Data
public class ExecuteSQLRequestDTO {
    // Target instance identifiers
    private String clusterId;
    private String nodeId;

    // Target dialect and schema
    private DatabaseDialect dialect;
    private String targetSchema;

    // Raw DBML payload
    private String dbmlText;
}
