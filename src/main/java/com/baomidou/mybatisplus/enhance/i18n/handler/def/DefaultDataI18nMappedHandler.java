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
package com.baomidou.mybatisplus.enhance.i18n.handler.def;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nColumn;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nLocale;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nMapper;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nPrimary;
import com.baomidou.mybatisplus.enhance.i18n.handler.DataI18nMappedHandler;
import com.baomidou.mybatisplus.enhance.i18n.handler.DataI18nMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
@SuppressWarnings("unchecked")
public class DefaultDataI18nMappedHandler implements DataI18nMappedHandler {

	protected static Logger LOG = LoggerFactory.getLogger(DefaultDataI18nMappedHandler.class);
	protected static final ConcurrentMap<Class<?> , DataI18nMapper> COMPLIED_I18N_MAPPER = new ConcurrentHashMap<Class<?> , DataI18nMapper>();
	protected static final ConcurrentMap<Class<?> , Field[]> COMPLIED_FIELDS = new ConcurrentHashMap<Class<?> , Field[]>();
	protected static final ConcurrentMap<Class<?> , String> COMPLIED_PRIMARYS = new ConcurrentHashMap<Class<?> , String>();

	protected Field[] getCachedFields(Class<?> clazz) {
		Field[] ret = COMPLIED_FIELDS.get(clazz);
		if (ret != null) {
			return ret;
		}
		List<Field> fieldList = new ArrayList<Field>();
		for (Class<?> superClass = clazz; superClass != Object.class && superClass != null; superClass = superClass.getSuperclass()) {
			for (Field field : superClass.getDeclaredFields()) {
				fieldList.add(field);
			}
		}
		ret = fieldList.toArray(new Field[fieldList.size()]);
		Field[] existing = COMPLIED_FIELDS.putIfAbsent(clazz, ret);
		if (existing != null) {
			ret = existing;
		}
		return ret;
	}

	@Override
	public String getPrimaryName(I18nPrimary i18nPrimary, Object source) throws Exception{
		Class<?> clazz = source.getClass();
		String ret = COMPLIED_PRIMARYS.get(clazz);
		if (ret != null) {
			return ret;
		}
		synchronized (source) {
			String primaryName = null;
			//方法注解优先
			if(i18nPrimary != null){
				primaryName = i18nPrimary.value();
			}
			//其次使用字段注解
			if(StringUtils.isEmpty(primaryName)){
				for (Class<?> superClass = clazz; superClass != Object.class && superClass != null; superClass = superClass.getSuperclass()) {
					for (Field field : superClass.getDeclaredFields()) {
						//查找对象属性上的主键注解，直到找到为止
						if(field.getAnnotation(I18nPrimary.class) != null){
							ret = field.getName();
							String existing = COMPLIED_PRIMARYS.putIfAbsent(clazz, ret);
							if (existing != null) {
								ret = existing;
							}
							return ret;
						}
					}
				}
			}
		}
		return ret;
	}


