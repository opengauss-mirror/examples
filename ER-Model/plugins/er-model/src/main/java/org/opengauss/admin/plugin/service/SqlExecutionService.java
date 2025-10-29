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

package org.opengauss.admin.plugin.service;

import java.sql.SQLException;

/**
 * Service interface that executes SQL on managed database instances.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
public interface SqlExecutionService {
    /**
     * Execute SQL on a database instance managed by the platform.
     *
     * @param clusterId target cluster identifier
     * @param nodeId    target node identifier
     * @param sql       SQL text to execute
     * @throws SQLException           when database access fails
     * @throws ClassNotFoundException when the JDBC driver is not available
     */
    void executeSql(String clusterId, String nodeId, String sql) throws SQLException, ClassNotFoundException;
}
