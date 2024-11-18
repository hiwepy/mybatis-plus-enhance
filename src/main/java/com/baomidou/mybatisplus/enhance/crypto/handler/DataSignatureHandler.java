package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;

public interface DataSignatureHandler {

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param entity 参数
     * @param <T> 对象类型
     * @return 签名完成后是否继续执行数据更新操作
     */
    <T> boolean doEntitySignature(T entity);

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     * @return 签名完成后是否继续执行数据更新操作
     */
    boolean doWrapperSignature(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper);

    /**
     * 对单个对象进行验签
     * @param rawObject 单个对象
     * @param <T> 对象类型
     */
    <T> void doSignatureVerification(Object rawObject, Class<T> entityClass);

}
