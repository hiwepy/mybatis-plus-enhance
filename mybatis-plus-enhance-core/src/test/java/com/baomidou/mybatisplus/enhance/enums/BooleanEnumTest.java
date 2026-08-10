package com.baomidou.mybatisplus.enhance.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link BooleanEnum}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("BooleanEnum")
class BooleanEnumTest {

    @Test
    @DisplayName("should map false to database value 0")
    void shouldMapFalseToZero() {
        assertThat(BooleanEnum.IS_FALSE.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("should map true to database value 1")
    void shouldMapTrueToOne() {
        assertThat(BooleanEnum.IS_TRUE.getValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("should resolve IS_FALSE from database value 0")
    void shouldResolveFalseFromDbValue() {
        assertThat(BooleanEnum.valueOf(0)).isSameAs(BooleanEnum.IS_FALSE);
    }

    @Test
    @DisplayName("should resolve IS_TRUE from database value 1")
    void shouldResolveTrueFromDbValue() {
        assertThat(BooleanEnum.valueOf(1)).isSameAs(BooleanEnum.IS_TRUE);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 2, 3, 100})
    @DisplayName("should reject unsupported database values")
    void shouldRejectUnsupportedDatabaseValue(int value) {
        assertThatThrownBy(() -> BooleanEnum.valueOf(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported boolean database value");
    }

    @Test
    @DisplayName("should expose boolean semantic value")
    void shouldExposeBooleanSemanticValue() {
        assertThat(BooleanEnum.IS_FALSE.isBooleanValue()).isFalse();
        assertThat(BooleanEnum.IS_TRUE.isBooleanValue()).isTrue();
    }

    @Test
    @DisplayName("should retain Chinese display name")
    void shouldRetainChineseDisplayName() {
        assertThat(BooleanEnum.IS_FALSE.getNameCn()).isEqualTo("否");
        assertThat(BooleanEnum.IS_TRUE.getNameCn()).isEqualTo("是");
    }

    @Test
    @DisplayName("should have exactly two enum constants")
    void shouldHaveExactlyTwoConstants() {
        assertThat(BooleanEnum.values()).hasSize(2);
    }
}
