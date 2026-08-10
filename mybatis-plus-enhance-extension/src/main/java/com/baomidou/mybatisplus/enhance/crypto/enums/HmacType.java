package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.digest.HmacAlgorithm;

/**
 * HMAC signature algorithm type.
 *
 * <p>Encapsulates standard HMAC algorithms for use in the public API,
 * isolating third-party crypto library types.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 2.0.0
 */
public enum HmacType {

    /** HMAC-MD5; for legacy system compatibility only. */
    HmacMD5(HmacAlgorithm.HmacMD5),

    /** HMAC-SHA1. */
    HmacSHA1(HmacAlgorithm.HmacSHA1),

    /** HMAC-SHA256; generally recommended. */
    HmacSHA256(HmacAlgorithm.HmacSHA256),

    /** HMAC-SHA384. */
    HmacSHA384(HmacAlgorithm.HmacSHA384),

    /** HMAC-SHA512. */
    HmacSHA512(HmacAlgorithm.HmacSHA512);

    private final HmacAlgorithm hutoolAlgorithm;

    HmacType(HmacAlgorithm hutoolAlgorithm) {
        this.hutoolAlgorithm = hutoolAlgorithm;
    }

    /**
     * Returns the internal Hutool HMAC algorithm enum for framework-internal use only.
     *
     * @return the Hutool {@link HmacAlgorithm} instance
     */
    public HmacAlgorithm toHutoolAlgorithm() {
        return hutoolAlgorithm;
    }
}
