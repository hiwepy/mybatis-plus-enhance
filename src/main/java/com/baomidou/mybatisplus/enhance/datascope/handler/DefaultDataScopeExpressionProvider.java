package com.baomidou.mybatisplus.enhance.datascope.handler;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.enhance.datascope.annotation.DataScopePlus;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultDataScopeExpressionProvider implements DataScopeExpressionProvider{

    @Override
    public Expression getDataScopeSqlSegment(Table table, Expression where, String mappedStatementId, DataScopePlus dataScopeAnnotation) {
        /*// 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (com.risk.control.common.utils.StringUtils.isNotNull(loginUser))
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
                if(Constants.DataScope.DATA_SCOPE_ALL.equals(dataScope)){
                    return null;
                }
                // 处理所在本部门的数据
                if(Constants.DataScope.DATA_SCOPE_DEPT.equals(dataScope)){
                    createIdList = loginUser.getCreateIdList();
                }
                // 仅查询自己创建的数据
                if(Constants.DataScope.DATA_SCOPE_SELF.equals(dataScope)){
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
            deptIdInExpression.setRightExpression(new ParenthesedExpressionList(deptIds));
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
            columnName = tableAlias + "." + columnName;
        }
        return new Column(columnName);
    }

}
