package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 该注解用于标记存储在数据库中的字段，用于加密和解密
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TableHmacField {
}
