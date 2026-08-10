package com.baomidou.mybatisplus.enhance.plugins.inner;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for the default methods of {@link EnhanceInnerInterceptor}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("EnhanceInnerInterceptor defaults")
class EnhanceInnerInterceptorTest {

    private EnhanceInnerInterceptor createDefaultInterceptor() {
        return new EnhanceInnerInterceptor() {};
    }

    @Test
    @DisplayName("phase() should default to UNSPECIFIED")
    void phaseShouldDefaultToUnspecified() {
        EnhanceInnerInterceptor interceptor = createDefaultInterceptor();
        assertThat(interceptor.phase()).isEqualTo(EnhancePhase.UNSPECIFIED);
    }

    @Test
    @DisplayName("afterQuery() should return the input list unchanged")
    void afterQueryShouldReturnInputList() throws SQLException {
        EnhanceInnerInterceptor interceptor = createDefaultInterceptor();
        List<Object> input = Arrays.asList("a", "b", "c");

        List<Object> result = interceptor.afterQuery(
                null, null, null, null, null, null, input);
        assertThat(result).isSameAs(input);
    }

    @Test
    @DisplayName("afterQuery() should return empty list when given empty list")
    void afterQueryShouldReturnEmptyList() throws SQLException {
        EnhanceInnerInterceptor interceptor = createDefaultInterceptor();
        List<Object> empty = Collections.emptyList();

        List<Object> result = interceptor.afterQuery(
                null, null, null, null, null, null, empty);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("afterUpdate() should not throw with null parameters")
    void afterUpdateShouldNotThrow() {
        EnhanceInnerInterceptor interceptor = createDefaultInterceptor();

        assertThatNoException()
                .isThrownBy(() -> interceptor.afterUpdate(null, null, null, null, 5));
    }

    @Test
    @DisplayName("afterExecution() should not throw on success path")
    void afterExecutionShouldNotThrowOnSuccess() {
        EnhanceInnerInterceptor interceptor = createDefaultInterceptor();

        assertThatNoException()
                .isThrownBy(() -> interceptor.afterExecution(
                        null, null, null, null, "result", null, 1000L));
    }

    @Test
    @DisplayName("afterExecution() should not throw on failure path")
    void afterExecutionShouldNotThrowOnFailure() {
        EnhanceInnerInterceptor interceptor = createDefaultInterceptor();
        Throwable failure = new RuntimeException("test");

        assertThatNoException()
                .isThrownBy(() -> interceptor.afterExecution(
                        null, null, null, null, null, failure, 500L));
    }

    @Test
    @DisplayName("afterExecution() should not throw with zero elapsed time")
    void afterExecutionShouldNotThrowWithZeroElapsed() {
        EnhanceInnerInterceptor interceptor = createDefaultInterceptor();

        assertThatNoException()
                .isThrownBy(() -> interceptor.afterExecution(
                        null, null, null, null, null, null, 0L));
    }
}