	@Override
	public DataI18nMapper handle(Locale locale, I18nMapper i18nMapper, String primaryName , Object orginObject, Object i18nObject) throws Exception {
		DataI18nMapper ret = COMPLIED_I18N_MAPPER.get(orginObject.getClass());
		if (ret != null) {
			return ret;
		}
		ret = new DataI18nMapper();
		//解析主键名称
		ret.setPrimaryName(primaryName);
		//1、根据字段名称生成映射关系
		Map<String, String> mapperMap = new HashMap<String, String>();
		//原查询行数据对象是Map
		if(orginObject instanceof Map){
			Map<String,Object>  orginMap = ((Map<String,Object>) orginObject);
			if(i18nObject instanceof Map ){
				Map<String,Object>  i18nMap = ((Map<String,Object>) i18nObject);
				//循环原查询结果列
				for (String orgin_column : orginMap.keySet()) {
					//循环国际化数据结果列
					for (String i18n_column : i18nMap.keySet()) {
						//如果字段匹配：忽略大小写
						if(orgin_column.equalsIgnoreCase(i18n_column)){
							//记录该映射关系
							mapperMap.put(orgin_column, i18n_column);
							break;
						}
					}
				}
			} else {
				PropertyDescriptor[] i18nDescriptors = null;//BeanUtils.getPropertyDescriptors(i18nObject.getClass());
				//循环原查询结果列
				for (String orgin_column : orginMap.keySet()) {
					//循环国际化数据结果列
					for (PropertyDescriptor propDes : i18nDescriptors) {
						//如果字段匹配：忽略大小写
						if(orgin_column.equalsIgnoreCase(propDes.getName())){
							//记录该映射关系
							mapperMap.put(orgin_column, propDes.getName());
							break;
						}
					}
				}
			}
		}
		//原查询行数据对象不是Map
		else {

			PropertyDescriptor[] orginDescriptors = null;//BeanUtils.getPropertyDescriptors(orginObject.getClass());
			if(i18nObject instanceof Map ){
				Map<String,Object>  i18nMap = ((Map<String,Object>) i18nObject);
				//循环原查询结果列
				for (PropertyDescriptor propDes : orginDescriptors) {
					//循环国际化数据结果列
					for (String i18n_column : i18nMap.keySet()) {
						//如果字段匹配：忽略大小写
						if(propDes.getName().equalsIgnoreCase(i18n_column)){
							//记录该映射关系
							mapperMap.put(propDes.getName(), i18n_column);
							break;
						}
					}
				}
			} else {
				PropertyDescriptor[] i18nDescriptors = null;//BeanUtils.getPropertyDescriptors(i18nObject.getClass());
				//循环原查询结果列
				for (PropertyDescriptor propDes : orginDescriptors) {
					//循环国际化数据结果列
					for (PropertyDescriptor i18nDes : i18nDescriptors) {
						//如果字段匹配：忽略大小写
						if(propDes.getName().equalsIgnoreCase(i18nDes.getName())){
							//记录该映射关系
							mapperMap.put(propDes.getName(), i18nDes.getName());
							break;
						}
					}
				}
			}

			//循环原查询结果列
			for (PropertyDescriptor propDes : orginDescriptors) {
				// 解析字段注解映射关系
				for (Class<?> superClass = i18nObject.getClass(); superClass != Object.class && superClass != null; superClass = superClass.getSuperclass()) {
					//当前字段对象
					Field field = superClass.getDeclaredField(propDes.getName());
					//查找对象属性上的映射注解
					I18nColumn i18nColumn = field.getAnnotation(I18nColumn.class);
					if(i18nColumn != null){
						//默认以当前指定的列为映射列
						if(!StringUtils.isEmpty(i18nColumn.column())){
							mapperMap.put(propDes.getName(), i18nColumn.column());
						}
						//获取国际化语言映射列
						I18nLocale[] locales = i18nColumn.i18n();
						for (I18nLocale i18nLocale : locales) {
							//国际化语言匹配
							if(locale.toString().equals(i18nLocale.locale().getLocale().toString())){
								//记录该映射关系
								mapperMap.put(propDes.getName(), i18nLocale.column());
								break;
							}
						}
						break;
					}
				}
			}

		}

		//2、解析方法注解映射关系
		I18nColumn[] i18nColumns  = i18nMapper.value();
		if(i18nColumns != null && i18nColumns.length > 0){
			//循环标记对象
			for (I18nColumn i18nColumn : i18nColumns) {
				if(i18nColumn != null && !StringUtils.isEmpty(i18nColumn.column()) ){
					//获取国际化语言映射列
					I18nLocale[] locales = i18nColumn.i18n();
					for (I18nLocale i18nLocale : locales) {
						//国际化语言匹配
						if(locale.toString().equals(i18nLocale.locale().getLocale().toString())){
							//记录该映射关系
							mapperMap.put(i18nColumn.column(), i18nLocale.column());
							break;
						}
					}
				}
			}
		}

		ret.setMapper(mapperMap);
		DataI18nMapper existing = COMPLIED_I18N_MAPPER.putIfAbsent(orginObject.getClass(), ret);
		if (existing != null) {
			ret = existing;
		}
		return ret;
	}



}
