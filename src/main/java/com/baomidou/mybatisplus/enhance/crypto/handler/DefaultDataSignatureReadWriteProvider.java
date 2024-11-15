package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.enhance.util.EncryptedFieldHelper;

import java.util.Map;
import java.util.Optional;

/**
 * 默认的数据签名读写提供者，直接通过反射读写签名字段
 */
public class DefaultDataSignatureReadWriteProvider implements DataSignatureReadWriteProvider {

    /**
     * 从对象中读取签名值
     * @param rawObject 数据对象
     * @return 签名值
     * @param <T> 对象类型
     */
    @Override
    public <T> Optional<Object> readSignature(Object rawObject, Class<T> entityClass) {
        // 1、获取存储签名结果的字段
        Optional<TableFieldInfo> signatureStoreFieldInfo = EncryptedFieldHelper.getTableSignatureStoreFieldInfo(rawObject.getClass());
        // 2、如果存储签名结果的字段存在，则进行签名验证
        if(signatureStoreFieldInfo.isPresent()) {
            // 2.1、如果存储签名结果的字段是Map类型，则从Map中获取签名值
            if(rawObject instanceof Map) {
                Map<?,?> rawMap = (Map<?,?>) rawObject;
                Object signFieldValue = MapUtil.getStr(rawMap, signatureStoreFieldInfo.get().getProperty());
                return Optional.ofNullable(signFieldValue);
            }
            // 2.2、如果存储签名结果的字段是对象类型，则从对象中获取签名值
            Object signFieldValue = ReflectUtil.getFieldValue(rawObject, signatureStoreFieldInfo.get().getProperty());
            return Optional.ofNullable(signFieldValue);
        }
        // 3、如果存储签名结果的字段不存在，则返回空
        return Optional.empty();
    }

    /**
     * 将签名值写到对象中
     * @param rawObject 数据对象
     * @param signValue 签名值
     * @param <T> 对象类型
     */
    @Override
    public <T> void writeSignature(T rawObject, String signValue) {
        // 6.2、获取存储的签名结果的字段
        Optional<TableFieldInfo> signatureStoreFieldInfo = EncryptedFieldHelper.getTableSignatureStoreFieldInfo(rawObject.getClass());
        // 6.3、如果存储的签名结果的字段存在，则将签名值通过反射设置到字段上
        signatureStoreFieldInfo.ifPresent(fieldInfo -> ReflectUtil.setFieldValue(rawObject, fieldInfo.getField(), signValue));
    }

}
