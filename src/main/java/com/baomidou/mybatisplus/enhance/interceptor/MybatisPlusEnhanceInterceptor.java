package com.baomidou.mybatisplus.enhance.interceptor;

import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedField;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedTable;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignatureField;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.interceptor.inner.EnhanceInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

/**
 * MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
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
public class MybatisPlusEnhanceInterceptor extends MybatisPlusInterceptor {

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
                ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];
                BoundSql boundSql;
                if (args.length == 4) {
                    boundSql = ms.getBoundSql(parameter);
                } else {
                    // 几乎不可能走进这里面,除非使用Executor的代理对象调用query[args[6]]
                    boundSql = (BoundSql) args[5];
                }
                for (InnerInterceptor interceptor : super.getInterceptors()) {
                    if (!interceptor.willDoQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql)) {
                        return Collections.emptyList();
                    }
                    interceptor.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
                }
                CacheKey cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
                List<Object> rtList = executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
                // 增加查询完成后的增强逻辑
                for (InnerInterceptor interceptor : super.getInterceptors()) {
                    if(interceptor instanceof EnhanceInnerInterceptor){
                        EnhanceInnerInterceptor innerInterceptor = (EnhanceInnerInterceptor) interceptor;
                        if (!innerInterceptor.willDoAfterQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql, rtList)) {
                            return rtList;
                        }
                        innerInterceptor.afterQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql, rtList);
                    }
                }
                return rtList;
            } else if (isUpdate) {
                for (InnerInterceptor update : super.getInterceptors()) {
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
                for (InnerInterceptor innerInterceptor : super.getInterceptors()) {
                    innerInterceptor.beforeGetBoundSql(sh);
                }
            } else {
                Connection connections = (Connection) args[0];
                Integer transactionTimeout = (Integer) args[1];
                for (InnerInterceptor innerInterceptor : super.getInterceptors()) {
                    innerInterceptor.beforePrepare(sh, connections, transactionTimeout);
                }
            }
        }
        return invocation.proceed();
    }

    @Override
    public String toString() {
        return "MybatisPlusEnhanceInterceptor{" +
                "interceptors=" + getInterceptors() +
                '}';
    }

}
