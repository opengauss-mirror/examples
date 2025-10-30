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

/**
 * Request payload for importing `.sql` content into DBML.
 *
 * @version 1.0.0
 * @since 2025-09-01
 */
@Data
public class SqlToDbmlRequest {
    /** Dialect: POSTGRESQL | MYSQL | OPENGAUSS | AUTO (detected server-side). */
    private String dialect;

    /** Raw SQL (may include DML; backend filters to retain only DDL). */
    private String sqlText;

    /** Skip data during import (defaults to true). */
    private Boolean shouldSkipData;

    /**
     * Compatibility alias for older clients.
     *
     * @return value of {@link #shouldSkipData}
     */
    public Boolean getSkipData() {
        return shouldSkipData;
    }
}
