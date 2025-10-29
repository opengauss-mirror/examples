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

import java.util.List;

/**
 * Data transfer object representing a single column and its metadata used for SQL generation.
 *
 * @version 1.0.0
 * @since 2025-08-25
 */
@Data
public class ColumnDTO {
    /* -------------- Basic metadata -------------- */
    private String columnName; // Column name
    private String dataType; // Logical data type (varchar / int / enum, already normalised)

    /* -------------- Optional parameters -------------- */
    private Integer length; // Character length or bit width
    private Integer precision; // Numeric precision (p)
    private Integer scale; // Numeric scale (s)

    /* -------------- Constraints and defaults -------------- */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isNullable; // Whether the column is nullable
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isPrimaryKey; // Whether the column participates in the primary key
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isUnique; // Whether the column has a unique constraint
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isAutoIncrement; // Whether the column auto-increments
    private String defaultValue; // Default value (raw text)
    private String comment; // Column comment

    /* -------------- MySQL-specific -------------- */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isUnsigned; // Whether UNSIGNED/ZEROFILL is applied (integer or floating types)
    private List<String> enumValues; // ENUM/SET options

    public Boolean getNullable() {
        return isNullable;
    }

    public void setNullable(Boolean isNullable) {
        this.isNullable = isNullable;
    }

    public Boolean getPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(Boolean isPrimaryKey) {
        this.isPrimaryKey = isPrimaryKey;
    }

    public Boolean getUnique() {
        return isUnique;
    }

    public void setUnique(Boolean isUnique) {
        this.isUnique = isUnique;
    }

    public Boolean getAutoIncrement() {
        return isAutoIncrement;
    }

    public void setAutoIncrement(Boolean isAutoIncrement) {
        this.isAutoIncrement = isAutoIncrement;
    }

    public Boolean getUnsigned() {
        return isUnsigned;
    }

    public void setUnsigned(Boolean isUnsigned) {
        this.isUnsigned = isUnsigned;
    }
}
