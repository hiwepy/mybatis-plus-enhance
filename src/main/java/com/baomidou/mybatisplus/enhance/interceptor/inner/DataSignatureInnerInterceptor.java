package com.baomidou.mybatisplus.enhance.interceptor.inner;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignature;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignatureField;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import util.EncryptedFieldHelper;
import util.EnhanceConstants;
import util.ParameterUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据签名和验签拦截器
 * 1、用于对新增/更新数据进行签名操作
 * 2、用于对查询数据进行验签操作
 * 3、更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
 */
@Slf4j
public class DataSignatureInnerInterceptor extends JsqlParserSupport implements EnhanceInnerInterceptor {

    /**
     * 变量占位符正则
     */
    private static final Pattern PARAM_PAIRS_RE = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(" + Constants.WRAPPER_PARAM + "\\d+)\\}");
    /**
     * 加解密处理器，加解密的情况都在该处理器中自行判断
     */
    @Getter
    private final EncryptedFieldHandler encryptedFieldHandler;
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

    public DataSignatureInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler) {
        this(encryptedFieldHandler, true, false);
    }

    public DataSignatureInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler, boolean signSwitch) {
        this(encryptedFieldHandler, signSwitch, false);
    }

    public DataSignatureInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler, boolean signSwitch, boolean signVerify) {
        this.encryptedFieldHandler = encryptedFieldHandler;
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
            this.doEntitySignature(param);
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
            this.doEntitySignature(parameterObject);
            return;
        }
        // 3、Map类型参数
        Map<?,?> paramMap = (Map<?,?>) parameterObject;
        Object param;
        // 4、通过MybatisPlus自带API（update、updateById等）修改数据库时
        if (paramMap.containsKey(Constants.ENTITY) && null != (param = paramMap.get(Constants.ENTITY))) {
            // 对参数进行签名处理
            doEntitySignature(param);
            return;
        }
        // 5、通过在mapper.xml中自定义API修改数据库时
        if (paramMap.containsKey(EnhanceConstants.CUSTOM_ENTITY) && null != (param = paramMap.get(EnhanceConstants.CUSTOM_ENTITY))) {
            // 对参数进行签名处理
            doEntitySignature(param);
            return;
        }
        // 6、通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
        if (paramMap.containsKey(Constants.WRAPPER) && null != (param = paramMap.get(Constants.WRAPPER))) {
            // 6.1、判断是否是UpdateWrapper、LambdaUpdateWrapper类型
            if (param instanceof Update && param instanceof AbstractWrapper) {
                Class<?> entityClass = mappedStatement.getParameterMap().getType();
                doWrapperSignature(entityClass, (AbstractWrapper<?,?,?>) param);
            }
        }
    }

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param parameter 参数
     */
    private void doEntitySignature(Object parameter) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断是否开启数据签名
        if(!signSwitch){
            return;
        }

        // 3、判断自定义Entity类是否被@TableSignature所注解
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, parameter.getClass());
        if(Objects.isNull(tableSignature)){
            return;
        }

        // 4、获取自定义Entity类联合签名的字段信息列表（排序后）
        List<TableFieldInfo> signatureFieldInfos = EncryptedFieldHelper.getSortedSignatureFieldInfos(parameter.getClass());
        if (CollectionUtils.isEmpty(signatureFieldInfos)) {
            return;
        }

        // 5、遍历字段，对字段进行签名处理
        StringJoiner hmacJoiner = new StringJoiner(Constants.PIPE);
        for (TableFieldInfo fieldInfo : signatureFieldInfos) {
            // 5.1、获取字段上的@TableSignatureField注解
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            // 5.2、如果Entity类被@TableSignature注解，并且 unionAll = true；或者字段被@TableSignatureField注解，并且 stored = false，则进行签名处理
            if (tableSignature.unionAll() || (Objects.nonNull(signatureField) && !signatureField.stored())) {
                // 5.2.1、获取签名字段的原始值
                Object fieldValue = ReflectUtil.getFieldValue(parameter, fieldInfo.getField());
                // 5.2.2、如果签名字段需要进行HMAC签名，则将原始值加入到HMAC签名列表中
                hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
            }
        }

        // 6、如果实体类需要进行单表数据存储完整性验证，则对数据表进行签名处理
        if (hmacJoiner.length() > 0){
            // 6.1、对数据进行签名处理
            String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
            // 6.2、获取存储的签名结果的字段
            Optional<TableFieldInfo> signatureStoreFieldInfo = EncryptedFieldHelper.getTableSignatureStoreFieldInfo(parameter.getClass());
            // 6.3、如果存储的签名结果的字段存在，则将签名值通过反射设置到字段上
            signatureStoreFieldInfo.ifPresent(fieldInfo -> ReflectUtil.setFieldValue(parameter, fieldInfo.getField(), hmacValue));
        }
    }

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     */
    private void doWrapperSignature(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断是否开启数据签名
        if(!signSwitch){
            return;
        }

        // 3、判断自定义Entity的类是否被@EncryptedTable所注解
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, entityClass);
        if(Objects.isNull(tableSignature)){
            return;
        }

        // 4、获取自定义Entity类联合签名的字段信息列表（排序后）
        List<TableFieldInfo> signatureFieldInfos = EncryptedFieldHelper.getSortedSignatureFieldInfos(entityClass);
        if (CollectionUtils.isEmpty(signatureFieldInfos)) {
            return;
        }

        // 5、获取 SQL 更新字段内容，例如：name='1', age=2
        String sqlSet = updateWrapper.getSqlSet();
        // 5.1、解析SQL更新字段内容，例如：name='1', age=2，解析为Map 对象
        String[] sqlSetArr = StringUtils.split(sqlSet, Constants.COMMA);
        Map<String, String> propMap = Arrays.stream(sqlSetArr).map(el -> el.split(Constants.EQUALS)).collect(Collectors.toMap(el -> el[0], el -> el[1]));

        // 6、遍历字段，对字段进行签名和签名处理
        StringJoiner hmacJoiner = new StringJoiner(Constants.PIPE);
        for (TableFieldInfo fieldInfo : signatureFieldInfos) {
            // 6.1、获取字段上的@TableSignatureField注解
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            // 6.2、如果Entity类被@TableSignature注解，并且 unionAll = true；或者字段被@TableSignatureField注解，并且 stored = false，则进行签名处理
            if (tableSignature.unionAll() || (Objects.nonNull(signatureField) && !signatureField.stored())) {
                // 6.2、获取字段的原始值
                String el = MapUtil.getStr(propMap, fieldInfo.getProperty());
                // 6.3、进行参数正则匹配，如果匹配成功，则对参数进行签名处理
                Matcher matcher = PARAM_PAIRS_RE.matcher(el);
                if (matcher.matches()) {
                    // 6.3.1、获取参数变量名
                    String valueKey = matcher.group(1);
                    // 6.3.2、获取参数变量值
                    Object fieldValue = updateWrapper.getParamNameValuePairs().get(valueKey);
                    // 6.3.3、如果签名字段需要进行HMAC签名，则将原始值加入到HMAC签名列表中
                    hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
                }
            }
        }

        // 7、如果数据表需要进行单表数据存储完整性验证，则对数据表进行HMAC签名处理
        if (hmacJoiner.length() > 0){
            // 7.1、获取存储的签名结果的字段
            Optional<TableFieldInfo> signatureStoreFieldInfo = EncryptedFieldHelper.getTableSignatureStoreFieldInfo(entityClass);
            // 7.2、如果数据表的HMAC字段存在，则将HMAC签名值通过反射设置到HMAC字段上
            if(signatureStoreFieldInfo.isPresent()){
                // 7.2.1、获取字段的原始值
                String el = MapUtil.getStr(propMap, signatureStoreFieldInfo.get().getProperty());
                // 7.2.2、进行参数正则匹配，如果匹配成功，则对参数进行签名处理
                Matcher matcher = PARAM_PAIRS_RE.matcher(el);
                if (matcher.matches()) {
                    // 7.2.2.1、对HMAC签名列表进行HMAC签名处理
                    String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
                    // 7.2.2.2、获取参数变量名
                    String valueKey = matcher.group(1);
                    // 7.2.2.4、替换参数变量值为签名后的值
                    updateWrapper.getParamNameValuePairs().put(valueKey, hmacValue);
                }
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
            // 逐一解密、验签
            handleResultSets(object);
        }
    }

    /**
     * 对单个对象进行解密
     * @param rowObject 单个对象
     * @param <T> 对象类型
     */
    private <T> void handleResultSets(T rowObject) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 3、判断自定义Entity的类是否被@EncryptedTable所注解
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, rowObject.getClass());
        if(Objects.isNull(tableSignature)){
            return;
        }

        // 4、获取自定义Entity类联合签名的字段信息列表（排序后）
        List<TableFieldInfo> signatureFieldInfos = EncryptedFieldHelper.getSortedSignatureFieldInfos(rowObject.getClass());
        if (CollectionUtils.isEmpty(signatureFieldInfos)) {
            return;
        }

        // 5、遍历字段，对字段进行签名处理
        StringJoiner hmacJoiner = new StringJoiner(Constants.PIPE);
        for (TableFieldInfo fieldInfo : signatureFieldInfos) {
            // 5.1、获取字段上的@TableSignatureField注解
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            // 5.2、如果Entity类被@TableSignature注解，并且 unionAll = true；或者字段被@TableSignatureField注解，并且 stored = false，则进行验签处理
            if (tableSignature.unionAll() || (Objects.nonNull(signatureField) && !signatureField.stored())) {
                // 5.2.1、获取签名字段的原始值
                Object fieldValue = ReflectUtil.getFieldValue(rowObject, fieldInfo.getField());
                // 5.2.2、将原始值加入到联合签名字符串中
                hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
            }
        }

        // 6、如果实体类需要进行单表数据存储完整性验证，则对数据表进行签名处理
        if (hmacJoiner.length() > 0){
            // 6.1、获取存储签名结果的字段
            Optional<TableFieldInfo> signatureStoreFieldInfo = EncryptedFieldHelper.getTableSignatureStoreFieldInfo(rowObject.getClass());
            // 6.2、如果存储签名结果的字段存在，则进行签名验证
            if(signatureStoreFieldInfo.isPresent()){
                // 6.2.1、获取存储签名结果的字段的值
                Object hmacFieldValue = ReflectUtil.getFieldValue(rowObject, signatureStoreFieldInfo.get().getProperty());
                // 6.2.2、获取表名，用于异常提示
                TableName tableName = AnnotationUtils.findFirstAnnotation(TableName.class, rowObject.getClass());
                // 6.2.3、对联合签名字符串进行签名处理，获取签名值
                String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
                // 6.2.4、对比签名值，如果不一致，则抛出异常
                ExceptionUtils.throwMpe(!Objects.equals(hmacValue, hmacFieldValue),
                        "表【%s】的数据列【%s】,数据存储完整性验证不通过，数据被篡改，请检查数据完整性",
                        tableName.value(),
                        signatureFieldInfos.stream().map(TableFieldInfo::getColumn).reduce((a, b) -> a + Constants.COMMA + b).orElse(Constants.EMPTY));
            }
        }

    }

}
