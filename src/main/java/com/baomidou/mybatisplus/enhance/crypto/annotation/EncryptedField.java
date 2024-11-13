package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 需要加解密的字段用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EncryptedField {

    /**
     * 该字段是否需要进行HMAC签名，当多个字段进行加密签名时，会将所有字段排序后，获取字段值并使用 | 拼接后进行HMAC签名
     */
    boolean hmac() default false;

    /**
     * 待加密字段的顺序，用于多个字段进行加密签名时保证加密字段的顺序
     */
    int order() default 0;

}
