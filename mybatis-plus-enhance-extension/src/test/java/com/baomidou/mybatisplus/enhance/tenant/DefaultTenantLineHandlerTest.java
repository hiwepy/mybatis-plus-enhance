package com.baomidou.mybatisplus.enhance.tenant;

import com.baomidou.mybatisplus.enhance.context.TenantContext;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultTenantLineHandler}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("DefaultTenantLineHandler")
class DefaultTenantLineHandlerTest {

    private final TenantContext context = new TenantContext();

    @AfterEach
    void tearDown() {
        context.clear();
    }

    @Test
    @DisplayName("should create LongValue expression for numeric tenant")
    void shouldCreateNumericTenantExpression() {
        context.setCurrentTenantId(1001L);
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(context);

        assertThat(handler.getTenantId()).isEqualTo(new LongValue(1001L));
    }

    @Test
    @DisplayName("should create StringValue expression for string tenant")
    void shouldCreateStringTenantExpression() {
        context.setCurrentTenantId("tenant-a");
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(context);

        assertThat(handler.getTenantId()).isEqualTo(new StringValue("tenant-a"));
    }

    @Test
    @DisplayName("should reject missing tenant with IllegalStateException")
    void shouldRejectMissingTenant() {
        assertThatThrownBy(() -> new DefaultTenantLineHandler(context).getTenantId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant ID is missing");
    }

    @Test
    @DisplayName("should use default tenant column name")
    void shouldUseDefaultTenantColumn() {
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(context);
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }

    @Test
    @DisplayName("should support custom tenant column")
    void shouldSupportCustomTenantColumn() {
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(
                context, "organization_id", tableName -> false);
        assertThat(handler.getTenantIdColumn()).isEqualTo("organization_id");
    }

    @Test
    @DisplayName("should ignore tables matching the predicate")
    void shouldIgnoreTablesMatchingPredicate() {
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(
                context, "tenant_id", tableName -> tableName.startsWith("sys_"));

        assertThat(handler.ignoreTable("sys_tenant")).isTrue();
        assertThat(handler.ignoreTable("sys_config")).isTrue();
        assertThat(handler.ignoreTable("biz_order")).isFalse();
    }

    @Test
    @DisplayName("should not ignore any table with default predicate")
    void shouldNotIgnoreAnyTableByDefault() {
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(context);

        assertThat(handler.ignoreTable("any_table")).isFalse();
    }

    @Test
    @DisplayName("should reject null context")
    void shouldRejectNullContext() {
        assertThatThrownBy(() -> new DefaultTenantLineHandler(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should reject null tenant column")
    void shouldRejectNullTenantColumn() {
        assertThatThrownBy(() -> new DefaultTenantLineHandler(context, null, t -> false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should reject null ignored-table predicate")
    void shouldRejectNullIgnoredTablePredicate() {
        assertThatThrownBy(() -> new DefaultTenantLineHandler(context, "tenant_id", null))
                .isInstanceOf(NullPointerException.class);
    }
}
