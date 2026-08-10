package com.baomidou.mybatisplus.enhance.plugins.inner;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;

/**
 * Lifecycle extension for MyBatis-Plus enhanced interceptors.
 *
 * <p>Adds post-query, post-update, and post-execution callbacks on top of the official
 * {@link InnerInterceptor} pre-execution hooks. Implementations are dispatched by the
 * {@code MybatisPlusEnhanceInterceptor} chain entry point.</p>
 *
 * <p>Result-enhancement methods may modify return objects. Execution-complete methods
 * must remain sidecar-only and must not affect the SQL main flow.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 2.0.0
 */
public interface EnhanceInnerInterceptor extends InnerInterceptor {

    /**
     * Declares the phase of this enhance interceptor within the unified chain.
     *
     * <p>Built-in enhance interceptors must return a definite phase. Third-party
     * enhancements default to {@link EnhancePhase#UNSPECIFIED} to preserve
     * compatibility with the official {@link InnerInterceptor} contract.</p>
     *
     * @return the enhance phase
     */
    default EnhancePhase phase() {
        return EnhancePhase.UNSPECIFIED;
    }

    /**
     * Post-query result enhancement callback, invoked after
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
     * completes successfully.
     *
     * @param executor       the MyBatis executor (may be a proxy)
     * @param ms             mapped statement metadata
     * @param parameter      mapper invocation parameter
     * @param rowBounds      pagination bounds
     * @param resultHandler  result handler
     * @param boundSql       the bound SQL
     * @param rtList         the query result list
     * @return the (possibly modified) result list; must not be {@code null}
     * @throws SQLException if result enhancement fails
     */
    default List<Object> afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                                    ResultHandler<?> resultHandler, BoundSql boundSql,
                                    List<Object> rtList) throws SQLException {
        return rtList;
    }

    /**
     * Post-update enhancement callback, invoked after
     * {@link Executor#update(MappedStatement, Object)} completes successfully.
     *
     * @param executor      the MyBatis executor (may be a proxy)
     * @param ms            mapped statement metadata
     * @param parameter     mapper invocation parameter
     * @param boundSql      the bound SQL
     * @param affectedRows  number of affected rows
     * @throws SQLException if update enhancement fails
     */
    default void afterUpdate(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                             int affectedRows) throws SQLException {
        // do nothing
    }

    /**
     * Post-execution lifecycle notification, invoked after all SQL execution and result
     * enhancement is complete. Covers query, insert, update, delete, and error paths.
     * Suitable for monitoring, tracing, and other sidecar capabilities.
     *
     * <p>Implementations must not throw exceptions that affect the SQL main flow.</p>
     *
     * @param executor      the MyBatis executor (may be a proxy)
     * @param ms            mapped statement metadata
     * @param parameter     mapper invocation parameter
     * @param boundSql      the bound SQL
     * @param result        the execution result; may be {@code null} on failure
     * @param failure       the execution or enhancement exception; {@code null} on success
     * @param elapsedNanos  executor actual execution time in nanoseconds
     */
    default void afterExecution(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                                Object result, Throwable failure, long elapsedNanos) {
        // do nothing
    }

}
