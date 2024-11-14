package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.enhance.util.SymmetricCryptoUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
public class DefaultEncryptedFieldHandler implements EncryptedFieldHandler {

    @Getter
    private ObjectMapper objectMapper;
    private final SymmetricAlgorithmType algorithmType;
    private final HmacAlgorithm hmacAlgorithm;
    private final Mode mode;
    private final Padding padding;
    private final String key;
    private final String iv;
    private final boolean plainIsEncode;

    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper, SymmetricAlgorithmType algorithmType, HmacAlgorithm hmacAlgorithm, Mode mode, Padding padding, String key) {
        this(objectMapper, algorithmType, hmacAlgorithm, mode, padding, key, null, true);
    }

    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper, SymmetricAlgorithmType algorithmType, HmacAlgorithm hmacAlgorithm, Mode mode, Padding padding, String key, String iv) {
        this(objectMapper, algorithmType, hmacAlgorithm, mode, padding, key, iv, true);
    }

    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper, SymmetricAlgorithmType algorithmType, HmacAlgorithm hmacAlgorithm, Mode mode, Padding padding, String key, String iv, boolean plainIsEncode) {
        this.objectMapper = objectMapper;
        this.algorithmType = algorithmType;
        this.hmacAlgorithm = hmacAlgorithm;
        this.mode = mode;
        this.padding = padding;
        this.key = Base64.decodeStr(key);
        this.iv = Objects.isNull(iv) ? null : Base64.decodeStr(iv);
        this.plainIsEncode = plainIsEncode;
    }

    @Override
    public <T> String encrypt(T value) {
        try {
            // 1、序列化Value
            String valueAsString = getObjectMapper().writeValueAsString(value);
            log.debug("Plain Value To {} Encrypt: {}", algorithmType.getName(), valueAsString);
            // 2、获取加密器
            SymmetricCrypto crypto = algorithmType.getSymmetricCrypto(mode, padding, key, iv);
            // 3、加密Value，如果 plainIsEncode =true 则对加密结果进行Base64
            if(plainIsEncode){
                valueAsString = crypto.encryptBase64(valueAsString);
            } else {
                valueAsString = crypto.encryptHex(valueAsString);
            }
            log.debug("{} Encrypt Value : {}", algorithmType.getName(), valueAsString);
            return valueAsString;
        } catch (Exception ex) {
            log.error("{} Encrypt Error : {}", algorithmType.getName(), ex.getMessage());
            throw ExceptionUtils.mpe("{} Encrypt Error", ex, algorithmType.getName());
        }
    }

    @Override
    public <T> T decrypt(String value, Class<T> rtType) {
        try {
            log.debug("Plain Value to {} Decrypt : {}", algorithmType.getName(), value);
            // 2、获取解密器
            SymmetricCrypto crypto = SymmetricCryptoUtil.getSymmetricCrypto(algorithmType.getName(), mode, padding, key, iv);
            // 3、解密请求体
            String decryptStr = crypto.decryptStr(value);
            log.debug("{} Decrypt Value : {}", algorithmType.getName(), decryptStr);
            return getObjectMapper().readValue(decryptStr, rtType);
        } catch (Exception ex) {
            log.error("{} Decrypt Error : {}", algorithmType.getName(), ex.getMessage());
            throw ExceptionUtils.mpe("{} Encrypt Error", ex, algorithmType.getName());
        }
    }

    @Override
    public <T> String hmac(T value) {
        try {
            log.debug("Plain Value to {} HMAC : {}", hmacAlgorithm.name(), value);
            HMac hMac = SymmetricCryptoUtil.getHmac(hmacAlgorithm, Base64.decodeStr(key));
            String hmacValue;
            if(plainIsEncode){
                hmacValue = hMac.digestBase64(getObjectMapper().writeValueAsString(value), StandardCharsets.UTF_8, Boolean.TRUE);
            } else {
                hmacValue = new String(hMac.digest(getObjectMapper().writeValueAsString(value)), StandardCharsets.UTF_8);
            }
            log.debug("HMAC Digest Value : {}", hmacValue);
            return hmacValue;
        } catch (Exception ex) {
            log.error("HMAC Digest Error : {}", ex.getMessage());
            throw ExceptionUtils.mpe("HMAC Digest Error", ex);
        }
    }

}
