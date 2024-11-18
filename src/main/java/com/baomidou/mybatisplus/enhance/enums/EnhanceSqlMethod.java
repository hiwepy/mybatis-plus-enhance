package com.baomidou.mybatisplus.enhance.enums;

public enum EnhanceSqlMethod {

    /**
     * 查询
     */
    SELECT_IGNORE_DECRYPT_BY_ID("selectIgnoreDecryptById", "根据ID 查询一条数据", "SELECT %s FROM %s WHERE %s=#{%s} %s"),
    SELECT_IGNORE_DECRYPT_BATCH_BY_IDS("selectIgnoreDecryptBatchIds", "根据ID集合，批量查询数据", "<script>SELECT %s FROM %s WHERE %s IN (%s) %s </script>"),
    SELECT_IGNORE_DECRYPT_LIST("selectIgnoreDecryptList", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),
    SELECT_IGNORE_DECRYPT_MAPS("selectIgnoreDecryptMaps", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),
    SELECT_IGNORE_DECRYPT_OBJS("selectIgnoreDecryptObjs", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>")

    ;

    private final String method;
    private final String desc;
    private final String sql;

    EnhanceSqlMethod(String method, String desc, String sql) {
        this.method = method;
        this.desc = desc;
        this.sql = sql;
    }

    public String getMethod() {
        return method;
    }

    public String getDesc() {
        return desc;
    }

    public String getSql() {
        return sql;
    }
}
