package com.baomidou.mybatisplus.enhance.context;

import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SignatureUpdateContext}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("SignatureUpdateContext")
class SignatureUpdateContextTest {

    @AfterEach
    void tearDown() {
        SignatureUpdateContext.clear();
    }

    @Test
    @DisplayName("should default to REJECT_PARTIAL when no scope is open")
    void shouldDefaultToRejectPartial() {
        assertThat(SignatureUpdateContext.current())
                .isEqualTo(SignatureUpdateStrategy.REJECT_PARTIAL);
    }

    @Test
    @DisplayName("should restore previous strategy after nested scope")
    void shouldRestorePreviousStrategyAfterNestedScope() {
        assertThat(SignatureUpdateContext.current())
                .isEqualTo(SignatureUpdateStrategy.REJECT_PARTIAL);

        try (SignatureUpdateContext.Scope ignored = SignatureUpdateContext.open(
                SignatureUpdateStrategy.DEFERRED_RESIGN)) {
            assertThat(SignatureUpdateContext.current())
                    .isEqualTo(SignatureUpdateStrategy.DEFERRED_RESIGN);
            try (SignatureUpdateContext.Scope nested = SignatureUpdateContext.open(
                    SignatureUpdateStrategy.SIGNATURE_ONLY)) {
                assertThat(SignatureUpdateContext.current())
                        .isEqualTo(SignatureUpdateStrategy.SIGNATURE_ONLY);
            }
            assertThat(SignatureUpdateContext.current())
                    .isEqualTo(SignatureUpdateStrategy.DEFERRED_RESIGN);
        }

        assertThat(SignatureUpdateContext.current())
                .isEqualTo(SignatureUpdateStrategy.REJECT_PARTIAL);
    }

    @Test
    @DisplayName("should be safe to close scope multiple times")
    void shouldBeSafeToCloseMultipleTimes() {
        SignatureUpdateContext.Scope scope = SignatureUpdateContext.open(
                SignatureUpdateStrategy.DEFERRED_RESIGN);
        scope.close();
        scope.close();
        assertThat(SignatureUpdateContext.current())
                .isEqualTo(SignatureUpdateStrategy.REJECT_PARTIAL);
    }

    @Test
    @DisplayName("should reject null strategy")
    void shouldRejectNullStrategy() {
        assertThatThrownBy(() -> SignatureUpdateContext.open(null))
                .isInstanceOf(NullPointerException.class);
    }
}
