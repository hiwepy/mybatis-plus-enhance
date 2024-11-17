package com.baomidou.mybatisplus.enhance.sensitive.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;

public interface DataMaskingHandler {

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param entity 参数
     * @param <T> 对象类型
     */
    <T> void doQueryMasking(T entity);

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     */
    void doQueryMasking(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper);

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param entity 参数
     * @param <T> 对象类型
     */
    <T> void doResultMasking(T entity);


}
