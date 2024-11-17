package com.baomidou.mybatisplus.enhance.datascope.handler;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.enhance.datascope.annotation.DataScopePlus;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

import java.lang.reflect.Method;

/**
 * 数据权限拼装逻辑处理
 */
@Slf4j
public class DataScopeAnnotationHandler implements MultiDataPermissionHandler {

    @Getter
    private final DataScopeExpressionProvider dataScopeExpressionProvider;

    public DataScopeAnnotationHandler(DataScopeExpressionProvider dataScopeExpressionProvider) {
        this.dataScopeExpressionProvider = dataScopeExpressionProvider;
    }

    /**
     * 获取数据权限 SQL 片段。
     * <p>旧的 {@link MultiDataPermissionHandler#getSqlSegment(Expression, String)} 方法第一个参数包含所有的 where 条件信息，如果 return 了 null 会覆盖原有的 where 数据，</p>
     * <p>新版的 {@link MultiDataPermissionHandler#getSqlSegment(Table, Expression, String)} 方法不能覆盖原有的 where 数据，如果 return 了 null 则表示不追加任何 where 条件</p>
     *
     * @param table             所执行的数据库表信息，可以通过此参数获取表名和表别名
     * @param where             原有的 where 条件信息
     * @param mappedStatementId Mybatis MappedStatement Id 根据该参数可以判断具体执行方法
     * @return JSqlParser 条件表达式，返回的条件表达式会拼接在原有的表达式后面（不会覆盖原有的表达式）
     */
    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        try {
            Class<?> mapperClazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(".")));
            String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
            /*
             * DataScope注解优先级：【类上 > 方法上】
             */
            // 获取 DataScope注解
            DataScopePlus dataScopeAnnotationClazz = mapperClazz.getAnnotation(DataScopePlus.class);
            if (ObjectUtils.isNotEmpty(dataScopeAnnotationClazz) && dataScopeAnnotationClazz.enabled()) {
                return getDataScopeExpressionProvider().getDataScopeSqlSegment(table, where, mappedStatementId, dataScopeAnnotationClazz);
            }
            // 获取自身类中的所有方法，不包括继承。与访问权限无关
            Method[] methods = mapperClazz.getDeclaredMethods();
            for (Method method : methods) {
                DataScopePlus dataScopeAnnotationMethod = method.getAnnotation(DataScopePlus.class);
                if (ObjectUtils.isEmpty(dataScopeAnnotationMethod) || !dataScopeAnnotationMethod.enabled()) {
                    continue;
                }
                if (method.getName().equals(methodName) || (method.getName() + "_COUNT").equals(methodName) || (method.getName() + "_count").equals(methodName)) {
                    return getDataScopeExpressionProvider().getDataScopeSqlSegment(table, where, mappedStatementId, dataScopeAnnotationClazz);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }





}
