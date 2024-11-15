package com.baomidou.mybatisplus.enhance.interceptor.inner;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.util.EnhanceConstants;
import com.baomidou.mybatisplus.enhance.util.ParameterUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据签名和验签拦截器
 * 1、用于对新增/更新数据进行签名操作
 * 2、用于对查询数据进行验签操作
 * 3、更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
 */
@Slf4j
public class DataSignatureInnerInterceptor extends JsqlParserSupport implements EnhanceInnerInterceptor {

    /**
     * 数据签名和验签 Handler
     */
    @Getter
    private final DataSignatureHandler dataSignatureHandler;
    /**
     * 是否开启数据签名
     */
    @Getter
    private final boolean signSwitch;
    /**
     * 是否开启数据签名验证
     */
    @Getter
    private final boolean signVerify;

    public DataSignatureInnerInterceptor(DataSignatureHandler dataSignatureHandler) {
        this(dataSignatureHandler, true, false);
    }

    public DataSignatureInnerInterceptor(DataSignatureHandler dataSignatureHandler, boolean signSwitch) {
        this(dataSignatureHandler, signSwitch, false);
    }

    public DataSignatureInnerInterceptor(DataSignatureHandler dataSignatureHandler, boolean signSwitch, boolean signVerify) {
        this.dataSignatureHandler = dataSignatureHandler;
        this.signSwitch = signSwitch;
        this.signVerify = signVerify;
    }

    /**
     * 如果查询条件是签名数据列，那么要将查询条件进行数据签名。
     * 例如，手机号签名存储后，按手机号查询时，先把要查询的手机号进行签名，再和数据库存储的签名数据进行匹配
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(signSwitch, parameterObject)) {
            return;
        }
        // 2、如果参数
        if (!(parameterObject instanceof Map)) {
            return;
        }
        // 3、Map类型参数
        Map<?,?> paramMap = (Map<?,?>) parameterObject;
        // 4、参数去重，否则多次签名会导致查询失败
        Set<?> set = new HashSet<>(paramMap.values());
        // 5、遍历参数，进行签名处理
        for (Object param : set) {
            /*
             *  仅支持类型是自定义Entity的参数，不支持mapper的参数是QueryWrapper、String等，例如：
             *
             *  支持：findList(@Param(value = "query") UserEntity query);
             *  支持：findPage(@Param(value = "query") UserEntity query, Page<UserEntity> page);
             *
             *  不支持：findOne(@Param(value = "mobile") String mobile);
             *  不支持：findList(QueryWrapper wrapper);
             */
            if (param instanceof AbstractWrapper || param instanceof String) {
                // Wrapper、String类型查询参数，无法获取参数变量上的注解，无法确认是否需要签名，因此不做判断
                continue;
            }
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(param);
        }
    }

    /**
     * 新增、更新数据时，如果包含隐私数据，则进行签名
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement mappedStatement, Object parameterObject) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(signSwitch, parameterObject)) {
            return;
        }
        // 2、通过MybatisPlus自带API（save、insert等）新增数据库时
        if (!(parameterObject instanceof Map)) {
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(parameterObject);
            return;
        }
        // 3、Map类型参数
        Map<?,?> paramMap = (Map<?,?>) parameterObject;
        Object param;
        // 4、通过MybatisPlus自带API（update、updateById等）修改数据库时
        if (paramMap.containsKey(Constants.ENTITY) && null != (param = paramMap.get(Constants.ENTITY))) {
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(param);
            return;
        }
        // 5、通过在mapper.xml中自定义API修改数据库时
        if (paramMap.containsKey(EnhanceConstants.CUSTOM_ENTITY) && null != (param = paramMap.get(EnhanceConstants.CUSTOM_ENTITY))) {
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(param);
            return;
        }
        // 6、通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
        if (paramMap.containsKey(Constants.WRAPPER) && null != (param = paramMap.get(Constants.WRAPPER))) {
            // 6.1、判断是否是UpdateWrapper、LambdaUpdateWrapper类型
            if (param instanceof Update && param instanceof AbstractWrapper) {
                Class<?> entityClass = mappedStatement.getParameterMap().getType();
                getDataSignatureHandler().doWrapperSignature(entityClass, (AbstractWrapper<?,?,?>) param);
            }
        }
    }

    @Override
    public void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(signVerify, rtList)) {
            return;
        }
        for (Object object : rtList) {
            // 逐一验签
            getDataSignatureHandler().doSignatureVerification(object);
        }
    }

}
