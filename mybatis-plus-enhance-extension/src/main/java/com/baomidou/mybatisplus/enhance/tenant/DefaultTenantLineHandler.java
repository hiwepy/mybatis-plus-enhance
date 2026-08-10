/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.tenant;

import com.baomidou.mybatisplus.enhance.context.TenantContext;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Default MyBatis-Plus tenant line handler.
 *
 * <p>The tenant value is provided by {@link TenantContext}. The tenant column name and
 * ignore-table strategy are configurable by the caller.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 2.0.0
 */
public class DefaultTenantLineHandler implements TenantLineHandler {

    /** Default tenant column name. */
    public static final String DEFAULT_TENANT_COLUMN = "tenant_id";

    /**
     * Default predicate that never ignores any business table.
     */
    private static final Predicate<String> NEVER_IGNORE = tableName -> false;

    /**
     * Context providing the current execution chain's tenant identifier.
     */
    private final TenantContext context;

    /**
     * Column name appended to tenant SQL conditions.
     */
    private final String tenantColumn;

    /**
     * Predicate determining whether a given table should skip the tenant condition.
     */
    private final Predicate<String> ignoredTable;

    /**
     * Creates a handler using the default tenant column {@value #DEFAULT_TENANT_COLUMN}.
     *
     * @param context the tenant context
     */
    public DefaultTenantLineHandler(TenantContext context) {
        this(context, DEFAULT_TENANT_COLUMN, NEVER_IGNORE);
    }

    /**
     * Creates a handler with a configurable tenant column and ignore-table strategy.
     *
     * @param context      the tenant context
     * @param tenantColumn the tenant column name
     * @param ignoredTable predicate returning {@code true} for tables that should skip
     *                     the tenant condition
     */
    public DefaultTenantLineHandler(TenantContext context, String tenantColumn, Predicate<String> ignoredTable) {
        this.context = Objects.requireNonNull(context, "TenantContext must not be null");
        this.tenantColumn = Objects.requireNonNull(tenantColumn, "Tenant column must not be null");
        this.ignoredTable = Objects.requireNonNull(ignoredTable, "Ignored-table predicate must not be null");
    }

    /**
     * Converts the current tenant identifier to a JSqlParser expression.
     *
     * <p>Numeric tenants produce a {@link LongValue}; all other types produce
     * a {@link StringValue}.</p>
     *
     * @return the tenant expression
     * @throws IllegalStateException if no tenant identifier is present in the context
     */
    @Override
    public Expression getTenantId() {
        Object tenantId = context.getCurrentTenantId();
        if (Objects.isNull(tenantId)) {
            throw new IllegalStateException("Tenant ID is missing from TenantContext");
        }
        if (tenantId instanceof Number) {
            return new LongValue(tenantId.toString());
        }
        return new StringValue(tenantId.toString());
    }

    /**
     * Returns the tenant column name.
     *
     * @return the tenant column name
     */
    @Override
    public String getTenantIdColumn() {
        return tenantColumn;
    }

    /**
     * Determines whether the given table should skip the tenant condition.
     *
     * @param tableName the database table name
     * @return {@code true} if the table should be ignored
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return ignoredTable.test(tableName);
    }
}
