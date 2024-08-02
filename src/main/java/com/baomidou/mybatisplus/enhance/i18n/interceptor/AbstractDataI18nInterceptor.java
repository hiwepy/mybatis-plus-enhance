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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nMapper;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nSwitch;
import com.baomidou.mybatisplus.enhance.i18n.i18n.handler.DataI18nHandler;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.AbstractInterceptorAdapter;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.meta.MetaResultSetHandler;
import org.apache.ibatis.plugin.meta.MetaStatementHandler;
import com.baomidou.mybatisplus.enhance.i18n.i18n.handler.def.DefaultDataI18nHandler;
import org.mybatis.spring.cache.BeanMethodDefinitionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractDataI18nInterceptor extends AbstractInterceptorAdapter {

	protected static Logger LOG = LoggerFactory.getLogger(AbstractDataI18nInterceptor.class);
	protected DataI18nHandler i18nHandler;

	@Override
	protected boolean isRequireIntercept(Invocation invocation, StatementHandler statementHandler, MetaStatementHandler metaStatementHandler) {
		// 通过反射获取到当前MappedStatement
		MappedStatement mappedStatement = metaStatementHandler.getMappedStatement();
		// 获取对应的BoundSql，这个BoundSql其实跟我们利用StatementHandler获取到的BoundSql是同一个对象。
		BoundSql boundSql = metaStatementHandler.getBoundSql();
		Object paramObject = boundSql.getParameterObject();
		//提取被国际化注解标记的方法
		Method method = BeanMethodDefinitionFactory.getMethodDefinition(mappedStatement.getId(), paramObject != null ? new Class<?>[] {paramObject.getClass()} : null);
		return  SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType()) && method != null &&
				AnnotationUtils.findAnnotation(method, I18nSwitch.class) != null;
	}

	@Override
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
	}

	public abstract Locale getLocale();

	protected Object wrapI18nParam(Locale locale, Invocation invocation, MetaResultSetHandler metaResultSetHandler, Object result,Object orginParam) throws Exception {
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
	}


	@Override
	public void setInterceptProperties(Properties properties) {
		String i18nHandlerClazz = properties.getProperty("i18nHandler");
		if(!StringUtils.isEmpty(i18nHandlerClazz)){
			try {
				Class<?> clazz = Class.forName(i18nHandlerClazz);
				this.i18nHandler = BeanUtils.instantiateClass(clazz, DataI18nHandler.class);
			} catch (ClassNotFoundException e) {
				LOG.warn("Class :" + i18nHandlerClazz + " is not found !");
			} catch (Exception e) {
				LOG.warn(e.getMessage());
			}
		}
	}


	@Override
	public void doDestroyIntercept(Invocation invocation) throws Throwable {
		extraContext.clear();
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}


}
