package com.baomidou.mybatisplus.enhance.util;

import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.util.*;

/**
 * Utility class for normalizing MyBatis Mapper parameters.
 *
 * <p>Used by interceptors to evaluate switch states and to extract deduplicated processing
 * objects from entities, arrays, collections, and {@code ParamMap} instances.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public class ParameterUtils {

    /**
     * Determines whether parameter-based enhancement should be skipped.
     *
     * @param globalSwitch    the global enhancement switch
     * @param parameterObject the Mapper parameter object
     * @return {@code true} if the switch is off, the parameter is {@code null}, or the parameter is a simple type
     */
    public static boolean isSwitchOff(boolean globalSwitch, Object parameterObject) {
        return !globalSwitch || Objects.isNull(parameterObject) || SimpleTypeRegistry.isSimpleType(parameterObject.getClass());
    }


    /**
     * Determines whether result-set enhancement should be skipped.
     *
     * @param globalSwitch the global enhancement switch
     * @param rtObjectList the query result list
     * @return {@code true} if the switch is off or the result list is {@code null} or empty
     */
    public static boolean isSwitchOff(boolean globalSwitch, List<Object> rtObjectList) {
        return !globalSwitch || Objects.isNull(rtObjectList) || CollectionUtils.isEmpty(rtObjectList);
    }

    /**
     * Extracts processing objects from the given parameter, supporting top-level
     * collections, arrays, and {@code Map} values. Nested containers are not
     * recursively expanded.
     *
     * @param parameterObject the Mapper parameter
     * @return a collection of candidate objects for enhancement processing
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Collection<Object> extractParameters(Object parameterObject) {
        if (parameterObject instanceof Collection) {
            return (Collection) parameterObject;
        } else if (ArrayUtils.isArray(parameterObject)) {
            return toCollection(parameterObject);
        } else if (parameterObject instanceof Map) {
            Collection<Object> parameters = new ArrayList<>();
            Map<String, Object> parameterMap = (Map) parameterObject;
            Set<Object> objectSet = new HashSet<>();
            parameterMap.forEach((k, v) -> {
                if (objectSet.add(v)) {
                    Collection<Object> collection = toCollection(v);
                    parameters.addAll(collection);
                }
            });
            return parameters;
        } else {
            return Collections.singleton(parameterObject);
        }
    }

    /**
     * Normalizes a single value, object array, or collection into a {@code Collection}.
     *
     * @param value the value to convert
     * @return a non-empty collection; an empty collection if the input is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object> toCollection(Object value) {
        if (Objects.isNull(value)) {
            return Collections.emptyList();
        }
        if (ArrayUtils.isArray(value) && !value.getClass().getComponentType().isPrimitive()) {
            return Arrays.asList((Object[]) value);
        } else if (Collection.class.isAssignableFrom(value.getClass())) {
            return (Collection<Object>) value;
        } else {
            return Collections.singletonList(value);
        }
    }


}
