package com.baomidou.mybatisplus.enhance.interceptor;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedField;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedTable;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableHmacField;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.toolkit.PropertyMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import util.EncryptedFieldHelper;
import util.ParameterUtils;

import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

/**
 * MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
 * - 该拦截器会拦截 MybatisPlus 的所有操作，对查询结果进行解密操作
 * - 该拦截器需要配合 {@link EncryptedTable} 注解使用，用于标记需要解密的实体类
 * - 该拦截器需要配合 {@link EncryptedField} 注解使用，用于标记需要解密的字段
 * - 该该拦截器需要配合 {@link TableHmacField} 注解使用，用于标记需要存储HMAC签名的字段
 * - 该拦截器需要实现 {@link EncryptedFieldHandler} 接口，用于实现解密和签名逻辑
 * - 该拦截器会对所有查询结果进行解密操作，如果查询结果中存在加密字段，会对加密字段进行解密操作
 * 参考：
 * - https://blog.csdn.net/tianmaxingkonger/article/details/130986784
 */
@Intercepts(
    {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
    }
)
public class MybatisPlusDecryptInterceptor implements Interceptor {

    /**
     * 加解密处理器，加解密的情况都在该处理器中自行判断
     */
    @Getter
    private final EncryptedFieldHandler encryptedFieldHandler;
    @Getter
    private final boolean checkHmac;

    public MybatisPlusDecryptInterceptor(EncryptedFieldHandler encryptedFieldHandler) {
        this(false, encryptedFieldHandler);
    }

    public MybatisPlusDecryptInterceptor(boolean checkHmac, EncryptedFieldHandler encryptedFieldHandler) {
        super();
        this.checkHmac = checkHmac;
        this.encryptedFieldHandler = encryptedFieldHandler;
    }


