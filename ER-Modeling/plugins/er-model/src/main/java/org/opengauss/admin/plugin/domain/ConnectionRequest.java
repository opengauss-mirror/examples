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

package org.opengauss.admin.plugin.domain;

import org.opengauss.admin.plugin.enums.DatabaseDialect;

/**
 * Represents a request for establishing a database connection.
 *
 * @since 2025-10-10
 */
public record ConnectionRequest(
        DatabaseDialect dialect,
        String host,
        int port,
        String database,
        String username,
        String password,
        String schema) {

    /**
     * Creates a new builder for constructing ConnectionRequest instances.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new ConnectionRequest with the specified schema name.
     *
     * @param schemaName the schema name to use, or null/blank to keep the current schema
     * @return a new ConnectionRequest with the specified schema
     */
    public ConnectionRequest withSchema(String schemaName) {
        String normalizedSchema = schemaName == null || schemaName.isBlank()
                ? this.schema
                : schemaName;
        return new ConnectionRequest(dialect, host, port, database, username, password, normalizedSchema);
    }

    /**
     * Returns a new ConnectionRequest with the specified database name.
     *
     * @param databaseName the database name to use, or null/blank to keep the current database
     * @return a new ConnectionRequest with the specified database
     */
    public ConnectionRequest withDatabase(String databaseName) {
        String normalizedDatabase = databaseName == null || databaseName.isBlank()
                ? this.database
                : databaseName;
        return new ConnectionRequest(dialect, host, port, normalizedDatabase, username, password, schema);
    }

    /**
     * Builder for constructing ConnectionRequest instances.
     */
    public static final class Builder {
        private DatabaseDialect dialect;
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private String schema;

        /**
         * Sets the database dialect.
         *
         * @param value the database dialect
         * @return this builder
         */
        public Builder dialect(DatabaseDialect value) {
            this.dialect = value;
            return this;
        }

        /**
         * Sets the host.
         *
         * @param value the host
         * @return this builder
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the port.
         *
         * @param value the port
         * @return this builder
         */
        public Builder port(int value) {
            this.port = value;
            return this;
        }

        /**
         * Sets the database name.
         *
         * @param value the database name
         * @return this builder
         */
        public Builder database(String value) {
            this.database = value;
            return this;
        }

        /**
         * Sets the username.
         *
         * @param value the username
         * @return this builder
         */
        public Builder username(String value) {
            this.username = value;
            return this;
        }

        /**
         * Sets the password.
         *
         * @param value the password
         * @return this builder
         */
        public Builder password(String value) {
            this.password = value;
            return this;
        }

        /**
         * Sets the schema.
         *
         * @param value the schema
         * @return this builder
         */
        public Builder schema(String value) {
            this.schema = value;
            return this;
        }

        /**
         * Builds a new ConnectionRequest instance.
         *
         * @return a new ConnectionRequest
         */
        public ConnectionRequest build() {
            return new ConnectionRequest(dialect, host, port, database, username, password, schema);
        }
    }
}