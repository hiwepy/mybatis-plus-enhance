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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public <RT> void doEntitySignature(RT entity) {
        getDataSignatureHandler().doEntitySignature(entity);
    }

    @Override
    public <RT> void doSignatureVerification(RT rowObject) {
        getDataSignatureHandler().doSignatureVerification(rowObject);
    }

    @Override
    public <RT> void doSignatureVerification(Map<String, Object> rowMap, Class<?> entityClass) {
        getDataSignatureHandler().doSignatureVerification(rowMap, entityClass);
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
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
            doEntitySignature(entity);
            sqlSession.insert(sqlStatement, entity);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatchSigned(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        return SqlHelper.saveOrUpdateBatch(getSqlSessionFactory(), this.currentMapperClass(), this.log, entityList, batchSize, (sqlSession, entity) -> {
            Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
            return StringUtils.checkValNull(idVal)
                    || CollectionUtils.isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
        }, (sqlSession, entity) -> {
            doEntitySignature(entity);
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateBatchSignedById(Collection<T> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.UPDATE_BY_ID);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
            doEntitySignature(entity);
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            sqlSession.update(sqlStatement, param);
        });
    }

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return boolean
     */
    @Override
    public boolean saveOrUpdateSigned(T entity) {
        doEntitySignature(entity);
        return getBaseMapper().insertOrUpdate(entity);
    }

    @Override
    public T getSignedOne(Wrapper<T> queryWrapper, boolean throwEx) {
        // 1、调用selectOne查询数据
        T entity = getBaseMapper().selectOne(queryWrapper, throwEx);
        if (entity == null) {
            return null;
        }
        // 2、验证签名
        doSignatureVerification(entity);
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
        getDataSignatureHandler().doSignatureVerification(entity);
        // 3、返回数据
        return Optional.of(entity);
    }

    @Override
    public Map<String, Object> getSignedMap(Wrapper<T> queryWrapper) {
        // 1、调用selectMaps查询数据
        List<Map<String, Object>> rtList = getBaseMapper().selectMaps(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach( rowMap -> getDataSignatureHandler().doSignatureVerification(rowMap, queryWrapper.getEntity().getClass()));
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
