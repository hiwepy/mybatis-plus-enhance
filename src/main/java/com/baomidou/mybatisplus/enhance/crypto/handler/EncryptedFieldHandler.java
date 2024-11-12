package com.baomidou.mybatisplus.enhance.crypto.handler;

public interface EncryptedFieldHandler {

    /**
     * 字段加密
     * @param value 待加密字段的值
     *
     * @return T 加密后的字段值
     * @param <T> 字段类型
     */
    <T> String encrypt(T value);

    /**
     * 字段解密
     * @param value 待解密字段的值
     * @return T 解密后的字段值
     * @param <T> 字段类型
     */
    <T> T decrypt(Object value, Class<T> rtType);

    /**
     * hmac 签名
     * @param value 待签名的值
     * @return 签名后的字符串
     * @param <T> 字段类型
     */
    <T> String hmac(T value);

}
