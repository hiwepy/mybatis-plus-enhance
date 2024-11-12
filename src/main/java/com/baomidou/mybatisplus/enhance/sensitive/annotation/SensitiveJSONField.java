package com.baomidou.mybatisplus.enhance.sensitive.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对json内的key_value进行脱敏
 * @author chenhaiyang
 */
@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SensitiveJSONField {
    /**
     * 需要脱敏的字段的数组
     * @return 返回结果
     */
    SensitiveJSONFieldKey[] sensitivelist();
}
