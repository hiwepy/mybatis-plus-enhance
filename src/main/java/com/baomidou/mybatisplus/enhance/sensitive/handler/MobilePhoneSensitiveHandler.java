package com.baomidou.mybatisplus.enhance.sensitive.handler;

import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveType;
import org.apache.commons.lang3.StringUtils;

/**
 * 手机号脱敏处理类
 * 18233583070 脱敏后: 182****3030
 * @author ;
 */
public class MobilePhoneSensitiveHandler implements SensitiveTypeHandler {
    @Override
    public SensitiveType getSensitiveType() {
        return SensitiveType.MOBILE_PHONE;
    }

    @Override
    public String handle(Object src) {
        if(src==null){
            return null;
        }
        String value = src.toString();
        return StringUtils.left(value, 3).concat(StringUtils.removeStart(StringUtils.leftPad(StringUtils.right(value, 4), StringUtils.length(value), "*"), "***"));
    }

}
