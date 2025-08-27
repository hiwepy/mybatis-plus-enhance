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
 */
package com.baomidou.mybatisplus.enhance.i18n.interceptor;

import com.baomidou.mybatisplus.enhance.i18n.handler.DataI18nHandler;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Locale;

@Slf4j
public abstract class AbstractDataI18nInterceptor extends JsqlParserSupport implements InnerInterceptor {

	protected DataI18nHandler i18nHandler;

	/**
	 *
	 * @param executor      Executor(可能是代理对象)
	 * @param ms            MappedStatement 对象
	 * @param parameter     parameter
	 * @param rowBounds     rowBounds
	 * @param resultHandler resultHandler
	 * @param boundSql      boundSql
	 * @throws SQLException
	 */
	@Override
	public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
/*
		return  SqlCommandType.SELECT.equals(ms.getSqlCommandType()) && method != null &&
				AnnotationUtils.findAnnotation(method, I18nSwitch.class) != null;*/
	}

	/*@Override
	protected boolean isRequireIntercept(Invocation invocation,ResultSetHandler resultSetHandler,MetaResultSetHandler metaResultSetHandler) {
		// 通过反射获取到当前MappedStatement
		MappedStatement mappedStatement = metaResultSetHandler.getMappedStatement();
		// 获取对应的BoundSql，这个BoundSql其实跟我们利用StatementHandler获取到的BoundSql是同一个对象。
		BoundSql boundSql = metaResultSetHandler.getBoundSql();
		Object paramObject = boundSql.getParameterObject();
		//提取被国际化注解标记的方法
		Method method = BeanMethodDefinitionFactory.getMethodDefinition(mappedStatement.getId(), paramObject != null ? new Class<?>[] {paramObject.getClass()} : null);
		return  SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType()) && method != null &&
				AnnotationUtils.findAnnotation(method, I18nMapper.class) != null;
	}

	protected boolean isIntercepted(CacheKey cacheKey) {
		//获取当前线程绑定的上下文对象
		String uniqueKey = DigestUtils.md5DigestAsHex(cacheKey.toString().getBytes());
		if(! extraContext.containsKey(uniqueKey)){
			return true;
		}
		extraContext.put(uniqueKey, cacheKey);
		return false;
	}*/

	public abstract Locale getLocale();

	/*protected Object wrapI18nParam(Locale locale, Invocation invocation, MetaResultSetHandler metaResultSetHandler, Object result,Object orginParam) throws Exception {
		if(this.i18nHandler == null){
			this.i18nHandler = new DefaultDataI18nHandler();
		}
		return this.i18nHandler.wrap(locale, invocation, metaResultSetHandler, result, orginParam);
	}

	protected Object doI18nMapper(Locale locale, Invocation invocation,MetaResultSetHandler metaResultSetHandler, Object orginList, List<Object> i18nDataList) throws Exception {
		if(this.i18nHandler == null){
			this.i18nHandler = new DefaultDataI18nHandler();
		}
		return this.i18nHandler.handle(locale, invocation, metaResultSetHandler, orginList, i18nDataList);
	}*/


}
