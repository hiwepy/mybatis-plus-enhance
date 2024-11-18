package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;

public interface DataEncryptionHandler {

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param entity 参数
     * @param <T> 对象类型
     * @return 签名完成后是否继续执行数据更新操作
     */
    <T> boolean doEntityEncrypt(T entity);

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     * @return 签名完成后是否继续执行数据更新操作
     */
    boolean doWrapperEncrypt(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper);

    /**
     * 对原始对象进行解密
     * @param rawObject 单个原始对象
     * @param <T> 对象类型
     */
    <T> void doRawObjectDecrypt(Object rawObject, Class<T> entityClass);

}
