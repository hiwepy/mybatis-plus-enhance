package com.baomidou.mybatisplus.enhance.sensitive.handler;

import com.baomidou.mybatisplus.enhance.sensitive.annotation.SensitiveType;
import org.apache.commons.lang3.StringUtils;

/**
 * 身份证号脱敏类型
 * 前3位，后4位
 * 130722199102323232 脱敏后: 130*************3232
 * @author ;
 */
public class IDCardSensitiveHandler implements SensitiveTypeHandler {
    @Override
    public SensitiveType getSensitiveType() {
        return SensitiveType.ID_CARD;
    }

    @Override
    public String handle(Object src) {
        if(src==null){
            return null;
        }
        String idCard = src.toString();
        return StringUtils.left(idCard, 3).concat(StringUtils.removeStart(StringUtils.leftPad(StringUtils.right(idCard, 4), StringUtils.length(idCard), "*"), "***"));

    }
}
