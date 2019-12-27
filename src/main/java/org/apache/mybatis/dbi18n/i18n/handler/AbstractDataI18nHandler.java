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
package org.apache.mybatis.dbi18n.i18n.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.meta.MetaResultSetHandler;
import org.springframework.util.ObjectUtils;

@SuppressWarnings("unchecked")
public abstract class AbstractDataI18nHandler implements DataI18nHandler {

	@Override
	public Object handle(Locale locale,Invocation invocation,MetaResultSetHandler metaResultSetHandler, Object orginData, Object i18nData) throws Exception  {
		Collection<Object> orginList  = null;
		Collection<Object> i18nList   = null;
		// 原始数据集合化转换
		if(!Collection.class.isAssignableFrom(orginData.getClass())){
			orginList  = Arrays.asList(ObjectUtils.toObjectArray(orginData));
		} else {
			orginList  = (Collection<Object>) orginData;
		}
		// 原始数据为空，则跳过后面逻辑
		if(orginList == null || orginList.size() == 0){
			return orginList;
		}
		// 国际化数据集合化转换
		if(!Collection.class.isAssignableFrom(i18nData.getClass())){
			i18nList  = Arrays.asList(ObjectUtils.toObjectArray(i18nData));
		} else {
			i18nList  = (Collection<Object>) i18nData;
		}
		//国际化数据为空，则跳过后面逻辑
		if(i18nList == null || i18nList.size() == 0){
			return orginList;
		}
		return doHandle(locale, invocation, metaResultSetHandler, orginList , i18nList );
	}
	
	public abstract Object doHandle(Locale locale,Invocation invocation,MetaResultSetHandler metaResultSetHandler,Collection<Object> orginList,Collection<Object> i18nList) throws Exception ;

}
