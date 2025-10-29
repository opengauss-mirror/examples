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

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO that describes an instance managed by the ER-Model plugin, including cluster/node identifiers and display info.
 *
 * @since 2025-10-19
 */
@Data
@AllArgsConstructor // Lombok generates a constructor with all fields
public class ManagedInstanceDTO {
    private String clusterId;
    private String nodeId;
    private String displayName; // Friendly name for UI display
    private String dbType;      // Database type, e.g., "openGauss", "MySQL"
    private String databaseName;
}
