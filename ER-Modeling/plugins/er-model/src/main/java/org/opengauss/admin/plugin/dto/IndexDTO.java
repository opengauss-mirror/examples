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

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object describing a database index and its attributes.
 *
 * @version 1.0.0
 * @since 2025-08-25
 */
@Data
public class IndexDTO {
    private String indexName; // Index name
    private List<String> columnNames = new ArrayList<>(); // Column list (supports composite indexes)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isUnique; // Whether the index is unique
    private String indexType; // Index type, e.g. btree or hash (openGauss supports btree, gist, etc.)
    private String comment; // Index comment
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isPrimaryKey; // Whether the index represents a primary key

    public Boolean getUnique() {
        return isUnique;
    }

    public void setUnique(Boolean isUnique) {
        this.isUnique = isUnique;
    }

    public Boolean getPk() {
        return isPrimaryKey;
    }

    public void setPk(Boolean isPrimaryKey) {
        this.isPrimaryKey = isPrimaryKey;
    }
}
