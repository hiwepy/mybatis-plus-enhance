package com.baomidou.mybatisplus.enhance.interceptor.inner;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedField;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedTable;
import com.baomidou.mybatisplus.enhance.crypto.annotation.IgnoreEncrypted;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.util.ParameterUtils;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * 数据解密拦截器，用于对查询结果进行解密操作
 */
@Slf4j
public class DataDecryptionInnerInterceptor implements EnhanceInnerInterceptor {

    /**
     * 加解密处理器，加解密的情况都在该处理器中自行判断
     */
    @Getter
    private final EncryptedFieldHandler encryptedFieldHandler;
    @Getter
    private final boolean decryptSwitch;

    public DataDecryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler) {
        this(false, encryptedFieldHandler);
    }

    public DataDecryptionInnerInterceptor(boolean decryptSwitch, EncryptedFieldHandler encryptedFieldHandler) {
        super();
        this.decryptSwitch = decryptSwitch;
        this.encryptedFieldHandler = encryptedFieldHandler;
    }

    @Override
    public void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(decryptSwitch, rtList)) {
            return;
        }
        // 2、检查Mapper接口类和方法名
        try {
            String mappedStatementId = ms.getId();
            Class<?> mapperClazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(".")));
            String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
            Method method = ReflectUtil.getMethodByName(mapperClazz, methodName);
            if (Objects.nonNull(method)) {
                // 获取 @EncryptedTable 注解
                IgnoreEncrypted ignoreEncrypted = AnnotationUtils.findFirstAnnotation(IgnoreEncrypted.class, method);
                if (ObjectUtils.isNotEmpty(ignoreEncrypted)) {
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("DataDecryptionInnerInterceptor.afterQuery ClassNotFoundException", e);
        }
        // 3、对查询结果进行解密
        for (Object object : rtList) {
            // 逐一解密、验签
            handleResultSets(object);
        }
    }

    /**
     * 对单个对象进行解密
     * @param rtObject 单个对象
     * @param <T> 对象类型
     */
    private <T> void handleResultSets(T rtObject) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, rtObject.getClass());
        if(Objects.isNull(encryptedTable)){
            return;
        }

        // 3、获取该类的所有标记为加密字段的属性列表
        List<TableFieldInfo> encryptedFieldInfos = TableFieldHelper.getEncryptedFieldInfos(rtObject.getClass());
        if (CollectionUtils.isEmpty(encryptedFieldInfos)) {
            return;
        }

        // 4、遍历字段，对字段进行解密处理
        for (TableFieldInfo fieldInfo : encryptedFieldInfos) {

            // 4.1、获取字段上的@EncryptedField注解，如果没有则跳过
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            if (Objects.isNull(encryptedField)) {
                continue;
            }

            // 4.2、获取加密字段的原始值
            Object fieldValue = ReflectUtil.getFieldValue(rtObject, fieldInfo.getField());
            // 4.3、如果原始值不为空，则对原始值进行解密处理
            if (Objects.nonNull(fieldValue)) {
                // 4.3.1、对原始值进行解密处理
                fieldValue = getEncryptedFieldHandler().decrypt(Objects.toString(fieldValue), fieldValue.getClass());
                // 4.3.2、将解密后的值通过反射设置到字段上
                ReflectUtil.setFieldValue(rtObject, fieldInfo.getField(), fieldValue);
            }
        }

    }

}
