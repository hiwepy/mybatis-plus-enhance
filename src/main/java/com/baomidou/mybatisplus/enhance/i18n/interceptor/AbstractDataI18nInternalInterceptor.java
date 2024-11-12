/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
package com.baomidou.mybatisplus.enhance.i18n.interceptor;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.meta.MetaResultSetHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.utils.MybatisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataI18nInternalInterceptor extends AbstractDataI18nInterceptor {

	protected static Logger LOG = LoggerFactory.getLogger(AbstractDataI18nInternalInterceptor.class);

	@Override
	public Object doResultSetIntercept(Invocation invocation,ResultSetHandler resultSetHandler,MetaResultSetHandler metaResultSetHandler) throws Throwable{
		// 获取处理结果
		Object result = invocation.proceed();
		//检查是否需要进行拦截处理
		if(result != null && isRequireIntercept(invocation, resultSetHandler, metaResultSetHandler)){

			// 获取当前上下文中的Locale对象
			Locale locale = this.getLocale();
			// 利用反射获取到FastResultSetHandler的mappedStatement属性，从而获取到MappedStatement；
			MappedStatement mappedStatement = metaResultSetHandler.getMappedStatement();
			// 获取当前MappedStatement的Mybatis Configuration对象
			Configuration configuration = metaResultSetHandler.getConfiguration();
			//connection = configuration.getEnvironment().getDataSource().getConnection();
			// 获取当前MappedStatement对应的国际化MappedStatement对象
			String newID = mappedStatement.getId() + "_" + locale.toString();
			LOG.debug(" Get i18n data query statement by id [" +  newID + "]" );
			// 获取与当前查询ID相同的Statement对象
			MappedStatement i18nMS = configuration.getMappedStatement(newID);
			//如果未定义当前查询方法对应的国际化查询配置
			if(i18nMS == null){
				// 返回原始结果
				return result;
			}

			// 利用反射获取到FastResultSetHandler的ParameterHandler属性，从而获取到ParameterObject；
			ParameterHandler parameterHandler = metaResultSetHandler.getParameterHandler();
			//定义BoundSql对象
		 	BoundSql boundSql = i18nMS.getBoundSql(parameterHandler.getParameterObject());
		 	//国际化查询参数
		 	Object i18nObject = super.wrapI18nParam(locale, invocation, metaResultSetHandler, result, parameterHandler.getParameterObject());
		 	//已经处理过国际化的数据跳过
		 	if( isIntercepted(MybatisUtils.createCacheKey(i18nMS, i18nObject, RowBounds.DEFAULT, boundSql, locale)) ){
		 		// 返回结果
		 		return result;
		 	}

			try {

				//获取当前数据库连接
				Connection connection = MybatisUtils.getDataSourceFromEnvironment(configuration).getConnection();
				//获取当前事物工厂对象
				TransactionFactory transactionFactory = MybatisUtils.getTransactionFactoryFromEnvironment(configuration);
				//创建事物对象
				Transaction tx = transactionFactory.newTransaction(connection);
				//创建执行器对象
				Executor executor = configuration.newExecutor(tx, ExecutorType.SIMPLE);
				//创建ResultHandler对象
				ResultHandler<Object> resultHandler = MybatisUtils.newResultHandler(configuration);

				//查询当前ID对应的国际化数据结果
				List<Object> i18nDataList = executor.query(i18nMS, i18nObject , RowBounds.DEFAULT, resultHandler);
				if(i18nDataList == null){
					return result;
				}

				// 调用统一国际化映射解析方法;处理后返回结果
				return super.doI18nMapper(locale, invocation, metaResultSetHandler, result, i18nDataList);

		    } catch (Exception e) {
		    	throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
		    } finally {
		    	ErrorContext.instance().reset();
		    }

		}

		// 返回结果
		return result;
	}

}
 */
