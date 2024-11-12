package com.baomidou.mybatisplus.enhance.datascope.handler;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.enhance.datascope.annotation.DataScopePlus;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限拼装逻辑处理
 */
@Slf4j
public class AnnotationDataPermissionHandler implements MultiDataPermissionHandler {

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
            /**
             * DataScope注解优先级：【类上 > 方法上】
             */
            // 获取 DataScope注解
            DataScopePlus dataScopeAnnotationClazz = mapperClazz.getAnnotation(DataScopePlus.class);
            if (ObjectUtils.isNotEmpty(dataScopeAnnotationClazz) && dataScopeAnnotationClazz.enabled()) {
                return buildDataScopeByAnnotation(dataScopeAnnotationClazz);
            }
            // 获取自身类中的所有方法，不包括继承。与访问权限无关
            Method[] methods = mapperClazz.getDeclaredMethods();
            for (Method method : methods) {
                DataScopePlus dataScopeAnnotationMethod = method.getAnnotation(DataScopePlus.class);
                if (ObjectUtils.isEmpty(dataScopeAnnotationMethod) || !dataScopeAnnotationMethod.enabled()) {
                    continue;
                }
                if (method.getName().equals(methodName) || (method.getName() + "_COUNT").equals(methodName) || (method.getName() + "_count").equals(methodName)) {
                    return buildDataScopeByAnnotation(dataScopeAnnotationMethod);
                }
            }
        } catch (ClassNotFoundException e) {
           log.error("【DataScopeHandlerPlus】获取数据权限注解失败，原因：{}", e.getMessage());
        }
        return null;
    }

    /**
     * DataScope注解方式，拼装数据权限
     *
     * @param controllerDataScope
     * @return
     */
    private Expression buildDataScopeByAnnotation(DataScopePlus controllerDataScope) {
        /*// 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            SysUser currentUser = loginUser.getUser();
            // 如果是超级管理员，则不过滤数据
            if (com.risk.control.common.utils.StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                List<SysRole> roleList = currentUser.getRoles();
                if(roleList.isEmpty()){
                    return null;
                }
                //List<Long> dataScopeDeptIds = new ArrayList<>();
                List<Long> createIdList = new ArrayList<>();
                Long dataScopeCreateId = null;
                // 目前用户只能是一种角色
                SysRole role = roleList.get(0);
                String dataScope = role.getDataScope();
                if(DataScopeConstants.DATA_SCOPE_ALL.equals(dataScope)){
                    return null;
                }
                // 处理所在本部门的数据
                if(DataScopeConstants.DATA_SCOPE_DEPT.equals(dataScope)){
                    createIdList = loginUser.getCreateIdList();
                }
                // 仅查询自己创建的数据
                if(DataScopeConstants.DATA_SCOPE_SELF.equals(dataScope)){
                    dataScopeCreateId = currentUser.getUserId();
                }
                Expression expression = dataScopeFilter(controllerDataScope.tableAlias(),controllerDataScope.oneselfScopeName(),
                        controllerDataScope.oneselfScopeName(),createIdList,dataScopeCreateId);
                log.info("【DataScopeHandlerPlus】数据权限处理的sql语句:" + expression.toString());
                return dataScopeFilter(controllerDataScope.tableAlias(),controllerDataScope.oneselfScopeName(),
                        controllerDataScope.oneselfScopeName(),createIdList,dataScopeCreateId);
            }
        }*/
        return null;
    }

    /**
     * 拼装数据权限
     *
     * @param tableAlias        表别名
     * @param deptScopeName     部门限制范围的字段名称
     * @param oneselfScopeName  本人限制范围的字段名称
     * @param createIdList  该用户部门下创建人id集合
     * @param dataScopeCreateId 数据权限本人ID
     * @return
     */
    private Expression dataScopeFilter(String tableAlias, String deptScopeName, String oneselfScopeName, List<Long> createIdList, Long dataScopeCreateId) {
        /**
         * 构造部门in表达式。
         */
        InExpression deptIdInExpression = null;
        if (CollectionUtils.isNotEmpty(createIdList)) {
            deptIdInExpression = new InExpression();
            ExpressionList deptIds = new ExpressionList(createIdList.stream().map(LongValue::new).collect(Collectors.toList()));
            // 设置左边的字段表达式，右边设置值。
            deptIdInExpression.setLeftExpression(buildColumn(tableAlias, deptScopeName));
            deptIdInExpression.setRightExpression(new Parenthesis(deptIds));
            //deptIdInExpression.setRightItemsList(deptIds);

        }

        /**
         * 构造本人eq表达式
         */
        EqualsTo oneselfEqualsTo = null;
        if (dataScopeCreateId != null) {
            oneselfEqualsTo = new EqualsTo();
            oneselfEqualsTo.withLeftExpression(buildColumn(tableAlias, oneselfScopeName));
            oneselfEqualsTo.setRightExpression(new LongValue(dataScopeCreateId));
        }

        if (deptIdInExpression != null && oneselfEqualsTo != null) {
            return new OrExpression(deptIdInExpression, oneselfEqualsTo);
        } else if (deptIdInExpression != null && oneselfEqualsTo == null) {
            return deptIdInExpression;
        } else if (deptIdInExpression == null && oneselfEqualsTo != null) {
            return oneselfEqualsTo;
        }
        return null;
    }

    /**
     * 构建Column
     *
     * @param tableAlias 表别名
     * @param columnName 字段名称
     * @return 带表别名字段
     */
    public static Column buildColumn(String tableAlias, String columnName) {
        if (StringUtils.isNotBlank(tableAlias)) {
            columnName = tableAlias + StringPool.DOT + columnName;
        }
        return new Column(columnName);
    }

}
