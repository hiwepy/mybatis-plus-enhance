package com.baomidou.mybatisplus.enhance.enums;

import lombok.Getter;

/**
 * Raw-data query SQL templates injected by {@code EnhanceSqlInjector}.
 *
 * <p>These methods use {@code IgnoreEncrypted} semantics to skip query-result decryption.
 * They are intended primarily for table-signature re-signing, verification, and ciphertext
 * maintenance scenarios, and should not be directly exposed to the business layer.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public enum EnhanceSqlMethod {

    /**
     * Queries a single entity's raw persisted data by primary key.
     */
    SELECT_IGNORE_DECRYPT_BY_ID("selectIgnoreDecryptById", "Select one row by ID", "SELECT %s FROM %s WHERE %s=#{%s} %s"),

    /**
     * Batch-queries entities' raw persisted data by a set of primary keys.
     */
    SELECT_IGNORE_DECRYPT_BATCH_BY_IDS("selectIgnoreDecryptBatchIds", "Batch-select rows by ID collection", "<script>SELECT %s FROM %s WHERE %s IN (%s) %s </script>"),

    /**
     * Queries a list of entities' raw persisted data matching the given Wrapper conditions.
     */
    SELECT_IGNORE_DECRYPT_LIST("selectIgnoreDecryptList", "Select all rows matching conditions", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),

    /**
     * Queries a list of Maps containing raw persisted data matching the given Wrapper conditions.
     */
    SELECT_IGNORE_DECRYPT_MAPS("selectIgnoreDecryptMaps", "Select all rows as Maps matching conditions", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),

    /**
     * Queries a list of first-column raw persisted values matching the given Wrapper conditions.
     */
    SELECT_IGNORE_DECRYPT_OBJS("selectIgnoreDecryptObjs", "Select all first-column values matching conditions", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),

    /**
     * Updates only the table-signature storage column by primary key, without rewriting
     * other business columns.
     */
    UPDATE_SIGNATURE_BY_ID("updateSignatureById", "Update table signature by ID only", "<script>UPDATE %s SET %s=#{et.%s} WHERE %s=#{et.%s} %s</script>");

    /**
     * Method name injected into the Mapper interface.
     */
    private final String method;

    /**
     * Human-readable description of the method's purpose.
     */
    private final String desc;

    /**
     * SQL script template for the MyBatis-Plus SQL injector to format.
     */
    private final String sql;

    /**
     * Creates an enhanced SQL method definition.
     *
     * @param method the Mapper method name
     * @param desc   human-readable description
     * @param sql    SQL script template
     */
    EnhanceSqlMethod(String method, String desc, String sql) {
        this.method = method;
        this.desc = desc;
        this.sql = sql;
    }

}
