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

package org.opengauss.admin.plugin.service.impl;

import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.plugin.service.DataSourceService;
import org.opengauss.admin.plugin.service.SqlExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service that executes SQL scripts against managed cluster connections.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
@Slf4j
@Service
public class SqlExecutionServiceImpl implements SqlExecutionService {
    private final DataSourceService dataSourceService;

    @Autowired
    public SqlExecutionServiceImpl(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @Override
    public void executeSql(String clusterId, String nodeId, String sqlScript)
            throws SQLException, ClassNotFoundException {
        // Use try-with-resources to ensure the connection closes automatically
        try (Connection connection = dataSourceService.getConnection(clusterId, nodeId)) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                for (String raw : sqlScript.split(";")) {
                    String sql = raw.trim();
                    if (sql.isEmpty()) {
                        continue;
                    }
                    log.debug("Executing SQL: [{}]", sql);
                    statement.execute(sql);
                }
                log.info("SQL script executed successfully and transaction committed.");
                connection.commit();
            } catch (SQLException e) {
                log.error("Error executing SQL script, rolling back transaction.", e);
                connection.rollback();
                throw e;
            }
        }
    }
}
