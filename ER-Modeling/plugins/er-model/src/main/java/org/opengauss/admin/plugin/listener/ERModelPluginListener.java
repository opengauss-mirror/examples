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

package org.opengauss.admin.plugin.listener;

import com.gitee.starblues.spring.MainApplicationContext;
import com.gitee.starblues.spring.SpringBeanFactory;

import lombok.extern.slf4j.Slf4j;

import org.opengauss.admin.common.core.vo.MenuVo;
import org.opengauss.admin.system.plugin.facade.MenuFacade;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Listener reacting to lifecycle events to manage ER model plugin resources and menu entries.
 *
 * @version 1.0.0
 * @since 2024-05-07
 */
@Slf4j
@Component
public class ERModelPluginListener implements ApplicationListener<ApplicationEvent> {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            log.info("ERModel init env");
        } else if (event instanceof ApplicationPreparedEvent) {
            log.info("ERModel init complete");
        } else if (event instanceof ContextRefreshedEvent) {
            log.info("ERModel was refreshed");
        } else if (event instanceof ApplicationReadyEvent) {
            MainApplicationContext context = ((ApplicationReadyEvent) event)
                    .getApplicationContext()
                    .getBean(MainApplicationContext.class);
            SpringBeanFactory factory = context.getSpringBeanFactory();
            MenuFacade menuFacade = factory.getBean(MenuFacade.class);
            if (menuFacade != null) {
                String pluginId = "er-model";
                // 重新注册菜单前，先清理旧记录，避免多语言切换产生重复
                menuFacade.deletePluginMenu(pluginId);
                // Root menu path is an empty string ""; final route is /static-plugin/er-model/
                MenuVo parentMenu = menuFacade.savePluginMenu(pluginId, "ER建模",
                        "ER diagram modeling", 1, "er-tool");

                // Child menu path editor -> /static-plugin/er-model/editor
                menuFacade.savePluginMenu(pluginId, "设计ER图", "Design ER diagram",
                        1, "editor", parentMenu.getMenuId());

                // Child menu path erShow -> /static-plugin/er-model/erShow
                menuFacade.savePluginMenu(pluginId, "展示数据库ER图", "Show ER diagram",
                        2, "show-diagram", parentMenu.getMenuId());
            }
            log.info("ERModel start complete");
        } else if (event instanceof ContextClosedEvent) {
            MainApplicationContext context = ((ContextClosedEvent) event)
                    .getApplicationContext()
                    .getBean(MainApplicationContext.class);
            SpringBeanFactory factory = context.getSpringBeanFactory();
            MenuFacade menuFacade = factory.getBean(MenuFacade.class);
            if (menuFacade != null) {
                menuFacade.deletePluginMenu("er-model");
            }
            log.info("ERModel is stopped");
        } else {
            log.debug("Unhandled plugin event type: {}", event.getClass().getName());
        }
    }
}
