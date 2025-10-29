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

package org.opengauss.admin.plugin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA fallback controller so that history-mode routes under /static-plugin/er-model/
 * always serve the compiled index.html artifact.
 *
 * Without this forwarder the host would return 404 for paths such as
 * /static-plugin/er-model/editor because the static resource handler only knows
 * about physical files. By forwarding to index.html we let the Vue/Quasar router
 * take over on the client side.
 *
 * @since 2025-10-28
 */
@Slf4j
@Controller
public class ErModelSpaForwardController {
    private static final String FORWARD_INDEX = "forward:/static-plugin/er-model/index.html";

    @RequestMapping(value = {
            "/static-plugin/er-model",
            "/static-plugin/er-model/",
            "/static-plugin/er-model/{path:[^\\.]*}"
    })
    public String forwardToIndex() {
        log.debug("Forwarding ER model SPA request to index.html");
        return FORWARD_INDEX;
    }
}

