package com.baomidou.mybatisplus.enhance.crypto.enums;

/**
 * Table-signature update strategy.
 *
 * <p>Whole-row signatures must be computed from the complete persisted state after update.
 * This strategy distinguishes ordinary partial updates, deferred re-sign after business
 * writes, full-row updates, and internal signature-column-only updates.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public enum SignatureUpdateStrategy {

    /**
     * Default safe strategy: rejects partial updates on signed tables when
     *完整性 cannot be proven.
     */
    REJECT_PARTIAL,

    /**
     * Writes business fields first, then reads the complete raw row within the
     * same transaction and刷新 the signature.
     */
    DEFERRED_RESIGN,

    /**
     * The caller guarantees the parameter contains the complete updated row,
     * allowing signature computation before the write.
     */
    FULL_ROW,

    /**
     * Framework-internal update that writes only the pre-computed signature column.
     */
    SIGNATURE_ONLY
}
