package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.Padding;

/**
 * Symmetric encryption padding scheme.
 *
 * <p>Encapsulates standard block-cipher padding schemes for use in the public API,
 * isolating third-party crypto library types.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 2.0.0
 */
public enum CipherPadding {

    /** No padding; plaintext length must be a multiple of the block size. */
    NoPadding(Padding.NoPadding),

    /** Zero-byte padding. */
    ZeroPadding(Padding.ZeroPadding),

    /** ISO 10126 padding. */
    ISO10126Padding(Padding.ISO10126Padding),

    /** Optimal Asymmetric Encryption Padding; used with RSA. */
    OAEPPadding(Padding.OAEPPadding),

    /** PKCS#1 padding; used with RSA. */
    PKCS1Padding(Padding.PKCS1Padding),

    /** PKCS#5 padding (equivalent to PKCS#7 for 8-byte blocks); generally recommended. */
    PKCS5Padding(Padding.PKCS5Padding),

    /** SSL3 padding. */
    SSL3Padding(Padding.SSL3Padding);

    private final Padding hutoolPadding;

    CipherPadding(Padding hutoolPadding) {
        this.hutoolPadding = hutoolPadding;
    }

    /**
     * Returns the internal Hutool padding enum for framework-internal use only.
     *
     * @return the Hutool {@link Padding} instance
     */
    public Padding toHutoolPadding() {
        return hutoolPadding;
    }
}
