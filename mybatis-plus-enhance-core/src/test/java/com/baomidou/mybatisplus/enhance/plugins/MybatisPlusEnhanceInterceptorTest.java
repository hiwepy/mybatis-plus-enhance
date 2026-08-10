package com.baomidou.mybatisplus.enhance.plugins;

import com.baomidou.mybatisplus.enhance.plugins.inner.EnhanceInnerInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.EnhancePhase;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MybatisPlusEnhanceInterceptor}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("MybatisPlusEnhanceInterceptor")
class MybatisPlusEnhanceInterceptorTest {

    private MybatisPlusEnhanceInterceptor createInterceptor() {
        return new MybatisPlusEnhanceInterceptor();
    }

    private EnhanceInnerInterceptor createEnhanceInterceptor(EnhancePhase phase) {
        return new EnhanceInnerInterceptor() {
            @Override
            public EnhancePhase phase() {
                return phase;
            }
        };
    }

    private InnerInterceptor createStandardInterceptor() {
        return new InnerInterceptor() {};
    }

    // ---- addInnerInterceptor ----

    @Nested
    @DisplayName("addInnerInterceptor")
    class AddInnerInterceptor {

        @Test
        @DisplayName("should register a standard inner interceptor")
        void shouldRegisterStandardInterceptor() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            InnerInterceptor standard = createStandardInterceptor();
            interceptor.addInnerInterceptor(standard);
            assertThat(interceptor.getInterceptors()).containsExactly(standard);
        }

        @Test
        @DisplayName("should register an enhance interceptor with UNSPECIFIED phase")
        void shouldRegisterUnspecifiedPhase() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor enhance = createEnhanceInterceptor(EnhancePhase.UNSPECIFIED);
            interceptor.addInnerInterceptor(enhance);
            assertThat(interceptor.getInterceptors()).containsExactly(enhance);
        }

        @Test
        @DisplayName("should reject null interceptor")
        void shouldRejectNull() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            assertThatThrownBy(() -> interceptor.addInnerInterceptor(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should accept enhance interceptors in valid phase order")
        void shouldAcceptValidPhaseOrder() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor enc = createEnhanceInterceptor(EnhancePhase.PARAMETER_ENCRYPTION);
            EnhanceInnerInterceptor sig = createEnhanceInterceptor(EnhancePhase.DATA_SIGNATURE);
            EnhanceInnerInterceptor dec = createEnhanceInterceptor(EnhancePhase.RESULT_DECRYPTION);
            interceptor.addInnerInterceptor(enc);
            interceptor.addInnerInterceptor(sig);
            interceptor.addInnerInterceptor(dec);
            assertThat(interceptor.getInterceptors()).hasSize(3);
        }

        @Test
        @DisplayName("should reject enhance interceptors in invalid phase order")
        void shouldRejectInvalidPhaseOrder() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor dec = createEnhanceInterceptor(EnhancePhase.RESULT_DECRYPTION);
            EnhanceInnerInterceptor enc = createEnhanceInterceptor(EnhancePhase.PARAMETER_ENCRYPTION);
            interceptor.addInnerInterceptor(dec);
            assertThatThrownBy(() -> interceptor.addInnerInterceptor(enc))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid enhance interceptor order");
        }

        @Test
        @DisplayName("should allow UNSPECIFIED phase interleaved without affecting ordering")
        void shouldAllowUnspecifiedInterleaved() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor enc = createEnhanceInterceptor(EnhancePhase.PARAMETER_ENCRYPTION);
            EnhanceInnerInterceptor unspecified = createEnhanceInterceptor(EnhancePhase.UNSPECIFIED);
            EnhanceInnerInterceptor sig = createEnhanceInterceptor(EnhancePhase.DATA_SIGNATURE);
            interceptor.addInnerInterceptor(enc);
            interceptor.addInnerInterceptor(unspecified);
            interceptor.addInnerInterceptor(sig);
            assertThat(interceptor.getInterceptors()).hasSize(3);
        }

        @Test
        @DisplayName("should allow same phase consecutively")
        void shouldAllowSamePhaseConsecutively() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor sig1 = createEnhanceInterceptor(EnhancePhase.DATA_SIGNATURE);
            EnhanceInnerInterceptor sig2 = createEnhanceInterceptor(EnhancePhase.DATA_SIGNATURE);
            interceptor.addInnerInterceptor(sig1);
            interceptor.addInnerInterceptor(sig2);
            assertThat(interceptor.getInterceptors()).hasSize(2);
        }
    }

    // ---- setInterceptors ----

    @Nested
    @DisplayName("setInterceptors")
    class SetInterceptors {

        @Test
        @DisplayName("should accept valid interceptor list")
        void shouldAcceptValidList() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor enc = createEnhanceInterceptor(EnhancePhase.PARAMETER_ENCRYPTION);
            EnhanceInnerInterceptor sig = createEnhanceInterceptor(EnhancePhase.DATA_SIGNATURE);
            interceptor.setInterceptors(Arrays.asList(enc, sig));
            assertThat(interceptor.getInterceptors()).containsExactly(enc, sig);
        }

        @Test
        @DisplayName("should reject null list")
        void shouldRejectNullList() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            assertThatThrownBy(() -> interceptor.setInterceptors(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject list with invalid phase ordering")
        void shouldRejectInvalidOrder() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor sig = createEnhanceInterceptor(EnhancePhase.DATA_SIGNATURE);
            EnhanceInnerInterceptor enc = createEnhanceInterceptor(EnhancePhase.PARAMETER_ENCRYPTION);
            assertThatThrownBy(() -> interceptor.setInterceptors(Arrays.asList(sig, enc)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should accept empty list")
        void shouldAcceptEmptyList() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            interceptor.setInterceptors(Collections.emptyList());
            assertThat(interceptor.getInterceptors()).isEmpty();
        }

        @Test
        @DisplayName("should replace existing interceptors")
        void shouldReplaceExisting() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor enc = createEnhanceInterceptor(EnhancePhase.PARAMETER_ENCRYPTION);
            EnhanceInnerInterceptor dec = createEnhanceInterceptor(EnhancePhase.RESULT_DECRYPTION);
            interceptor.addInnerInterceptor(enc);
            interceptor.setInterceptors(Collections.singletonList(dec));
            assertThat(interceptor.getInterceptors()).containsExactly(dec);
        }
    }

    // ---- toString ----

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain class name and interceptor list")
        void shouldContainClassName() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            assertThat(interceptor.toString())
                    .contains("MybatisPlusEnhanceInterceptor")
                    .contains("interceptors=");
        }

        @Test
        @DisplayName("should show registered interceptors")
        void shouldShowRegisteredInterceptors() {
            MybatisPlusEnhanceInterceptor interceptor = createInterceptor();
            EnhanceInnerInterceptor enc = createEnhanceInterceptor(EnhancePhase.PARAMETER_ENCRYPTION);
            interceptor.addInnerInterceptor(enc);
            assertThat(interceptor.toString()).contains("[");
        }
    }
}
