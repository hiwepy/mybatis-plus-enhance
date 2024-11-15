package com.baomidou.mybatisplus.enhance.crypto.handler;

import java.util.Optional;

/**
 * 数据签名读写提供者
 */
public interface DataSignatureReadWriteProvider {

    /**
     * 读取签名值
     * @param rawObject 数据对象
     * @param entityClass 对象类型
     * @return 签名值
     * @param <T> 对象类型
     */
   <T> Optional<Object> readSignature(Object rawObject, Class<T> entityClass);

    /**
     * 将签名值写出
     * @param rawObject 数据对象
     * @param signValue 签名值
     * @param <T> 对象类型
     */
    <T> void writeSignature(T rawObject, String signValue);

}
