package com.baomidou.mybatisplus.enhance.interceptor.inner;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveJSONField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveJSONFieldKey;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveType;
import com.baomidou.mybatisplus.enhance.sensitive.handler.DataMaskingHandler;
import com.baomidou.mybatisplus.enhance.sensitive.handler.SensitiveTypeRegisty;
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
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;

/**
 * 数据脱敏(Data Masking)拦截器
 * 1、用于对查询/更新数据时对字段进行脱敏操作
 * 2、查询操作执行顺序：DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor -> DataMaskingInnerInterceptor
 * 2、更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor -> DataMaskingInnerInterceptor
 */
@Slf4j
public class DataMaskingInnerInterceptor extends JsqlParserSupport implements EnhanceInnerInterceptor {

    /**
     * 数据数据脱敏(Data Masking) Handler
     */
    @Getter
    private final DataMaskingHandler dataMaskingHandler;
    /**
     * 是否开启数据查询时脱敏
     */
    @Getter
    private final boolean maskingSet;
    /**
     * 是否开启数据保存时脱敏
     */
    @Getter
    private final boolean maskingGet;

    public DataMaskingInnerInterceptor(DataMaskingHandler dataMaskingHandler) {
        this(dataMaskingHandler, true, false);
    }

    public DataMaskingInnerInterceptor(DataMaskingHandler dataMaskingHandler, boolean maskingGet) {
        this(dataMaskingHandler, maskingGet, false);
    }

    public DataMaskingInnerInterceptor(DataMaskingHandler dataMaskingHandler, boolean maskingGet, boolean maskingSet) {
        this.dataMaskingHandler = dataMaskingHandler;
        this.maskingSet = maskingSet;
        this.maskingGet = maskingGet;
    }

    /**
     * 如果查询条件是签名数据列，那么要将查询条件进行数据签名。
     * 例如，手机号签名存储后，按手机号查询时，先把要查询的手机号进行签名，再和数据库存储的签名数据进行匹配
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(maskingGet, parameterObject)) {
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
            if (Objects.isNull(param) || SimpleTypeRegistry.isSimpleType(param.getClass()) || param instanceof AbstractWrapper) {
                // Wrapper、String类型查询参数，无法获取参数变量上的注解，无法确认是否需要签名，因此不做判断
                continue;
            }
            // 对参数进行签名处理
            getDataMaskingHandler().doQueryMasking(param);
        }
    }

    /**
     * 新增、更新数据时，如果包含隐私数据，则进行签名
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement mappedStatement, Object parameterObject) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(maskingSet, parameterObject)) {
            return;
        }
        // 2、通过MybatisPlus自带API（save、insert等）新增数据库时
        if (!(parameterObject instanceof Map)) {
            // 对参数进行签名处理
            getDataMaskingHandler().doQueryMasking(parameterObject);
            return;
        }
        // 3、Map类型参数
        Map<?,?> paramMap = (Map<?,?>) parameterObject;
        Object param;
        // 4、通过MybatisPlus自带API（update、updateById等）修改数据库时
        if (paramMap.containsKey(Constants.ENTITY) && null != (param = paramMap.get(Constants.ENTITY))) {
            // 对参数进行签名处理
            getDataMaskingHandler().doQueryMasking(param);
            return;
        }
        // 5、通过在mapper.xml中自定义API修改数据库时
        if (paramMap.containsKey(EnhanceConstants.CUSTOM_ENTITY) && null != (param = paramMap.get(EnhanceConstants.CUSTOM_ENTITY))) {
            // 对参数进行签名处理
            getDataMaskingHandler().doQueryMasking(param);
            return;
        }
        // 6、通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
        if (paramMap.containsKey(Constants.WRAPPER) && null != (param = paramMap.get(Constants.WRAPPER))) {
            // 6.1、判断是否是UpdateWrapper、LambdaUpdateWrapper类型
            if (param instanceof Update && param instanceof AbstractWrapper) {
                Class<?> entityClass = mappedStatement.getParameterMap().getType();
                getDataMaskingHandler().doQueryMasking(entityClass, (AbstractWrapper<?,?,?>) param);
            }
        }
    }

    @Override
    public void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(maskingGet, rtList)) {
            return;
        }
        for (Object rawObject : rtList) {
            if(Objects.isNull(rawObject) || SimpleTypeRegistry.isSimpleType(rawObject.getClass())){
                continue;
            }
            getDataMaskingHandler().doResultMasking(rawObject);
        }
    }
}
