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

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object describing a database table along with its metadata.
 *
 * @version 1.0.0
 * @since 2025-08-25
 */
@Data
public class TableDTO {
    private String tableName;                      // Table name
    private String comment;                        // Table comment
    private List<ColumnDTO> columns = new ArrayList<>();               // Column metadata
    private List<List<String>> primaryKeys = new ArrayList<>();        // Supports composite primary keys
    private List<IndexDTO> indexes = new ArrayList<>();                // Index metadata
    private List<ForeignKeyDTO> foreignKeys = new ArrayList<>();       // Foreign key definitions

    /**
     * Default constructor. Now, whenever you call `new TableDTO()`,
     * the lists above will be ready to use immediately.
     */
    public TableDTO() {
    }

    /**
     * Convenience constructor that also benefits from the initialized lists.
     *
     * @param tableName The name of the table.
     */
    public TableDTO(String tableName) {
        this.tableName = tableName;
    }
}
