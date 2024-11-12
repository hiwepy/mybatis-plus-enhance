package com.baomidou.mybatisplus.enhance.crypto.enums;

/**
 * 加密解密方式
 */
public enum CryptoType {

    /**
     * 默认的行为
     */
    NOOP,
    /**
     * 系统内部加解密
     */
    INTERNAL,
    /**
     * 弗兰科信息
     */
    FLKSEC

}
