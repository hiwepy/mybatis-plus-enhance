package com.baomidou.mybatisplus.enhance.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.service.IEnhanceService;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * EnhanceServiceImpl
 * <p>
 * 1. 数据签名和验签
 * 2. 重写 getSignedOne、getSignedOneOpt、getSignedMap、getSignedObj
 * 3. 重写 getBaseEnhanceMapper、getDataSignatureHandler
 *
 * @param <M> Mapper
 * @param <T> Entity
 */
public abstract class EnhanceServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements IEnhanceService<T> {

    /**
     * 数据签名和验签 Handler
     */
    @Autowired
    protected DataSignatureHandler dataSignatureHandler;

    @Override
    public <RT> boolean doEntitySignature(RT entity) {
        return getDataSignatureHandler().doEntitySignature(entity);
    }

    @Override
    public <RT> void doSignatureVerification(RT rowObject, Class<?> entityClass) {
        getDataSignatureHandler().doSignatureVerification(rowObject, entityClass);
    }

    /**
     * 批量插入
     *
     * @param entityList ignore
     * @param batchSize  ignore
     * @return ignore
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveBatchSigned(Collection<T> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.INSERT_ONE);
        Set<Serializable> idSet = new HashSet<>(entityList.size());
        try {
            return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
                // 保存数据
                sqlSession.insert(sqlStatement, entity);
                // 获取主键值
                idSet.add(TableFieldHelper.getKeyValue(entity));
            });
        } finally {
            // 批量签名
            this.doSignatureByBatchIds(idSet);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatchSigned(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        Set<Serializable> idSet = new HashSet<>(entityList.size());
        try {
            return SqlHelper.saveOrUpdateBatch(getSqlSessionFactory(), this.currentMapperClass(), this.log, entityList, batchSize, (sqlSession, entity) -> {
                Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                idSet.add((Serializable) idVal);
                return StringUtils.checkValNull(idVal)
                        || CollectionUtils.isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
            }, (sqlSession, entity) -> {
                MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
                param.put(Constants.ENTITY, entity);
                sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);
                Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                idSet.add((Serializable) idVal);
            });
        } finally {
            // 批量签名
            this.doSignatureByBatchIds(idSet);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateBatchSignedById(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        Set<Serializable> idSet = new HashSet<>(entityList.size());
        try {
            String sqlStatement = getSqlStatement(SqlMethod.UPDATE_BY_ID);
            return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
                doEntitySignature(entity);
                MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
                param.put(Constants.ENTITY, entity);
                sqlSession.update(sqlStatement, param);

                Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                idSet.add((Serializable) idVal);
            });
        } finally {
            // 批量签名
            this.doSignatureByBatchIds(idSet);
        }
    }

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateSigned(T entity) {
        boolean result = getBaseMapper().insertOrUpdate(entity);
        if (result) {
            this.doSignatureById(TableFieldHelper.getKeyValue(entity));
        }
        return result;
    }

    @Override
    public T getSignedOne(Wrapper<T> queryWrapper, boolean throwEx) {
        // 1、调用selectOne查询数据
        T entity = getBaseMapper().selectOne(queryWrapper, throwEx);
        if (entity == null) {
            return null;
        }
        // 2、验证签名
        this.doSignatureVerification(entity, entity.getClass());
        // 3、返回数据
        return entity;
    }

    @Override
    public Optional<T> getSignedOneOpt(Wrapper<T> queryWrapper, boolean throwEx) {
        // 1、调用selectOne查询数据
        T entity = getBaseMapper().selectOne(queryWrapper, throwEx);
        if (entity == null) {
            return Optional.empty();
        }
        // 2、验证签名
        this.doSignatureVerification(entity, entity.getClass());
        // 3、返回数据
        return Optional.of(entity);
    }

    @Override
    public Map<String, Object> getSignedMap(Wrapper<T> queryWrapper) {
        // 1、调用selectMaps查询数据
        List<Map<String, Object>> rtList = getBaseMapper().selectMaps(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach( rowMap -> this.doSignatureVerification(rowMap, queryWrapper.getEntity().getClass()));
        }
        return SqlHelper.getObject(log, rtList);
    }

    @Override
    public <V> V getSignedObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return SqlHelper.getObject(log, listSignedObjs(queryWrapper, mapper));
    }

    public DataSignatureHandler getDataSignatureHandler() {
        return dataSignatureHandler;
    }

}
