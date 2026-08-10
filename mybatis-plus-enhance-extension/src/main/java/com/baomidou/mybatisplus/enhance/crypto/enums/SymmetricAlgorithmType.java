package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.baomidou.mybatisplus.enhance.util.SymmetricCryptoUtil;
import lombok.Getter;

/**
 * Built-in symmetric encryption algorithm type.
 *
 * <p>Enum values describe algorithm names only; actual security depends on mode, padding,
 * key length, IV generation, and key management. New systems should prefer AES or SM4
 * with secure modes; legacy algorithms exist solely for backward compatibility with
 * existing ciphertext.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public enum SymmetricAlgorithmType {

    /** Advanced Encryption Standard (AES); the通用首选 for new systems. */
    AES(SymmetricAlgorithm.AES.name()),

    /** ARCFOUR/RC4 stream cipher; for legacy ciphertext compatibility only. */
    ARCFOUR(SymmetricAlgorithm.ARCFOUR.name()),

    /** Blowfish block cipher; for existing system compatibility only. */
    Blowfish(SymmetricAlgorithm.Blowfish.name()),

    /** DES block cipher; insufficient security strength, for legacy compatibility only. */
    DES(SymmetricAlgorithm.DES.name()),

    /** Triple DES (DESede); for legacy ciphertext compatibility only. */
    DESede(SymmetricAlgorithm.DESede.name()),

    /** RC2 block cipher; for existing system compatibility only. */
    RC2(SymmetricAlgorithm.RC2.name()),

    /** Password-based encryption with MD5 and DES; for legacy data compatibility only. */
    PBEWithMD5AndDES(SymmetricAlgorithm.PBEWithMD5AndDES.name()),

    /** Password-based encryption with SHA-1 and Triple DES; for legacy compatibility only. */
    PBEWithSHA1AndDESede(SymmetricAlgorithm.PBEWithSHA1AndDESede.name()),

    /** Password-based encryption with SHA-1 and 40-bit RC2; for legacy compatibility only. */
    PBEWithSHA1AndRC2_40(SymmetricAlgorithm.PBEWithSHA1AndRC2_40.name()),

    /** Chinese commercial cipher block algorithm SM4. */
    SM4("SM4");

    /**
     * Standard algorithm name passed to Hutool/JCE.
     */
    private final String name;

    /**
     * Creates an algorithm type.
     *
     * @param name the Hutool/JCE algorithm name
     */
    SymmetricAlgorithmType(String name) {
        this.name = name;
    }

    /**
     * Finds the enum value by algorithm name.
     *
     * @param name the Hutool/JCE algorithm name
     * @return the matching algorithm type, or {@code null} if not found
     */
    public SymmetricAlgorithmType getFor(String name) {
        for (SymmetricAlgorithmType type : SymmetricAlgorithmType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Creates a symmetric encryptor from string mode and padding names.
     *
     * @param mode    the block-cipher mode name
     * @param padding the padding scheme name
     * @param key     the encryption key; must not be logged or committed to source control
     * @param iv      the initialization vector; length must meet algorithm requirements
     * @return the configured symmetric encryptor
     */
    public SymmetricCrypto getSymmetricCrypto(String mode, String padding, String key, String iv) {
        return SymmetricCryptoUtil.getSymmetricCrypto(this.getName(), Mode.valueOf(mode), Padding.valueOf(padding), key, iv);
    }

    /**
     * Creates a symmetric encryptor from framework enum mode and padding.
     *
     * @param cipherMode    the block-cipher mode
     * @param cipherPadding the padding scheme
     * @param key           the encryption key; must not be logged or committed to source control
     * @param iv            the initialization vector; length must meet algorithm requirements
     * @return the configured symmetric encryptor
     * @since 2.0.0
     */
    public SymmetricCrypto getSymmetricCrypto(CipherMode cipherMode, CipherPadding cipherPadding, String key, String iv) {
        return SymmetricCryptoUtil.getSymmetricCrypto(this.getName(), cipherMode.toHutoolMode(), cipherPadding.toHutoolPadding(), key, iv);
    }

    /**
     * Creates a symmetric encryptor from Hutool mode and padding enums.
     *
     * @param mode    the block-cipher mode
     * @param padding the padding scheme
     * @param key     the encryption key; must not be logged or committed to source control
     * @param iv      the initialization vector; length must meet algorithm requirements
     * @return the configured symmetric encryptor
     * @deprecated Use {@link #getSymmetricCrypto(CipherMode, CipherPadding, String, String)} instead,
     *             to avoid exposing Hutool types in the public API.
     */
    @Deprecated
    public SymmetricCrypto getSymmetricCrypto(Mode mode, Padding padding, String key, String iv) {
        return SymmetricCryptoUtil.getSymmetricCrypto(this.getName(), mode, padding, key, iv);
    }

}
