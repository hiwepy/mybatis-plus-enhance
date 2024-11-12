package com.baomidou.mybatisplus.enhance.sensitive.interceptor;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveJSONField;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveJSONFieldKey;
import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveType;
import com.baomidou.mybatisplus.enhance.sensitive.handler.SensitiveTypeRegisty;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Map;

@Slf4j
public class SensitiveInnerInterceptor implements InnerInterceptor {

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        // do nothing
        sh.getParameterHandler().getParameterObject();



    }

    private Object handleSensitiveField(Field field, Object value) {
        SensitiveField sensitiveField = field.getAnnotation(SensitiveField.class);
        Object newValue = value;
        if (sensitiveField != null && value != null) {
            newValue = SensitiveTypeRegisty.get(sensitiveField.value()).handle(value);
        }
        return newValue;
    }
    private Object handleSensitiveJSONField(Field field, Object value) {
        SensitiveJSONField sensitiveJSONField = field.getAnnotation(SensitiveJSONField.class);
        Object newValue = value;
        if (sensitiveJSONField != null && value != null) {
            newValue = processJsonField(newValue,sensitiveJSONField);
        }
        return newValue;
    }

    /**
     * 在json中进行脱敏
     * @param newValue new
     * @param sensitiveJSONField 脱敏的字段
     * @return json
     */
    private Object processJsonField(Object newValue,SensitiveJSONField sensitiveJSONField) {

        try{
            Map<String,Object> map = JSONUtil.parseObj(newValue.toString());
            SensitiveJSONFieldKey[] keys =sensitiveJSONField.sensitivelist();
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
