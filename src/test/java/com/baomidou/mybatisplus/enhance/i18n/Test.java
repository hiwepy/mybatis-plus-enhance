/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.i18n;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.SM4;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultEncryptedFieldHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;

public class Test {

    public static void main(String[] args) {

        // 随机生成sm4加密key
        String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
        System.out.println("sm4Key:"+sm4Key);
        /**
         * 偏移向量，加盐
         */
        String sm4Iv = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
        System.out.println("sm4Iv:"+sm4Iv);
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, sm4Key.getBytes(), sm4Iv.getBytes());
        System.out.println(sm4.encryptBase64("123"));

        sm4Key = Base64.encode(sm4Key.getBytes());
        System.out.println("sm4Key-Base64:"+ sm4Key);
        sm4Iv = Base64.encode(sm4Iv.getBytes());
        System.out.println("sm4Iv-Base64:"+ sm4Iv);

        DefaultEncryptedFieldHandler handler = new DefaultEncryptedFieldHandler( new ObjectMapper(),SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
                Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);

        System.out.println(handler.encrypt("123"));

    }

}
