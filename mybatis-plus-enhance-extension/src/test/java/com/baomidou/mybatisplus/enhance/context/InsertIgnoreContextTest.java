package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InsertIgnoreContext}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("InsertIgnoreContext")
class InsertIgnoreContextTest {

    @AfterEach
    void cleanup() {
        InsertIgnoreContext.clear();
    }

    @Test
    @DisplayName("should be disabled by default")
    void shouldBeDisabledByDefault() {
        assertThat(InsertIgnoreContext.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should enable when scope is open")
    void shouldBeEnabledWhenScopeOpen() {
        try (InsertIgnoreContext.Scope scope = InsertIgnoreContext.open()) {
            assertThat(InsertIgnoreContext.isEnabled()).isTrue();
        }
    }

    @Test
    @DisplayName("should restore after scope is closed")
    void shouldRestoreAfterScopeClosed() {
        InsertIgnoreContext.open().close();
        assertThat(InsertIgnoreContext.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should restore nested scopes correctly")
    void shouldRestoreNestedScopes() {
        assertThat(InsertIgnoreContext.isEnabled()).isFalse();
        try (InsertIgnoreContext.Scope outer = InsertIgnoreContext.open()) {
            assertThat(InsertIgnoreContext.isEnabled()).isTrue();
            try (InsertIgnoreContext.Scope inner = InsertIgnoreContext.open()) {
                assertThat(InsertIgnoreContext.isEnabled()).isTrue();
            }
            assertThat(InsertIgnoreContext.isEnabled()).isTrue();
        }
        assertThat(InsertIgnoreContext.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should be safe to close scope multiple times")
    void shouldBeSafeToCloseMultipleTimes() {
        InsertIgnoreContext.Scope scope = InsertIgnoreContext.open();
        scope.close();
        scope.close();
        assertThat(InsertIgnoreContext.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should clear state completely")
    void shouldClearStateCompletely() {
        try (InsertIgnoreContext.Scope scope = InsertIgnoreContext.open()) {
            InsertIgnoreContext.clear();
            assertThat(InsertIgnoreContext.isEnabled()).isFalse();
        }
    }

    @Test
    @DisplayName("should transmit context to TTL thread pool task")
    void shouldTransmitContextToThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            Future<Boolean> enabled;
            try (InsertIgnoreContext.Scope ignored = InsertIgnoreContext.open()) {
                enabled = executor.submit(InsertIgnoreContext::isEnabled);
            }

            assertThat(enabled.get()).isTrue();
            assertThat(InsertIgnoreContext.isEnabled()).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("should capture context for each thread pool task independently")
    void shouldCaptureContextForEachThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            Future<Boolean> enabled;
            try (InsertIgnoreContext.Scope ignored = InsertIgnoreContext.open()) {
                enabled = executor.submit(InsertIgnoreContext::isEnabled);
            }
            Future<Boolean> disabled = executor.submit(InsertIgnoreContext::isEnabled);

            assertThat(enabled.get()).isTrue();
            assertThat(disabled.get()).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }
}
