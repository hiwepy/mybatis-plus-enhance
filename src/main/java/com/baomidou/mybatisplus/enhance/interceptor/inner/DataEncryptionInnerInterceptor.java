package com.baomidou.mybatisplus.enhance.interceptor.inner;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedField;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedTable;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.SimpleTypeRegistry;
import util.EncryptedFieldHelper;
import util.EnhanceConstants;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据加解密和签名拦截器，用于对新增/更新数据进行加密和签名操作
 */
@Slf4j
public class DataEncryptionInnerInterceptor extends JsqlParserSupport implements InnerInterceptor {

    /**
     * 变量占位符正则
     */
    private static final Pattern PARAM_PAIRS_RE = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(" + Constants.WRAPPER_PARAM + "\\d+)\\}");


    /**
     * 加解密处理器，加解密的情况都在该处理器中自行判断
     */
    @Getter
    private final EncryptedFieldHandler encryptedFieldHandler;

    public DataEncryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler) {
        this.encryptedFieldHandler = encryptedFieldHandler;
    }

    /**
     * 如果查询条件是加密数据列，那么要将查询条件进行数据加密。
     * 例如，手机号加密存储后，按手机号查询时，先把要查询的手机号进行加密，再和数据库存储的加密数据进行匹配
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，则直接返回
        if (Objects.isNull(parameterObject) || SimpleTypeRegistry.isSimpleType(parameterObject.getClass())) {
            return;
        }
        // 2、如果参数
        if (!(parameterObject instanceof Map)) {
            return;
        }
        // 3、Map类型参数
        Map<?,?> paramMap = (Map<?,?>) parameterObject;
        // 4、参数去重，否则多次加密会导致查询失败
        Set<?> set = new HashSet<>(paramMap.values());
        // 5、遍历参数，进行加密处理
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
                // Wrapper、String类型查询参数，无法获取参数变量上的注解，无法确认是否需要加密，因此不做判断
                continue;
            }
            // 对参数进行加密处理
            this.doEntityEncrypt(param);
        }
    }

    /**
     * 新增、更新数据时，如果包含隐私数据，则进行加密
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement mappedStatement, Object parameterObject) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，则直接返回
        if (Objects.isNull(parameterObject) || SimpleTypeRegistry.isSimpleType(parameterObject.getClass())) {
            return;
        }
        // 2、通过MybatisPlus自带API（save、insert等）新增数据库时
        if (!(parameterObject instanceof Map)) {
            // 对参数进行加密处理
            this.doEntityEncrypt(parameterObject);
            return;
        }
        // 3、Map类型参数
        Map<?,?> paramMap = (Map<?,?>) parameterObject;
        Object param;
        // 4、通过MybatisPlus自带API（update、updateById等）修改数据库时
        if (paramMap.containsKey(Constants.ENTITY) && null != (param = paramMap.get(Constants.ENTITY))) {
            // 对参数进行加密处理
            doEntityEncrypt(param);
            return;
        }
        // 5、通过在mapper.xml中自定义API修改数据库时
        if (paramMap.containsKey(EnhanceConstants.CUSTOM_ENTITY) && null != (param = paramMap.get(EnhanceConstants.CUSTOM_ENTITY))) {
            // 对参数进行加密处理
            doEntityEncrypt(param);
            return;
        }
        // 6、通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
        if (paramMap.containsKey(Constants.WRAPPER) && null != (param = paramMap.get(Constants.WRAPPER))) {
            // 6.1、判断是否是UpdateWrapper、LambdaUpdateWrapper类型
            if (param instanceof Update && param instanceof AbstractWrapper) {
                Class<?> entityClass = mappedStatement.getParameterMap().getType();
                doWrapperEncrypt(entityClass, (AbstractWrapper<?,?,?>) param);
            }
        }
    }

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param parameter 参数
     */
    private void doEntityEncrypt(Object parameter) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, parameter.getClass());
        if(Objects.isNull(encryptedTable)){
            return;
        }

        // 3、获取该类的所有标记为加密字段的属性列表
        List<TableFieldInfo> encryptedFieldInfos = EncryptedFieldHelper.getSortedEncryptedFieldInfos(parameter.getClass());
        if (CollectionUtils.isEmpty(encryptedFieldInfos)) {
            return;
        }

        // 4、遍历字段，对字段进行加密和签名处理
        StringJoiner hmacJoiner = encryptedTable.hmac() ? new StringJoiner(Constants.PIPE) : null;
        for (TableFieldInfo fieldInfo : encryptedFieldInfos) {
            // 4.1、获取加密字段上的@EncryptedField注解
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            if (Objects.isNull(encryptedField)) {
                continue;
            }
            // 4.2、获取加密字段的原始值
            Object oldValue = ReflectUtil.getFieldValue(parameter, fieldInfo.getField());
            // 4.3、如果加密字段需要进行HMAC加密，则将原始值加入到HMAC加密列表中
            if (encryptedTable.hmac() && Objects.nonNull(hmacJoiner) && encryptedField.hmac()) {
                hmacJoiner.add(Objects.toString(oldValue, Constants.EMPTY));
            }
            // 4.4、如果原始值不为空，则对原始值进行加密处理
            if (Objects.nonNull(oldValue)) {
                // 4.4.1、对原始值进行加密处理
                String newValue = getEncryptedFieldHandler().encrypt(oldValue);
                // 4.4.2、将加密后的值通过反射设置到字段上
                ReflectUtil.setFieldValue(parameter, fieldInfo.getField(), newValue);
            }
        }
        // 5、如果数据表需要进行单表数据存储完整性验证，则对数据表进行HMAC签名处理
        if (encryptedTable.hmac() && Objects.nonNull(hmacJoiner)){
            // 5.1、对HMAC加密列表进行HMAC签名处理
            String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
            // 5.2、获取数据表的HMAC字段
            Optional<TableFieldInfo> hmacFieldInfo = EncryptedFieldHelper.getTableHmacFieldInfo(parameter.getClass());
            // 5.3、如果数据表的HMAC字段存在，则将HMAC签名值通过反射设置到HMAC字段上
            hmacFieldInfo.ifPresent(fieldInfo -> ReflectUtil.setFieldValue(parameter, fieldInfo.getField(), hmacValue));
        }
    }

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     */
    private void doWrapperEncrypt(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, entityClass);
        if(Objects.isNull(encryptedTable)){
            return;
        }

        // 3、获取该类的所有标记为加密字段的属性列表
        List<TableFieldInfo> encryptedFieldInfos = EncryptedFieldHelper.getSortedEncryptedFieldInfos(entityClass);
        if (CollectionUtils.isEmpty(encryptedFieldInfos)) {
            return;
        }

        // 4、获取 SQL 更新字段内容，例如：name='1', age=2
        String sqlSet = updateWrapper.getSqlSet();
        // 4.1、解析SQL更新字段内容，例如：name='1', age=2，解析为Map 对象
        String[] sqlSetArr = StringUtils.split(sqlSet, Constants.COMMA);
        Map<String, String> propMap = Arrays.stream(sqlSetArr).map(el -> el.split(Constants.EQUALS)).collect(Collectors.toMap(el -> el[0], el -> el[1]));

        // 5、遍历字段，对字段进行加密和签名处理
        StringJoiner hmacJoiner = encryptedTable.hmac() ? new StringJoiner(Constants.PIPE) : null;
        for (TableFieldInfo fieldInfo : encryptedFieldInfos) {
            // 5.1、获取字段上的@EncryptedField注解
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            if (Objects.isNull(encryptedField)) {
                continue;
            }
            // 5.2、获取字段的原始值
            String el = MapUtil.getStr(propMap, fieldInfo.getProperty());
            // 5.3、进行参数正则匹配，如果匹配成功，则对参数进行加密处理
            Matcher matcher = PARAM_PAIRS_RE.matcher(el);
            if (matcher.matches()) {
                // 5.3.1、获取参数变量名
                String valueKey = matcher.group(1);
                // 5.3.2、获取参数变量值
                Object value = updateWrapper.getParamNameValuePairs().get(valueKey);
                // 5.3.3、如果加密字段需要进行HMAC加密，则将原始值加入到HMAC加密列表中
                if (encryptedTable.hmac() && Objects.nonNull(hmacJoiner) && encryptedField.hmac()) {
                    hmacJoiner.add(Objects.toString(value, Constants.EMPTY));
                }
                // 5.3.4、如果参数变量值不为空，则对参数变量值进行加密处理
                if (Objects.nonNull(value)) {
                    // 5.3.4.1、对原始值进行加密处理
                    String newValue = getEncryptedFieldHandler().encrypt(value);
                    // 5.3.4.2、替换参数变量值为加密后的值
                    updateWrapper.getParamNameValuePairs().put(valueKey, newValue);
                }
            }
        }

        // 5、如果数据表需要进行单表数据存储完整性验证，则对数据表进行HMAC签名处理
        if (encryptedTable.hmac() && Objects.nonNull(hmacJoiner)){
            // 5.2、获取数据表的HMAC字段
            Optional<TableFieldInfo> hmacFieldInfo = EncryptedFieldHelper.getTableHmacFieldInfo(entityClass);
            // 5.3、如果数据表的HMAC字段存在，则将HMAC签名值通过反射设置到HMAC字段上
            if(hmacFieldInfo.isPresent()){
                // 5.2、获取字段的原始值
                String el = MapUtil.getStr(propMap, hmacFieldInfo.get().getProperty());
                // 5.3、进行参数正则匹配，如果匹配成功，则对参数进行加密处理
                Matcher matcher = PARAM_PAIRS_RE.matcher(el);
                if (matcher.matches()) {
                    // 5.1、对HMAC加密列表进行HMAC签名处理
                    String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
                    // 5.3.1、获取参数变量名
                    String valueKey = matcher.group(1);
                    // 5.3.3.3、替换参数变量值为加密后的值
                    updateWrapper.getParamNameValuePairs().put(valueKey, hmacValue);
                }
            };
        }
    }

}
