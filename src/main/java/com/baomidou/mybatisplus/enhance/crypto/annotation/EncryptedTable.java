package com.baomidou.mybatisplus.enhance.crypto.annotation;

/**
 * 用于标记数据表是否进行加密存储
 * @author wandl
 */
public @interface EncryptedTable {

    /**
     * 该数据表是否进行单表数据存储完整性验证
     */
    boolean hmac() default false;

}
