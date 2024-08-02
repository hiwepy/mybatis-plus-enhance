/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.tenant;


import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.ArrayList;
import java.util.List;

/**
 * 多租户拦截 : https://cloud.tencent.com/developer/article/1494339
 * 租户处理器 -主要实现 mybatis-plus https://mp.baomidou.com/guide/tenant.html
 */
@Slf4j
public class PreTenantHandler implements TenantLineHandler {

    /**
     * 多租户标识
     */
    private static final String SYSTEM_TENANT_ID = "tenant_id";
    /**
     * 需要过滤的表
     */
    private static final List<String> IGNORE_TENANT_TABLES = new ArrayList<>();

    private final TenantContext context;

    public PreTenantHandler(TenantContext context) {
    	this.context = context;
    }

    /**
     * 租户Id
     *
     * @return
     */
    @Override
    public Expression getTenantId() {
        // 从当前系统上下文中取出当前请求的服务商ID，通过解析器注入到SQL中。
        Long tenantId = context.getCurrentTenantId();
        log.debug("当前租户为{}", tenantId);
        if (tenantId == null) {
            return new NullValue();
        }
        return new LongValue(tenantId);
    }

    /**
     * 租户字段名
     *
     * @return
     */
    @Override
    public String getTenantIdColumn() {
        return SYSTEM_TENANT_ID;
    }

    /**
     * 根据表名判断是否进行过滤
     * 忽略掉一些表：如租户表（sys_tenant）本身不需要执行这样的处理
     *
     * @param tableName
     * @return
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TENANT_TABLES.stream().anyMatch((e) -> e.equalsIgnoreCase(tableName));
    }

}
