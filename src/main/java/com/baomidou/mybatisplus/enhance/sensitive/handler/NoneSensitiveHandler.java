package com.baomidou.mybatisplus.enhance.sensitive.handler;

import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveType;

/**
 * 不脱敏
 * @author chenhaiyang
 */
public class NoneSensitiveHandler implements SensitiveTypeHandler {
    @Override
    public SensitiveType getSensitiveType() {
        return SensitiveType.NONE;
    }

    @Override
    public String handle(Object src) {
        if(src!=null){
            return src.toString();
        }
        return null;
    }
}
