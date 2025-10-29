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

import java.util.Objects;

/**
 * Immutable DTO describing whether a task succeeded together with a human readable message.
 *
 * @since 2025-03-10
 */
public final class ExecuteResult {
    private static final String DEFAULT_SUCCESS_MESSAGE = "操作成功";

    private final boolean isSuccess;
    private final String message;

    private ExecuteResult(boolean isSuccessful, String message) {
        this.isSuccess = isSuccessful;
        this.message = message;
    }

    /**
     * Create a successful result with a default success message.
     *
     * @return success result
     */
    public static ExecuteResult success() {
        return success(DEFAULT_SUCCESS_MESSAGE);
    }

    /**
     * Create a successful result with a custom message.
     *
     * @param message success description, sanitized to non-null
     * @return success result
     */
    public static ExecuteResult success(String message) {
        return new ExecuteResult(true, sanitize(message));
    }

    /**
     * Create a failed result with the provided message.
     *
     * @param message failure description, sanitized to non-null
     * @return failure result
     */
    public static ExecuteResult fail(String message) {
        return new ExecuteResult(false, sanitize(message));
    }

    /**
     * Whether the operation completed successfully.
     *
     * @return {@code true} when successful
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    /**
     * Human-readable message describing the outcome.
     *
     * @return outcome message, never {@code null}
     */
    public String getMessage() {
        return message;
    }

    /**
     * Create a new result with the same success flag but a different message.
     *
     * @param newMessage updated message
     * @return new result instance
     */
    public ExecuteResult withMessage(String newMessage) {
        return new ExecuteResult(isSuccess, sanitize(newMessage));
    }

    private static String sanitize(String message) {
        return Objects.requireNonNullElse(message, "");
    }
}
