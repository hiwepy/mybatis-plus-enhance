package com.baomidou.mybatisplus.enhance.sensitive.handler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureReadWriteProvider;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataSignatureReadWriteProvider;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveJSONField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveJSONFieldKey;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DefaultDataMaskingHandler implements DataMaskingHandler {

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
    private final DataSignatureReadWriteProvider signatureReadWriteProvider;

    public DefaultDataMaskingHandler(EncryptedFieldHandler encryptedFieldHandler) {
        this(encryptedFieldHandler, new DefaultDataSignatureReadWriteProvider());
    }

    public DefaultDataMaskingHandler(EncryptedFieldHandler encryptedFieldHandler, DataSignatureReadWriteProvider signatureReadWriteProvider) {
        this.encryptedFieldHandler = encryptedFieldHandler;
        this.signatureReadWriteProvider = signatureReadWriteProvider;
    }

    /**
     * 通过API（save、updateById等）修改数据库时
     * @param parameterObject 参数
     */
    @Override
    public  <T> void doQueryMasking(T parameterObject) {
        // 1、获取自定义Entity类的TableInfo
        TableInfo tableInfo = TableInfoHelper.getTableInfo(parameterObject.getClass());
        if (Objects.isNull(tableInfo)) {
            return;
        }
        // 2、遍历字段，对字段进行脱敏处理
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            // 2.1、非CharSequence类型字段不进行脱敏处理
            if(!fieldInfo.getPropertyType().isAssignableFrom(CharSequence.class)){
                continue;
            }
            // 2.2、获取字段上的@SensitiveField注解
            SensitiveField sensitiveField = AnnotationUtils.findFirstAnnotation(SensitiveField.class, fieldInfo.getField());
            // 2.3、获取字段上的@sensitiveJSONField注解
            SensitiveJSONField sensitiveJSONField = AnnotationUtils.findFirstAnnotation(SensitiveJSONField.class, fieldInfo.getField());
            // 2.4、如果字段上没有@SensitiveField注解，也没有@sensitiveJSONField注解，则不进行脱敏处理
            if (Objects.isNull(sensitiveField) && Objects.isNull(sensitiveJSONField)) {
                continue;
            }
            // 2.5、获取字段的原始值
            Object fieldValue = ReflectUtil.getFieldValue(parameterObject, fieldInfo.getField());
            Object markingValue = null;
            if (Objects.nonNull(sensitiveField) && sensitiveField.maskingWhenSet() && Objects.nonNull(fieldValue)) {
                markingValue = SensitiveTypeRegisty.get(sensitiveField.value()).handle(fieldValue);
            }
            else if (Objects.nonNull(sensitiveJSONField) && sensitiveJSONField.maskingWhenSet()  && Objects.nonNull(fieldValue)) {
                markingValue = processJsonField(fieldValue, sensitiveJSONField);
            }
            // 2.6、将脱敏结果写入对象中
            if(Objects.nonNull(markingValue)){
                ReflectUtil.setFieldValue(parameterObject, fieldInfo.getField(), markingValue);
            }
        }
    }

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     */

    @Override
    public void doQueryMasking(Class<?> entityClass, AbstractWrapper<?,?,?> updateWrapper) {

        // 1、获取自定义Entity类的TableInfo
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (Objects.isNull(tableInfo)) {
            return;
        }

        // 2、获取 SQL 更新字段内容，例如：name='1', age=2
        String sqlSet = updateWrapper.getSqlSet();
        // 2.1、解析SQL更新字段内容，例如：name='1', age=2，解析为Map 对象
        String[] sqlSetArr = StringUtils.split(sqlSet, Constants.COMMA);
        Map<String, String> propMap = Arrays.stream(sqlSetArr).map(el -> el.split(Constants.EQUALS)).collect(Collectors.toMap(el -> el[0], el -> el[1]));

        // 3、遍历字段，对字段进行脱敏处理
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            // 2.2、获取字段上的@SensitiveField注解
            SensitiveField sensitiveField = AnnotationUtils.findFirstAnnotation(SensitiveField.class, fieldInfo.getField());
            // 2.3、获取字段上的@sensitiveJSONField注解
            SensitiveJSONField sensitiveJSONField = AnnotationUtils.findFirstAnnotation(SensitiveJSONField.class, fieldInfo.getField());
            // 2.4、如果字段上没有@SensitiveField注解，也没有@sensitiveJSONField注解，则不进行脱敏处理
            if (Objects.isNull(sensitiveField) && Objects.isNull(sensitiveJSONField)) {
                continue;
            }
            // 2.5、如果字段上有@SensitiveField 或 @SensitiveJSONField注解，且 maskingWhenSet = true，则进行脱敏处理
            if ((Objects.nonNull(sensitiveField) && sensitiveField.maskingWhenSet()) || (Objects.nonNull(sensitiveJSONField) && sensitiveJSONField.maskingWhenSet())) {
                // 2.5.1、获取字段的原始值
                String el = MapUtil.getStr(propMap, fieldInfo.getProperty());
                // 2.5.2、进行参数正则匹配，如果匹配成功，则对参数进行签名处理
                Matcher matcher = PARAM_PAIRS_RE.matcher(el);
                if (matcher.matches()) {
                    // 2.5.2.1、获取参数变量名
                    String valueKey = matcher.group(1);
                    // 2.5.2.2、获取参数变量值
                    Object fieldValue = updateWrapper.getParamNameValuePairs().get(valueKey);
                    // 2.5.2.3、对字段进行脱敏处理
                    Object markingValue = null;
                    if (Objects.nonNull(sensitiveField) && sensitiveField.maskingWhenSet() && Objects.nonNull(fieldValue)) {
                        markingValue = SensitiveTypeRegisty.get(sensitiveField.value()).handle(fieldValue);
                    }
                    else if (Objects.nonNull(sensitiveJSONField) && sensitiveJSONField.maskingWhenSet()  && Objects.nonNull(fieldValue)) {
                        markingValue = processJsonField(fieldValue, sensitiveJSONField);
                    }
                    // 2.5.2.4、替换参数变量值为脱敏后的值
                    if(Objects.nonNull(markingValue)){
                        updateWrapper.getParamNameValuePairs().put(valueKey, markingValue);
                    }
                }
            }
        }
    }

    @Override
    public <T> void doResultMasking(T rawObject) {
        // 1、获取自定义Entity类的TableInfo
        TableInfo tableInfo = TableInfoHelper.getTableInfo(rawObject.getClass());
        if (Objects.isNull(tableInfo)) {
            return;
        }
        // 2、遍历字段，对字段进行脱敏处理
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            // 2.1、非CharSequence类型字段不进行脱敏处理
            if(!fieldInfo.getPropertyType().isAssignableFrom(CharSequence.class)){
                continue;
            }
            // 2.2、获取字段上的@SensitiveField注解
            SensitiveField sensitiveField = AnnotationUtils.findFirstAnnotation(SensitiveField.class, fieldInfo.getField());
            // 2.3、获取字段上的@sensitiveJSONField注解
            SensitiveJSONField sensitiveJSONField = AnnotationUtils.findFirstAnnotation(SensitiveJSONField.class, fieldInfo.getField());
            // 2.4、如果字段上没有@SensitiveField注解，也没有@sensitiveJSONField注解，则不进行脱敏处理
            if (Objects.isNull(sensitiveField) && Objects.isNull(sensitiveJSONField)) {
                continue;
            }
            // 2.5、获取字段的原始值
            Object fieldValue = ReflectUtil.getFieldValue(rawObject, fieldInfo.getField());
            Object markingValue = null;
            if (Objects.nonNull(sensitiveField) && sensitiveField.maskingWhenGet() && Objects.nonNull(fieldValue)) {
                markingValue = SensitiveTypeRegisty.get(sensitiveField.value()).handle(fieldValue);
            }
            else if (Objects.nonNull(sensitiveJSONField) && sensitiveJSONField.maskingWhenGet()  && Objects.nonNull(fieldValue)) {
                markingValue = processJsonField(fieldValue, sensitiveJSONField);
            }
            // 2.6、将脱敏结果写入对象中
            if(Objects.nonNull(markingValue)){
                ReflectUtil.setFieldValue(rawObject, fieldInfo.getField(), markingValue);
            }
        }
    }

    /**
     * 在json中进行脱敏
     * @param newValue new
     * @param sensitiveJSONField 脱敏的字段
     * @return json
     */
    private Object processJsonField(Object newValue, SensitiveJSONField sensitiveJSONField) {

        try{
            Map<String,Object> map = JSONUtil.parseObj(newValue.toString());
            SensitiveJSONFieldKey[] keys = sensitiveJSONField.sensitivelist();
            for(SensitiveJSONFieldKey jsonFieldKey :keys){
                String key = jsonFieldKey.key();
                SensitiveType sensitiveType = jsonFieldKey.type();
                Object oldData = map.get(key);
                if(oldData!=null){
                    String newData = SensitiveTypeRegisty.get(sensitiveType).handle(oldData);
                    map.put(key,newData);
                }
            }
            return JSONUtil.toJsonStr(map);
        }catch (Throwable e){
            //失败以后返回默认值
            log.error("脱敏json串时失败，cause : {}",e.getMessage(),e);
            return newValue;
        }
    }


}
