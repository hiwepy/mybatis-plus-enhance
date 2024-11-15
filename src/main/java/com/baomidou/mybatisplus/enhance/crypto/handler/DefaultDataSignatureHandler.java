package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignature;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignatureField;
import com.baomidou.mybatisplus.enhance.util.EncryptedFieldHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DefaultDataSignatureHandler implements DataSignatureHandler {

    /**
     * 变量占位符正则
     */
    public static final Pattern PARAM_PAIRS_RE = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(" + Constants.WRAPPER_PARAM + "\\d+)\\}");
    /**
     * 加解密处理器，加解密的情况都在该处理器中自行判断
     */
    @Getter
    private final EncryptedFieldHandler encryptedFieldHandler;

    @Getter
    @Setter
    private DataSignatureReadWriteProvider signatureReadWriteProvider = new DefaultDataSignatureReadWriteProvider();

    public DefaultDataSignatureHandler(EncryptedFieldHandler encryptedFieldHandler) {
        this.encryptedFieldHandler = encryptedFieldHandler;
    }

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param parameter 参数
     */
    @Override
    public void doEntitySignature(Object parameter) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

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
            // 6.2、调用签名读写提供者，将签名值写入到实体类中或外部存储
            getSignatureReadWriteProvider().writeSignature(parameter, parameter.getClass(), null, hmacValue);
        }
    }

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     */

    @Override
    public void doWrapperSignature(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, entityClass);
        if(Objects.isNull(tableSignature)){
            return;
        }

        // 3、获取自定义Entity类联合签名的字段信息列表（排序后）
        List<TableFieldInfo> signatureFieldInfos = EncryptedFieldHelper.getSortedSignatureFieldInfos(entityClass);
        if (CollectionUtils.isEmpty(signatureFieldInfos)) {
            return;
        }

        // 4、获取 SQL 更新字段内容，例如：name='1', age=2
        String sqlSet = updateWrapper.getSqlSet();
        // 4.1、解析SQL更新字段内容，例如：name='1', age=2，解析为Map 对象
        String[] sqlSetArr = StringUtils.split(sqlSet, Constants.COMMA);
        Map<String, String> propMap = Arrays.stream(sqlSetArr).map(el -> el.split(Constants.EQUALS)).collect(Collectors.toMap(el -> el[0], el -> el[1]));

        // 5、遍历字段，对字段进行签名和签名处理
        StringJoiner hmacJoiner = new StringJoiner(Constants.PIPE);
        for (TableFieldInfo fieldInfo : signatureFieldInfos) {
            // 5.1、获取字段上的@TableSignatureField注解
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            // 5.2、如果Entity类被@TableSignature注解，并且 unionAll = true；或者字段被@TableSignatureField注解，并且 stored = false，则进行签名处理
            if (tableSignature.unionAll() || (Objects.nonNull(signatureField) && !signatureField.stored())) {
                // 5.2.1、获取字段的原始值
                String el = MapUtil.getStr(propMap, fieldInfo.getProperty());
                // 5.2.2、进行参数正则匹配，如果匹配成功，则对参数进行签名处理
                Matcher matcher = PARAM_PAIRS_RE.matcher(el);
                if (matcher.matches()) {
                    // 5.2.2.1、获取参数变量名
                    String valueKey = matcher.group(1);
                    // 5.2.2.2、获取参数变量值
                    Object fieldValue = updateWrapper.getParamNameValuePairs().get(valueKey);
                    // 5.2.2.3、如果签名字段需要进行HMAC签名，则将原始值加入到HMAC签名列表中
                    hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
                }
            }
        }

        // 6、如果实体类需要进行单表数据存储完整性验证，则对数据表进行签名处理
        if (hmacJoiner.length() > 0){
            // 6.1、对数据进行签名处理
            String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
            // 6.2、调用签名读写提供者，将签名值写入到实体类中或外部存储
            getSignatureReadWriteProvider().writeSignature(propMap, entityClass, updateWrapper, hmacValue);
        }
    }

    /**
     * 对单个对象进行解密
     * @param rawObject 单个对象或Map
     * @param entityClass 实体类
     * @param <T> 对象类型
     */
    @Override
    public <T> void doSignatureVerification(Object rawObject, Class<T> entityClass) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

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

        // 5、遍历字段，对字段进行签名处理
        StringJoiner hmacJoiner = new StringJoiner(Constants.PIPE);
        for (TableFieldInfo fieldInfo : signatureFieldInfos) {
            // 5.1、获取字段上的@TableSignatureField注解
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            // 5.2、如果Entity类被@TableSignature注解，并且 unionAll = true；或者字段被@TableSignatureField注解，并且 stored = false，则进行验签处理
            if (tableSignature.unionAll() || (Objects.nonNull(signatureField) && !signatureField.stored())) {
                // 5.2.1、获取签名字段的原始值
                Object fieldValue;
                if(rawObject instanceof Map){
                    Map<?,?> rawMap = (Map<?,?>) rawObject;
                    fieldValue = MapUtil.getStr(rawMap, fieldInfo.getProperty());
                } else {
                    fieldValue = ReflectUtil.getFieldValue(rawObject, fieldInfo.getField());
                }
                // 5.2.2、将原始值加入到联合签名字符串中
                hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
            }
        }

        // 6、对联合签名字符串进行签名处理，获取签名值，并进行签名验证
        this.doSignatureVerification(signatureFieldInfos, hmacJoiner, rawObject, entityClass);

    }

   protected <T> void doSignatureVerification(List<TableFieldInfo> signatureFieldInfos, StringJoiner hmacJoiner, Object rawObject, Class<T> entityClass){
        // 1、如果实体类需要进行单表数据存储完整性验证，则对数据表进行签名处理
        if (Objects.isNull(rawObject) || Objects.isNull(entityClass) || CollectionUtils.isEmpty(signatureFieldInfos) || Objects.isNull(hmacJoiner) || hmacJoiner.length() == 0) {
            return;
        }
        // 6.1、获取之前存储的签名
        Optional<Object> signatureValue = getSignatureReadWriteProvider().readSignature(rawObject, entityClass);
        // 6.2.2、获取表名，用于异常提示
        TableName tableName = AnnotationUtils.findFirstAnnotation(TableName.class, entityClass);
        // 6.2、如果签名结果存在，则进行签名验证
        if(signatureValue.isPresent()){
            // 6.2.3、对联合签名字符串进行签名处理，获取签名值
            String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
            // 6.2.4、对比签名值，如果不一致，则抛出异常
            ExceptionUtils.throwMpe(!Objects.equals(hmacValue, signatureValue.get()),
                    "表【%s】的数据列【%s】,数据签名不匹配，数据存储完整性验证不通过，请检查数据完整性",
                    tableName.value(),
                    signatureFieldInfos.stream().map(TableFieldInfo::getColumn).reduce((a, b) -> a + Constants.COMMA + b).orElse(Constants.EMPTY));
        } else {
            // 6.3、如果签名结果不存在，则抛出异常
            throw ExceptionUtils.mpe("表【%s】的数据列【%s】,原来签名不存在，数据存储完整性验证不通过，请先进行数据签名",
                    tableName.value(),
                    signatureFieldInfos.stream().map(TableFieldInfo::getColumn).reduce((a, b) -> a + Constants.COMMA + b).orElse(Constants.EMPTY));
        }
    }

}