    @Setter
    private List<InnerInterceptor> interceptors = new ArrayList<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();
        if (target instanceof Executor) {
            final Executor executor = (Executor) target;
            Object parameter = args[1];
            boolean isUpdate = args.length == 2;
            MappedStatement ms = (MappedStatement) args[0];
            if (!isUpdate && ms.getSqlCommandType() == SqlCommandType.SELECT) {
                RowBounds rowBounds = (RowBounds) args[2];
                ResultHandler resultHandler = (ResultHandler) args[3];
                BoundSql boundSql;
                if (args.length == 4) {
                    boundSql = ms.getBoundSql(parameter);
                } else {
                    // 几乎不可能走进这里面,除非使用Executor的代理对象调用query[args[6]]
                    boundSql = (BoundSql) args[5];
                }
                for (InnerInterceptor query : interceptors) {
                    if (!query.willDoQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql)) {
                        return Collections.emptyList();
                    }
                    query.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
                }
                CacheKey cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
                List<Object> rtObject = executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
                if (CollectionUtils.isEmpty(rtObject)) {
                    return rtObject;
                }
                for (Object object : rtObject) {
                    // 逐一解密
                    handleResultSets(object);
                }
            } else if (isUpdate) {
                for (InnerInterceptor update : interceptors) {
                    if (!update.willDoUpdate(executor, ms, parameter)) {
                        return -1;
                    }
                    update.beforeUpdate(executor, ms, parameter);
                }
            }
        } else {
            // StatementHandler
            final StatementHandler sh = (StatementHandler) target;
            // 目前只有StatementHandler.getBoundSql方法args才为null
            if (null == args) {
                for (InnerInterceptor innerInterceptor : interceptors) {
                    innerInterceptor.beforeGetBoundSql(sh);
                }
            } else {
                Connection connections = (Connection) args[0];
                Integer transactionTimeout = (Integer) args[1];
                for (InnerInterceptor innerInterceptor : interceptors) {
                    innerInterceptor.beforePrepare(sh, connections, transactionTimeout);
                }
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor || target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    public void addInnerInterceptor(InnerInterceptor innerInterceptor) {
        this.interceptors.add(innerInterceptor);
    }

    public List<InnerInterceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }

    /**
     * 使用内部规则,拿分页插件举个栗子:
     * <p>
     * - key: "@page" ,value: "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor"
     * - key: "page:limit" ,value: "100"
     * <p>
     * 解读1: key 以 "@" 开头定义了这是一个需要组装的 `InnerInterceptor`, 以 "page" 结尾表示别名
     * value 是 `InnerInterceptor` 的具体的 class 全名
     * 解读2: key 以上面定义的 "别名 + ':'" 开头指这个 `value` 是定义的该 `InnerInterceptor` 属性需要设置的值
     * <p>
     * 如果这个 `InnerInterceptor` 不需要配置属性也要加别名
     */
    @Override
    public void setProperties(Properties properties) {
        PropertyMapper pm = PropertyMapper.newInstance(properties);
        Map<String, Properties> group = pm.group(StringPool.AT);
        group.forEach((k, v) -> {
            InnerInterceptor innerInterceptor = ClassUtils.newInstance(k);
            innerInterceptor.setProperties(v);
            addInnerInterceptor(innerInterceptor);
        });
    }

    @Override
    public String toString() {
        return "MybatisPlusInterceptor{" +
                "interceptors=" + interceptors +
                '}';
    }

    /**
     * 对单个对象进行解密
     * @param rtObject 单个对象
     * @param <T> 对象类型
     */
    private <T> void handleResultSets(T rtObject) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, rtObject.getClass());
        if(Objects.isNull(encryptedTable)){
            return;
        }

        // 3、获取该类的所有标记为加密字段的属性列表
        List<TableFieldInfo> encryptedFieldInfos = EncryptedFieldHelper.getSortedEncryptedFieldInfos(rtObject.getClass());
        if (CollectionUtils.isEmpty(encryptedFieldInfos)) {
            return;
        }

        // 4、遍历字段，对字段进行加密和签名处理
        StringJoiner hmacJoiner = checkHmac && encryptedTable.hmac() ? new StringJoiner(Constants.PIPE) : null;
        for (TableFieldInfo fieldInfo : encryptedFieldInfos) {
            // 4.1、获取字段上的@EncryptedField注解
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            if (Objects.isNull(encryptedField)) {
                continue;
            }

            // 4.2、获取加密字段的原始值
            Object fieldValue = ReflectUtil.getFieldValue(rtObject, fieldInfo.getField());
            // 4.3、如果原始值不为空，则对原始值进行解密处理
            if (Objects.nonNull(fieldValue)) {
                // 4.3.1、对原始值进行解密处理
                fieldValue = getEncryptedFieldHandler().decrypt(Objects.toString(fieldValue), fieldValue.getClass());
                // 4.3.2、将解密后的值通过反射设置到字段上
                ReflectUtil.setFieldValue(rtObject, fieldInfo.getField(), fieldValue);
            }
            // 4.4、如果加密字段需要进行HMAC签名验证，则将原始值加入到HMAC加密列表中
            if (Objects.nonNull(hmacJoiner) && encryptedTable.hmac() && encryptedField.hmac()) {
                hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
            }
        }
        // 5、如果数据表需要进行单表数据存储完整性验证，则对数据表进行HMAC签名处理
        if (checkHmac && encryptedTable.hmac() && Objects.nonNull(hmacJoiner)){
            // 5.1、获取数据表的HMAC字段
            Optional<TableFieldInfo> hmacFieldInfo = EncryptedFieldHelper.getTableHmacFieldInfo(rtObject.getClass());
            // 5.2、如果数据表的HMAC字段存在，则将HMAC签名值通过反射设置到HMAC字段上
            if(hmacFieldInfo.isPresent()){
                // 5.2.1、获取数据表的HMAC字段的值
                Object hmacFieldValue = ReflectUtil.getFieldValue(rtObject, hmacFieldInfo.get().getProperty());
                // 5.2.2、如果签名验证不通过，则抛出异常
                TableName tableName = AnnotationUtils.findFirstAnnotation(TableName.class, rtObject.getClass());
                // 5.2.3、对HMAC加密列表进行HMAC签名处理
                String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
                // 5.2.4、如果HMAC签名不匹配，则抛出异常
                ExceptionUtils.throwMpe(!Objects.equals(hmacValue, hmacFieldValue),
                        "表【%s】的数据列【%s】,数据存储完整性验证不通过，可能发生数据篡改，请检查数据完整性",
                        tableName.value(),
                        encryptedFieldInfos.stream().map(TableFieldInfo::getColumn).reduce((a, b) -> a + Constants.COMMA + b).orElse(Constants.EMPTY));
            }
        }
    }

}
