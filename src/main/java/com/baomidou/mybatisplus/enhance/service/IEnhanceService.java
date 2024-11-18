package com.baomidou.mybatisplus.enhance.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 增强 Service 接口
 * @param <T> 实体类
 */
public interface IEnhanceService<T> extends IService<T> {

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param entity 参数
     * @param <RT> 对象类型
     */
    <RT> void doEntitySignature(RT entity);

    /**
     * 对单个对象进行验签
     * @param rowObject 单个对象
     * @param <RT> 对象类型
     */
    <RT> void doSignatureVerification(RT rowObject, Class<?> entityClass);

    /**
     * 插入一条记录（选择字段，策略插入）
     *
     * @param entity 实体对象
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveSigned(T entity) {
        this.doEntitySignature(entity);
        return SqlHelper.retBool(getBaseMapper().insert(entity));
    }

    /**
     * 插入（批量）
     *
     * @param entityList 实体对象集合
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveBatchSigned(Collection<T> entityList) {
        return saveBatchSigned(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 插入（批量）
     *
     * @param entityList 实体对象集合
     * @param batchSize  插入批次数量
     */
    boolean saveBatchSigned(Collection<T> entityList, int batchSize);

    /**
     * 批量修改插入
     *
     * @param entityList 实体对象集合
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveOrUpdateBatchSigned(Collection<T> entityList) {
        return saveOrUpdateBatchSigned(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 批量修改插入
     *
     * @param entityList 实体对象集合
     * @param batchSize  每次的数量
     */
    boolean saveOrUpdateBatchSigned(Collection<T> entityList, int batchSize);

    /**
     * 根据 ID 选择修改
     *
     * @param entity 实体对象
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean updateSignedById(T entity) {
        this.doEntitySignature(entity);
        return SqlHelper.retBool(getBaseMapper().updateById(entity));
    }

    /**
     * 根据ID 批量更新
     *
     * @param entityList 实体对象集合
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean updateBatchSignedById(Collection<T> entityList) {
        return updateBatchSignedById(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 根据ID 批量更新
     *
     * @param entityList 实体对象集合
     * @param batchSize  更新批次数量
     */
    boolean updateBatchSignedById(Collection<T> entityList, int batchSize);

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     */
    boolean saveOrUpdateSigned(T entity);

    /**
     * 根据 ID 查询 签名验证通过的数据
     * @param id 主键ID
     */
    default T getSignedById(Serializable id) {
        // 1、调用selectById查询数据
        T entity = getBaseMapper().selectById(id);
        if (entity == null) {
            return null;
        }
        // 2、验证签名
        this.doSignatureVerification(entity, entity.getClass());
        // 3、返回数据
        return entity;
    }

    /**
     * 根据 ID 查询，返回一个Option对象
     *
     * @param id 主键ID
     * @return {@link Optional}
     */
    default Optional<T> getSignedOptById(Serializable id) {
        return Optional.ofNullable(getSignedById(id));
    }

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList 主键ID列表
     */
    default List<T> listSignedByIds(Collection<? extends Serializable> idList) {
        // 1、调用selectBatchIds查询数据
        List<T> rtList = getBaseMapper().selectBatchIds(idList);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList        idList 主键ID列表(不能为 null 以及 empty)
     * @param resultHandler resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedByIds(Collection<? extends Serializable> idList, ResultHandler<T> resultHandler){
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、调用结果处理器
            resultHandler.handleResult(context);
            // 3、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
        };
        // 4、调用selectBatchIds查询数据
        getBaseMapper().selectBatchIds(idList, wapperHandler);
    }

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param columnMap 表字段 map 对象
     */
    default List<T> listSignedByMap(Map<String, Object> columnMap) {
        // 1、调用selectByMap查询数据
        List<T> rtList = getBaseMapper().selectByMap(columnMap);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param columnMap     表字段 map 对象
     * @param resultHandler resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedByMap(Map<String, Object> columnMap, ResultHandler<T> resultHandler) {
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、调用结果处理器
            resultHandler.handleResult(context);
            // 3、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
        };
        // 4、调用selectByMap查询数据
        getBaseMapper().selectByMap(columnMap, wapperHandler);
    }

