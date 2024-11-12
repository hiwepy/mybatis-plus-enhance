package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

public enum SymmetricAlgorithmType {

    AES(SymmetricAlgorithm.AES.name()),
    ARCFOUR(SymmetricAlgorithm.ARCFOUR.name()),
    Blowfish(SymmetricAlgorithm.Blowfish.name()),
    DES(SymmetricAlgorithm.DES.name()),
    DESede(SymmetricAlgorithm.DESede.name()),
    RC2(SymmetricAlgorithm.RC2.name()),
    PBEWithMD5AndDES(SymmetricAlgorithm.PBEWithMD5AndDES.name()),
    PBEWithSHA1AndDESede(SymmetricAlgorithm.PBEWithSHA1AndDESede.name()),
    PBEWithSHA1AndRC2_40(SymmetricAlgorithm.PBEWithSHA1AndRC2_40.name()),

    SM4("SM4");

    private String name;

    SymmetricAlgorithmType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
