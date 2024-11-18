package com.baomidou.mybatisplus.enhance.interceptor.inner;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.IgnoreEncrypted;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.util.ParameterUtils;
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

    @Getter
    private final DataEncryptionHandler dataEncryptionHandler;
    @Getter
    private final boolean decryptSwitch;

    public DataDecryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler) {
        this(new DefaultDataEncryptionHandler(encryptedFieldHandler), true);
    }

    public DataDecryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler, boolean encryptSwitch) {
        this(new DefaultDataEncryptionHandler(encryptedFieldHandler), encryptSwitch);
    }

    public DataDecryptionInnerInterceptor(DataEncryptionHandler dataEncryptionHandler) {
        this(dataEncryptionHandler, false);
    }

    public DataDecryptionInnerInterceptor(DataEncryptionHandler dataEncryptionHandler, boolean decryptSwitch) {
        super();
        this.decryptSwitch = decryptSwitch;
        this.dataEncryptionHandler = dataEncryptionHandler;
    }

    @Override
    public void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(decryptSwitch, rtList)) {
            log.debug("DataDecryptionInnerInterceptor.afterQuery decryptSwitch is off, return directly.");
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
                    log.debug("mappedStatementId：{}, ignoreEncrypted is on, return directly.", mappedStatementId);
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("DataDecryptionInnerInterceptor.afterQuery ClassNotFoundException", e);
        }
        // 3、对查询结果进行解密
        for (Object object : rtList) {
            // 逐一解密
            getDataEncryptionHandler().doRawObjectDecrypt(object, object.getClass());
        }
    }

}
