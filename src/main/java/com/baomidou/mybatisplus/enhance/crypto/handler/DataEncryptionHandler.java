package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;

public interface DataEncryptionHandler {

    /**
     * 是否开启数据加密
     */
    boolean isEncryptionOn();

    /**
     * 是否开启数据签名验证
     */
    boolean isSignVerify();

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param entity 参数
     * @param <T> 对象类型
     */
    <T> void doEntitySignature(T entity);

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     */
    void doWrapperSignature(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper);



}
