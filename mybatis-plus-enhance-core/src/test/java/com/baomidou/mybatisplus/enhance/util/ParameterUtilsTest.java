package com.baomidou.mybatisplus.enhance.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ParameterUtils}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("ParameterUtils")
class ParameterUtilsTest {

    // ---- isSwitchOff(boolean, Object) ----

    @Nested
    @DisplayName("isSwitchOff(globalSwitch, parameterObject)")
    class SwitchOffForObject {

        @Test
        @DisplayName("should return true when global switch is off")
        void shouldReturnTrueWhenSwitchOff() {
            assertThat(ParameterUtils.isSwitchOff(false, new Object())).isTrue();
        }

        @Test
        @DisplayName("should return true when parameter is null")
        void shouldReturnTrueWhenParameterNull() {
            assertThat(ParameterUtils.isSwitchOff(true, null)).isTrue();
        }

        @Test
        @DisplayName("should return true when parameter is a simple type (String)")
        void shouldReturnTrueWhenSimpleTypeString() {
            assertThat(ParameterUtils.isSwitchOff(true, "hello")).isTrue();
        }

        @Test
        @DisplayName("should return true when parameter is a simple type (Integer)")
        void shouldReturnTrueWhenSimpleTypeInteger() {
            assertThat(ParameterUtils.isSwitchOff(true, 42)).isTrue();
        }

        @Test
        @DisplayName("should return true when parameter is a simple type (Long)")
        void shouldReturnTrueWhenSimpleTypeLong() {
            assertThat(ParameterUtils.isSwitchOff(true, 100L)).isTrue();
        }

        @Test
        @DisplayName("should return false when switch is on and parameter is a complex object")
        void shouldReturnFalseForComplexObject() {
            assertThat(ParameterUtils.isSwitchOff(true, new HashMap<>())).isFalse();
        }
    }

    // ---- isSwitchOff(boolean, List) ----

    @Nested
    @DisplayName("isSwitchOff(globalSwitch, rtObjectList)")
    class SwitchOffForList {

        @Test
        @DisplayName("should return true when global switch is off")
        void shouldReturnTrueWhenSwitchOff() {
            assertThat(ParameterUtils.isSwitchOff(false, Collections.singletonList("x"))).isTrue();
        }

        @Test
        @DisplayName("should return true when list is null")
        void shouldReturnTrueWhenListNull() {
            assertThat(ParameterUtils.isSwitchOff(true, null)).isTrue();
        }

        @Test
        @DisplayName("should return true when list is empty")
        void shouldReturnTrueWhenListEmpty() {
            assertThat(ParameterUtils.isSwitchOff(true, Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("should return false when switch is on and list is non-empty")
        void shouldReturnFalseForNonEmptyList() {
            assertThat(ParameterUtils.isSwitchOff(true, Collections.singletonList("x"))).isFalse();
        }
    }

    // ---- extractParameters ----

    @Nested
    @DisplayName("extractParameters")
    class ExtractParameters {

        @Test
        @DisplayName("should return collection as-is when input is a Collection")
        void shouldReturnCollectionAsIs() {
            List<String> list = Arrays.asList("a", "b");
            Collection<Object> result = ParameterUtils.extractParameters(list);
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        @DisplayName("should convert object array to collection")
        void shouldConvertObjectArray() {
            Object[] arr = {"x", "y", "z"};
            Collection<Object> result = ParameterUtils.extractParameters(arr);
            assertThat(result).containsExactly("x", "y", "z");
        }

        @Test
        @DisplayName("should extract unique values from Map")
        void shouldExtractFromMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("k1", "v1");
            map.put("k2", "v2");
            Collection<Object> result = ParameterUtils.extractParameters(map);
            assertThat(result).containsExactlyInAnyOrder("v1", "v2");
        }

        @Test
        @DisplayName("should deduplicate Map values")
        void shouldDeduplicateMapValues() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("k1", "same");
            map.put("k2", "same");
            Collection<Object> result = ParameterUtils.extractParameters(map);
            assertThat(result).containsExactly("same");
        }

        @Test
        @DisplayName("should wrap single object in singleton collection")
        void shouldWrapSingleObject() {
            Collection<Object> result = ParameterUtils.extractParameters("lonely");
            assertThat(result).containsExactly("lonely");
        }
    }

    // ---- toCollection ----

    @Nested
    @DisplayName("toCollection")
    class ToCollection {

        @Test
        @DisplayName("should return empty list for null input")
        void shouldReturnEmptyForNull() {
            assertThat(ParameterUtils.toCollection(null)).isEmpty();
        }

        @Test
        @DisplayName("should convert Object array to list")
        void shouldConvertObjectArray() {
            Object[] arr = {1, 2, 3};
            assertThat(ParameterUtils.toCollection(arr)).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("should return collection as-is")
        void shouldReturnCollectionAsIs() {
            List<String> list = Arrays.asList("a", "b");
            assertThat(ParameterUtils.toCollection(list)).isSameAs(list);
        }

        @Test
        @DisplayName("should wrap single value in singleton list")
        void shouldWrapSingleValue() {
            assertThat(ParameterUtils.toCollection("only")).containsExactly("only");
        }

        @Test
        @DisplayName("should wrap primitive array in singleton list")
        void shouldWrapPrimitiveArray() {
            int[] arr = {1, 2, 3};
            // primitive arrays are not expanded; wrapped as a single element
            Collection<Object> result = ParameterUtils.toCollection(arr);
            assertThat(result).hasSize(1);
        }
    }
}
