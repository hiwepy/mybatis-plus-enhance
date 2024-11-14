package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 需要签名或存储签名的字段用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface TableSignatureField {

    /**
     * 待加密字段的顺序，用于多个字段进行加密签名时保证加密字段的顺序
     */
    int order() default 0;

    /**
     * 是否作为签名结果的存储字段
     */
    boolean stored() default false;

}
