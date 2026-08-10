package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.Mode;

/**
 * Symmetric encryption block-cipher mode of operation.
 *
 * <p>Encapsulates standard block-cipher modes for use in the public API, isolating
 * third-party crypto library types. New systems should prefer {@link #CBC}; {@link #ECB}
 * is not recommended for encryption.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 2.0.0
 */
public enum CipherMode {

    /** No mode. */
    NONE(Mode.NONE),

    /** Cipher Block Chaining mode; generally recommended. */
    CBC(Mode.CBC),

    /** Cipher Feedback mode. */
    CFB(Mode.CFB),

    /** Counter mode. */
    CTR(Mode.CTR),

    /** Cipher Text Stealing. */
    CTS(Mode.CTS),

    /** Electronic Codebook mode; not recommended for encryption. */
    ECB(Mode.ECB),

    /** Output Feedback mode. */
    OFB(Mode.OFB),

    /** Propagating Cipher Block Chaining. */
    PCBC(Mode.PCBC);

    private final Mode hutoolMode;

    CipherMode(Mode hutoolMode) {
        this.hutoolMode = hutoolMode;
    }

    /**
     * Returns the internal Hutool mode enum for framework-internal use only.
     *
     * @return the Hutool {@link Mode} instance
     */
    public Mode toHutoolMode() {
        return hutoolMode;
    }
}
