package com.baomidou.mybatisplus.enhance.util;

import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.util.*;

public class ParameterUtils {

    public static boolean isSwitchOff(boolean globalSwitch, Object parameterObject) {
        return !globalSwitch || Objects.isNull(parameterObject) || SimpleTypeRegistry.isSimpleType(parameterObject.getClass());
    }


    public static boolean isSwitchOff(boolean globalSwitch, List<Object> rtObjectList) {
        return !globalSwitch || Objects.isNull(rtObjectList) || CollectionUtils.isEmpty(rtObjectList);
    }

    /**
     * 提取特殊key值 (只支持外层参数,嵌套参数不考虑)
     * List<Map>虽然这种写法目前可以进去提取et,但不考虑再提取list等其他类型,只做简单参数提取
     *
     * @param parameterObject 参数
     * @return 预期可能为填充参数值
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

    @SuppressWarnings("unchecked")
    public static Collection<Object> toCollection(Object value) {
        if (value == null) {
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
