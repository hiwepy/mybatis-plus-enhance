package util;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.handlers.AnnotationHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedField;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedTable;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableHmacField;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class EncryptedFieldHelper {

    /**
     * 校验该实例的类是否被 @EncryptedTable所注解
     */
    public static boolean isExistEncryptedTable(Object object) {
        Class<?> objectClass = object.getClass();
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
     * 获取该类的所有标记为加密字段的属性列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getEncryptedFieldInfos(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return tableInfo.getFieldList().stream().filter(fieldInfo -> {
            /* 过滤注解非加密表字段属性 */
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            return Objects.nonNull(encryptedField);
        }).collect(Collectors.toList());
    }

    /**
     * <p>
     * 获取该类的所有标记为加密字段的属性列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static List<TableFieldInfo> getSortedEncryptedFieldInfos(Class<?> entityClazz) {
        return getEncryptedFieldInfos(entityClazz).stream().sorted(Comparator.comparing(info -> {
            TableFieldInfo fieldInfo = (TableFieldInfo) info;
            EncryptedField ef1 = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            Objects.requireNonNull(ef1, "EncryptedField annotation is required");
            return ef1.order();
        }).thenComparing(info -> {
            TableFieldInfo fieldInfo = (TableFieldInfo) info;
            return fieldInfo.getColumn();
        })).collect(Collectors.toList());
    }

    /**
     * <p>
     * 获取该类的标记为Hmac字段的属性
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static Optional<TableFieldInfo> getTableHmacFieldInfo(Class<?> entityClazz) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClazz);
        return tableInfo.getFieldList().stream().filter(fieldInfo -> {
            /* 过滤注解 @TableHmacField 字段属性 */
            TableHmacField hmacField = AnnotationUtils.findFirstAnnotation(TableHmacField.class, fieldInfo.getField());
            return Objects.nonNull(hmacField);
        }).findFirst();
    }

    /**
     * <p>
     * 获取该类的所有标记为加密字段的属性列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static Field[] getSortedEncryptedFields(Class<?> entityClazz) {
        return Arrays.stream(getEncryptedFields(entityClazz)).sorted(Comparator.comparing(field -> {
            EncryptedField ef1 = AnnotationUtils.findFirstAnnotation(EncryptedField.class, (Field) field);
            Objects.requireNonNull(ef1, "EncryptedField annotation is required");
            return ef1.order();
        }).thenComparing(field -> ReflectUtil.getFieldName((Field) field))).toArray(Field[]::new);
    }

    /**
     * <p>
     * 获取该类的所有标记为加密字段的属性列表
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static Field[] getEncryptedFields(Class<?> entityClazz) {
        return ReflectUtil.getFields(entityClazz, field -> {
            /* 过滤注解非表字段属性 */
            TableField tableField = AnnotationUtils.findFirstAnnotation(TableField.class, field);
            if(Objects.isNull(tableField) || !tableField.exist()){
                return Boolean.FALSE;
            }
            /* 过滤注解非加密表字段属性 */
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, field);
            return Objects.nonNull(encryptedField);
        });
    }

    /**
     * <p>
     * 获取该类的标记为Hmac字段的属性
     * </p>
     *
     * @param entityClazz 反射类
     * @return 属性集合
     */
    public static Optional<Field> getTableHmacField(Class<?> entityClazz) {
        return Arrays.stream(ReflectUtil.getFields(entityClazz, field -> {
            /* 过滤注解非表字段属性 */
            TableField tableField = AnnotationUtils.findFirstAnnotation(TableField.class, field);
            if(Objects.isNull(tableField) || !tableField.exist()){
                return Boolean.FALSE;
            }
            /* 过滤注解非加密表字段属性 */
            TableHmacField hmacField = AnnotationUtils.findFirstAnnotation(TableHmacField.class, field);
            return Objects.nonNull(hmacField);
        })).findFirst();
    }

}
