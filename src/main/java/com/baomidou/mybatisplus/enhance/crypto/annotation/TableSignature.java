package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 需要签名的实体类用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TableSignature {

    /**
     * 是否将实体类的所有字段进行联合签名
     */
    boolean unionAll() default false;

}
