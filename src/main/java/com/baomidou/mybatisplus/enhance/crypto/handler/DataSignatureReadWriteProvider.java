package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

import java.util.Optional;

/**
 * 数据签名读写提供者
 */
public interface DataSignatureReadWriteProvider {

    /**
     * 读取签名值
     * @param rawObject 数据对象
     * @param tableInfo 对象表信息
     * @return 签名值
     */
    Optional<Object> readSignature(Object rawObject, TableInfo tableInfo);

    /**
     * 将签名值写出
     * @param rawObject 数据对象
     * @param tableInfo 对象表信息
     * @param signValue 签名值
     * @param <T> 对象类型
     * @return 签名写出后是否继续执行数据更新操作
     */
    boolean writeSignature(Object rawObject, TableInfo tableInfo, AbstractWrapper<?,?,?> updateWrapper, String signValue);

}
