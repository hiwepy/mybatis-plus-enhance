package com.baomidou.mybatisplus.enhance.crypto.annotation;

/**
 * 需要加解密的实体类用这个注解
 * @author wandl
 */
public @interface EncryptedTable {

    /**
     * 该数据表是否进行单表数据存储完整性验证
     */
    boolean hmac() default false;

}
