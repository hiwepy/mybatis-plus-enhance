package com.baomidou.mybatisplus.enhance.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.handlers.AnnotationHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedField;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedTable;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignature;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignatureField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveJSONField;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class TableFieldHelper {

    /**
     * 校验该实例的类是否被 @EncryptedTable所注解
     */
    public static boolean isExistEncryptedTable(Object object) {
        Class<?> objectClass = object.getClass();
/*
        ReflectUtils.doWithFields(objectClass, field -> {
            EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, field);
            return Objects.nonNull(encryptedTable);
        });*/
        return isExistEncryptedTable(objectClass);
    }

    /**
     * 校验该实例的类是否被@EncryptedTable所注解
     */
    public static boolean isExistEncryptedTable(Class<?> objectClass) {
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, objectClass);
        return Objects.nonNull(encryptedTable);
    }

    /**
     * <p>
     * 判断主键注解是否存在
     * </p>
     *
     * @param list 字段列表
     * @return true 为存在 {@link TableId} 注解;
     */
    public static boolean isExistTableCryptoField(List<Field> list, AnnotationHandler annotationHandler) {
        return list.stream().anyMatch(field -> annotationHandler.isAnnotationPresent(field, EncryptedField.class));
    }

    /**
     * <p>
     * 获取该类的标记有 @SensitiveJSONField 注解的的字段信息列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSensitiveJSONFieldInfos(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return getSensitiveFieldInfos(tableInfo);
    }

    /**
     * <p>
     * 获取该类的标记有 @SensitiveJSONField 注解的的字段信息列表
     * </p>
     *
     * @param tableInfo 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSensitiveJSONFieldInfos(TableInfo tableInfo) {
        return tableInfo.getFieldList().stream().filter(fieldInfo -> {
            SensitiveJSONField encryptedField = AnnotationUtils.findFirstAnnotation(SensitiveJSONField.class, fieldInfo.getField());
            return Objects.nonNull(encryptedField);
        }).collect(Collectors.toList());
    }

    /**
     * <p>
     * 获取该类的标记有 @SensitiveField 注解的的字段信息列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSensitiveFieldInfos(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return getSensitiveFieldInfos(tableInfo);
    }

    /**
     * <p>
     * 获取该类的标记有 @SensitiveField 注解的的字段信息列表
     * </p>
     *
     * @param tableInfo 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSensitiveFieldInfos(TableInfo tableInfo) {
        return tableInfo.getFieldList().stream().filter(fieldInfo -> {
            /* 过滤注解非加密表字段属性 */
            SensitiveField encryptedField = AnnotationUtils.findFirstAnnotation(SensitiveField.class, fieldInfo.getField());
            return Objects.nonNull(encryptedField);
        }).collect(Collectors.toList());
    }

    /**
     * <p>
     * 获取该类的标记有 @EncryptedField 注解的的字段信息列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getEncryptedFieldInfos(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return getEncryptedFieldInfos(tableInfo);
    }

    /**
     * <p>
     * 获取该类的标记有 @EncryptedField 注解的的字段信息列表
     * </p>
     *
     * @param tableInfo 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getEncryptedFieldInfos(TableInfo tableInfo) {
        return tableInfo.getFieldList().stream().filter(fieldInfo -> {
            /* 过滤注解非加密表字段属性 */
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            return Objects.nonNull(encryptedField);
        }).collect(Collectors.toList());
    }

    /**
     * 获取自定义Entity类联合签名的字段信息列表（未排序）
     * 1、@TableSignature 注解且 unionAll = true 的实体类的所有字段
     * 2、@TableSignature 注解且 unionAll = false 的实体类的被有 @TableSignatureField 注解且 stored = false 的字段信息列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSignatureFieldInfos(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return getSignatureFieldInfos(tableInfo);
    }

    /**
     * 获取自定义Entity类联合签名的字段信息列表（未排序）
     * 1、@TableSignature 注解且 unionAll = true 的实体类的所有字段
     * 2、@TableSignature 注解且 unionAll = false 的实体类的被有 @TableSignatureField 注解且 stored = false 的字段信息列表
     * </p>
     *
     * @param tableInfo 表信息
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSignatureFieldInfos(TableInfo tableInfo) {
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, tableInfo.getEntityType());
        if (Objects.nonNull(tableSignature) && tableSignature.unionAll()) {
            // 如果是联合签名，则返回除存储签名结果字段外的所有其他字段
            return tableInfo.getFieldList().stream().filter(fieldInfo -> {
                TableSignatureField tableSignatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
                if(Objects.isNull(tableSignatureField) ){
                    return true;
                }
                return !tableSignatureField.stored();
            }).collect(Collectors.toList());
        }
        // 如果不是联合签名，则返回所有标记有 @TableSignatureField 注解且 stored = false 的字段
        return tableInfo.getFieldList().stream().filter(fieldInfo -> {
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            return Objects.nonNull(signatureField) && !signatureField.stored();
        }).collect(Collectors.toList());
    }

    /**
     * <p>
     * 获取自定义Entity类联合签名的字段信息列表（排序后）
     * 1、@TableSignature 注解且 unionAll = true 的实体类的所有字段
     * 2、@TableSignature 注解且 unionAll = false 的实体类的被有 @TableSignatureField 注解且 stored = false 的字段信息列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSortedSignatureFieldInfos(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return getSortedSignatureFieldInfos(tableInfo);
    }

    /**
     * <p>
     * 获取自定义Entity类联合签名的字段信息列表（排序后）
     * 1、@TableSignature 注解且 unionAll = true 的实体类的所有字段
     * 2、@TableSignature 注解且 unionAll = false 的实体类的被有 @TableSignatureField 注解且 stored = false 的字段信息列表
     * </p>
     *
     * @param tableInfo 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSortedSignatureFieldInfos(TableInfo tableInfo) {
        return getSignatureFieldInfos(tableInfo).stream().sorted(Comparator.comparing(info -> {
            TableFieldInfo fieldInfo = (TableFieldInfo) info;
            TableSignatureField ef1 = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            if (Objects.isNull(ef1)) {
                return 0;
            }
            return ef1.order();
        }).thenComparing(info -> {
            TableFieldInfo fieldInfo = (TableFieldInfo) info;
            return fieldInfo.getColumn();
        })).collect(Collectors.toList());
    }

    /**
     * <p>
     * 获取该类的标记有 @TableSignatureField 注解且 stored = false 的第一个字段信息
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static Optional<TableFieldInfo> getTableSignatureStoreFieldInfo(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return getTableSignatureStoreFieldInfo(tableInfo);
    }

    /**
     * <p>
     * 获取该类的标记有 @TableSignatureField 注解且 stored = false 的第一个字段信息
     * </p>
     *
     * @param tableInfo 反射类
     * @return 属性集合
     */
    public static Optional<TableFieldInfo> getTableSignatureStoreFieldInfo(TableInfo tableInfo) {
        return tableInfo.getFieldList().stream().filter(fieldInfo -> {
            /* 过滤注解 @TableSignatureField 字段属性 */
            TableSignatureField tableSignatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            return Objects.nonNull(tableSignatureField) && tableSignatureField.stored();
        }).findFirst();
    }

    public static Serializable getKeyValue(Object rawObject) {
        return getKeyValue(rawObject, TableInfoHelper.getTableInfo(rawObject.getClass()));
    }

    public static Serializable getKeyValue(Object rawObject, TableInfo tableInfo) {
        // 1、获取主键值
        Serializable keyValue;
        // 1.1、如果源数据是Map类型，则从Map中获取主键值
        if(rawObject instanceof Map) {
            Map<?,?> rawMap = (Map<?,?>) rawObject;
            keyValue = MapUtil.getStr(rawMap, tableInfo.getKeyProperty());
        }
        // 1.2、如果源数据是对象类型，则从对象中获取主键值
        else {
            keyValue = (Serializable) ReflectUtil.getFieldValue(rawObject, tableInfo.getKeyProperty());
        }
        return keyValue;
    }

}