    /**
     * 根据 Wrapper，查询一条记录 <br/>
     * <p>结果集，如果是多个会抛出异常，随机取一条加上限制条件 wrapper.last("LIMIT 1")</p>
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default T getSignedOne(Wrapper<T> queryWrapper) {
        return getSignedOne(queryWrapper, true);
    }

    /**
     * 根据 Wrapper，查询一条记录 <br/>
     * <p>结果集，如果是多个会抛出异常，随机取一条加上限制条件 wrapper.last("LIMIT 1")</p>
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return {@link Optional} 返回一个Optional对象
     */
    default Optional<T> getSignedOneOpt(Wrapper<T> queryWrapper) {
        return getSignedOneOpt(queryWrapper, true);
    }

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param throwEx      有多个 result 是否抛出异常
     */
    T getSignedOne(Wrapper<T> queryWrapper, boolean throwEx);

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param throwEx      有多个 result 是否抛出异常
     * @return {@link Optional} 返回一个Optional对象
     */
    Optional<T> getSignedOneOpt(Wrapper<T> queryWrapper, boolean throwEx);

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    Map<String, Object> getSignedMap(Wrapper<T> queryWrapper);

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param mapper       转换函数
     */
    <V> V getSignedObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper);

    /**
     * 查询列表
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default List<T> listSigned(Wrapper<T> queryWrapper) {
        // 1、调用selectList查询数据
        List<T> rtList = getBaseMapper().selectList(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper  实体对象封装操作类（可以为 null）
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSigned(Wrapper<T> queryWrapper, ResultHandler<T> resultHandler){
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、调用结果处理器
            resultHandler.handleResult(context);
            // 3、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
        };
        // 4、调用selectList查询数据
        getBaseMapper().selectList(queryWrapper, wapperHandler);
    }

    /**
     * 查询列表
     *
     * @param page         分页条件
     * @param queryWrapper queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 列表数据
     * @since 3.5.3.2
     */
    default List<T> listSigned(IPage<T> page, Wrapper<T> queryWrapper) {
        // 1、调用selectList查询数据
        List<T> rtList = getBaseMapper().selectList(page, queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 根据 entity 条件，查询全部记录（并翻页）
     * @param page          分页查询条件
     * @param queryWrapper  实体对象封装操作类（可以为 null）
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSigned(IPage<T> page, Wrapper<T> queryWrapper, ResultHandler<T> resultHandler){
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、调用结果处理器
            resultHandler.handleResult(context);
            // 3、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
        };
        // 4、调用selectList查询数据
        getBaseMapper().selectList(page, queryWrapper, wapperHandler);
    }

    /**
     * 查询所有
     *
     * @see Wrappers#emptyWrapper()
     */
    default List<T> listSigned() {
        return listSigned(Wrappers.emptyWrapper());
    }

    /**
     * 分页查询单表数据
     *
     * @param page 分页条件
     * @return 列表数据
     * @since 3.5.3.2
     */
    default List<T> listSigned(IPage<T> page) {
        return listSigned(page, Wrappers.emptyWrapper());
    }

    /**
     * 翻页查询
     *
     * @param page         翻页对象
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default <E extends IPage<T>> E pageSigned(E page, Wrapper<T> queryWrapper) {
        // 1、查询数据
        page.setRecords(getBaseMapper().selectList(page, queryWrapper));
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(page.getRecords())) {
            page.getRecords().forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return page;
    }

    /**
     * 无条件翻页查询
     *
     * @param page 翻页对象
     * @see Wrappers#emptyWrapper()
     */
    default <E extends IPage<T>> E pageSigned(E page) {
        return pageSigned(page, Wrappers.emptyWrapper());
    }

    /**
     * 查询列表
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default List<Map<String, Object>> listSignedMaps(Wrapper<T> queryWrapper) {
        // 1、调用selectMaps查询数据
        List<Map<String, Object>> rtList = getBaseMapper().selectMaps(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach( rowMap -> this.doSignatureVerification(rowMap, queryWrapper.getEntity().getClass()));
        }
        return rtList;
    }

    /**
     * 根据 Wrapper 条件，查询全部记录
     *
     * @param queryWrapper  实体对象封装操作类
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedMaps(Wrapper<T> queryWrapper, ResultHandler<Map<String, Object>> resultHandler){
        // 1、封装结果处理器
        ResultHandler<Map<String, Object>> wapperHandler = context -> {
            // 2、调用结果处理器
            resultHandler.handleResult(context);
            // 3、验证签名
            this.doSignatureVerification(context.getResultObject(), queryWrapper.getEntity().getClass());
        };
        // 4、调用selectMaps查询数据
        getBaseMapper().selectMaps(queryWrapper, wapperHandler);
    }

    /**
     * 查询列表
     *
     * @param page         分页条件
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 列表数据
     * @since 3.5.3.2
     */
    default List<Map<String, Object>> listSignedMaps(IPage<? extends Map<String, Object>> page, Wrapper<T> queryWrapper) {
        // 1、调用selectMaps查询数据
        List<Map<String, Object>> rtList = getBaseMapper().selectMaps(page, queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach( rowMap -> this.doSignatureVerification(rowMap, queryWrapper.getEntity().getClass()));
        }
        return rtList;
    }

    /**
     * 根据 Wrapper 条件，查询全部记录（并翻页）
     *
     * @param page          分页查询条件
     * @param queryWrapper  实体对象封装操作类
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedMaps(IPage<? extends Map<String, Object>> page, Wrapper<T> queryWrapper, ResultHandler<Map<String, Object>> resultHandler){
        // 1、封装结果处理器
        ResultHandler<Map<String, Object>> wapperHandler = context -> {
            // 2、调用结果处理器
            resultHandler.handleResult(context);
            // 3、验证签名
            this.doSignatureVerification(context.getResultObject(), queryWrapper.getEntity().getClass());
        };
        // 4、调用selectMaps查询数据
        getBaseMapper().selectMaps(page, queryWrapper, wapperHandler);
    }

    /**
     * 查询所有列表
     *
     * @see Wrappers#emptyWrapper()
     */
    default List<Map<String, Object>> listSignedMaps() {
        return listSignedMaps(Wrappers.emptyWrapper());
    }

    /**
     * 查询列表
     *
     * @param page 分页条件
     * @see Wrappers#emptyWrapper()
     */
    default List<Map<String, Object>> listSignedMaps(IPage<? extends Map<String, Object>> page) {
        return listSignedMaps(page, Wrappers.emptyWrapper());
    }

    /**
     * 查询全部记录
     */
    default <E> List<E> listSignedObjs() {
        // 1、调用selectObjs查询数据
        List<E> rtList = getBaseMapper().selectObjs(null);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 查询全部记录
     *
     * @param mapper 转换函数
     */
    default <V> List<V> listSignedObjs(Function<? super Object, V> mapper) {
        return listSignedObjs(Wrappers.emptyWrapper(), mapper);
    }

    /**
     * 根据 Wrapper 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default <E> List<E> listSignedObjs(Wrapper<T> queryWrapper) {
        // 1、调用selectObjs查询数据
        List<E> rtList = getBaseMapper().selectObjs(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 根据 Wrapper 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param mapper       转换函数
     */
    default <V> List<V> listSignedObjs(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return listSignedObjs(queryWrapper).stream().filter(Objects::nonNull).map(mapper).collect(Collectors.toList());
    }

    /**
     * 根据 Wrapper 条件，查询全部记录
     * <p>注意： 只返回第一个字段的值</p>
     *
     * @param queryWrapper  实体对象封装操作类（可以为 null）
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default <E> void listSignedObjs(Wrapper<T> queryWrapper, ResultHandler<E> resultHandler){
        // 1、封装结果处理器
        ResultHandler<E> wapperHandler = context -> {
            // 2、调用结果处理器
            resultHandler.handleResult(context);
            // 3、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
        };
        // 3、调用selectObjs查询数据
        getBaseMapper().selectObjs(queryWrapper, wapperHandler);
    }

    /**
     * 翻页查询
     *
     * @param page         翻页对象
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default <E extends IPage<Map<String, Object>>> E pageSignedMaps(E page, Wrapper<T> queryWrapper) {
        // 1、查询数据
        page.setRecords(getBaseMapper().selectMaps(page, queryWrapper));
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(page.getRecords())) {
            page.getRecords().forEach( rowMap -> this.doSignatureVerification(rowMap, queryWrapper.getEntity().getClass()));
        }
        return page;
    }

    /**
     * 无条件翻页查询
     *
     * @param page 翻页对象
     * @see Wrappers#emptyWrapper()
     */
    default <E extends IPage<Map<String, Object>>> E pageSignedMaps(E page) {
        return pageMaps(page, Wrappers.emptyWrapper());
    }

    /**
     * 根据 ID 对匹配的实体进行表签名
     *
     * @param id 主键ID
     */
    default void doSignatureById(Serializable id){
        // 1、根据 ID 查询原始数据
        T entity = getBaseMapper().selectById(id);
        // 2、如果原始数据不为空，则对原始数据进行签名
        if (Objects.nonNull(entity)) {
            // 2.1、对原始数据进行签名
            this.doEntitySignature(entity);
            // 2.2、更新数据
            this.updateById(entity);
        }
    }

    /**
     * 根据 ID 批量对匹配的实体进行表签名
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Transactional(rollbackFor = Exception.class)
    default void doSignatureByBatchIds(Collection<? extends Serializable> idList) {
        // 1、根据 ID 批量查询原始数据
        List<T> rtList = getBaseMapper().selectBatchIds(idList);
        if(CollectionUtils.isNotEmpty(rtList)){
            // 2、对原始数据进行签名
            rtList.forEach(this::doEntitySignature);
            // 3、批量更新数据
            this.updateBatchById(rtList);
        }
    }

    /**
     * 查询（根据 columnMap 条件）匹配的实体进行表签名
     *
     * @param columnMap 表字段 map 对象
     */
    @Transactional(rollbackFor = Exception.class)
    default void doSignatureByMap(Map<String, Object> columnMap) {
        // 1、根据 columnMap 查询原始数据
        List<T> rtList = getBaseMapper().selectByMap(columnMap);
        if(CollectionUtils.isNotEmpty(rtList)){
            // 2、对原始数据进行签名
            rtList.forEach(this::doEntitySignature);
            // 3、批量更新数据
            this.updateBatchById(rtList);
        }
    }

    /**
     * 根据 Wrapper 条件，对匹配的实体进行表签名
     * @param queryWrappers 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    @Transactional(rollbackFor = Exception.class)
    default void doSignatureByWrappers(List<Wrapper<T>> queryWrappers) {
        for (Wrapper<T> queryWrapper : queryWrappers) {
            // 1、根据 Wrapper 条件查询原始数据
            List<T> rtList = getBaseMapper().selectList(queryWrapper);
            if(CollectionUtils.isNotEmpty(rtList)){
                // 2、对原始数据进行签名
                rtList.forEach(this::doEntitySignature);
                // 3、批量更新数据
                this.updateBatchById(rtList);
            }
        }
    }

    /**
     * 根据 ID 对匹配的实体进行表签名
     *
     * @param id 主键ID
     */
    default void doSignatureVerificationById(Serializable id){
        // 1、根据 ID 查询原始数据
        T entity = getBaseMapper().selectById(id);
        // 2、如果原始数据不为空，则对原始数据进行验签
        if (Objects.nonNull(entity)) {
            // 2.1、对原始数据进行验签
            this.doSignatureVerification(entity, entity.getClass());
        }
    }

    /**
     * 根据 ID 批量对匹配的实体进行表签名
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    default void doSignatureVerificationByBatchIds(Collection<? extends Serializable> idList){
        // 1、根据 ID 批量查询原始数据
        List<T> rtList = getBaseMapper().selectBatchIds(idList);
        if(CollectionUtils.isNotEmpty(rtList)){
            // 2、对原始数据进行验签
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
    }

    /**
     * 查询（根据 columnMap 条件）匹配的实体进行表签名
     *
     * @param columnMap 表字段 map 对象
     */
    default void doSignatureVerificationByMap(Map<String, Object> columnMap){
        // 1、根据 columnMap 查询原始数据
        List<T> rtList = getBaseMapper().selectByMap(columnMap);
        if(CollectionUtils.isNotEmpty(rtList)){
            // 2、对原始数据进行验签
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
    }

    /**
     * 根据 Wrapper 条件，对匹配的实体进行表签名
     * @param queryWrappers 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default void doSignatureVerificationByWrappers(List<Wrapper<T>> queryWrappers){
        for (Wrapper<T> queryWrapper : queryWrappers) {
            // 1、根据 Wrapper 条件查询原始数据
            List<T> rtList = getBaseMapper().selectList(queryWrapper);
            if(CollectionUtils.isNotEmpty(rtList)){
                // 2、对原始数据进行验签
                rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
            }
        }
    }

}
