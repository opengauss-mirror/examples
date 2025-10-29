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

package org.opengauss.admin.plugin.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.opengauss.admin.common.constant.SqlConstants;
import org.opengauss.admin.common.core.domain.AjaxResult;
import org.opengauss.admin.common.core.domain.model.LoginUser;
import org.opengauss.admin.common.core.page.PageDomain;
import org.opengauss.admin.common.core.page.TableDataInfo;
import org.opengauss.admin.common.core.page.TableSupport;
import org.opengauss.admin.common.enums.ResponseCode;
import org.opengauss.admin.common.utils.DateUtils;
import org.opengauss.admin.common.utils.SecurityUtils;
import org.opengauss.admin.common.utils.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.util.Date;

/**
 * Shared controller utilities providing pagination helpers, Ajax response shortcuts,
 * and common data binding configuration for plugin REST endpoints.
 *
 * @since 2025-10-19
 */
public class BaseController {
    /**
     * Register a custom date editor that converts string inputs to {@link Date}.
     *
     * @param binder web data binder that holds registered editors
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Date format
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(DateUtils.parseDate(text));
            }
        });
    }

    /**
     * Build a MyBatis-Plus {@link Page} object based on the current request context.
     *
     * @return configured page instance
     */
    protected Page startPage() {
        Page page = new Page<>();
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = pageDomain.getOrderBy();
        String orderColumn = pageDomain.getOrderByColumn();
        String isAsc = pageDomain.getIsAsc();

        if (StringUtils.isNotNull(pageNum) && StringUtils.isNotNull(pageSize)) {
            page.setCurrent(pageNum);
            page.setSize(pageSize);
            page.setOptimizeCountSql(false);
            page.setMaxLimit(500L);
        }

        addOrder(orderColumn, isAsc, page);

        return page;
    }

    /**
     * Build a MyBatis-Plus {@link Page} using explicit pagination parameters.
     *
     * @param pageNum     current page index (1-based)
     * @param size        page size
     * @param orderColumn column used for ordering
     * @param order       order direction (ASC/DESC)
     * @return configured page instance
     */
    protected Page startPage(Integer pageNum, Integer size, String orderColumn, String order) {
        Page page = new Page();
        page.setCurrent(pageNum);
        page.setSize(size);
        page.setOptimizeCountSql(false);
        page.setMaxLimit(500L);

        addOrder(orderColumn, order, page);
        return page;
    }

    /**
     * Append ordering metadata to a page instance if a column is provided.
     *
     * @param orderColumn column name used for ordering
     * @param order       order direction (ASC/DESC)
     * @param page        page instance receiving the ordering configuration
     */
    private void addOrder(String orderColumn, String order, Page page) {
        if (StringUtils.isNotBlank(orderColumn)) {
            if (SqlConstants.ASC.equalsIgnoreCase(order)) {
                page.addOrder(OrderItem.asc(orderColumn));
            } else {
                page.addOrder(OrderItem.desc(orderColumn));
            }
        }
    }

    /**
     * Wrap an {@link IPage} result into the standardized {@link TableDataInfo}.
     *
     * @param page underlying page data
     * @return table response containing rows and total count
     */
    protected TableDataInfo getDataTable(IPage<?> page) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(ResponseCode.SUCCESS.code());
        rspData.setMsg(ResponseCode.SUCCESS.msg());
        rspData.setRows(page.getRecords());
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    /**
     * Convert an affected-row count into a standard Ajax response.
     *
     * @param rows number of affected rows
     * @return success when rows &gt; 0; otherwise error
     */
    protected AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * Convert a boolean flag into a standard Ajax response.
     *
     * @param isSuccess operation success indicator
     * @return success or error response
     */
    protected AjaxResult toAjax(boolean isSuccess) {
        return isSuccess ? success() : error();
    }

    /**
     * Produce a success Ajax response without payload.
     *
     * @return success result
     */
    public AjaxResult success() {
        return AjaxResult.success();
    }

    /**
     * Produce an error Ajax response without payload.
     *
     * @return error result
     */
    public AjaxResult error() {
        return AjaxResult.error();
    }

    /**
     * Produce a success Ajax response with a message.
     *
     * @param message success message
     * @return success result
     */
    public AjaxResult success(String message) {
        return AjaxResult.success(message);
    }

    /**
     * Produce an error Ajax response with a message.
     *
     * @param message error message
     * @return error result
     */
    public AjaxResult error(String message) {
        return AjaxResult.error(message);
    }

    /**
     * Retrieve the current authenticated user.
     *
     * @return logged-in user details
     */
    public LoginUser getLoginUser() {
        return SecurityUtils.getLoginUser();
    }

    /**
     * Retrieve the current authenticated user identifier.
     *
     * @return user id
     */
    public Integer getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * Retrieve the current authenticated username.
     *
     * @return username
     */
    public String getUsername() {
        return getLoginUser().getUsername();
    }
}
