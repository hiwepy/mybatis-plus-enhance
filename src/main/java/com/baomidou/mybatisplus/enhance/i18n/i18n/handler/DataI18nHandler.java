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
package com.baomidou.mybatisplus.enhance.i18n.i18n.handler;

import java.util.Locale;

import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.meta.MetaResultSetHandler;

public interface DataI18nHandler {

	Object wrap(Locale locale, Invocation invocation, MetaResultSetHandler metaResultSetHandler, Object result, Object orginParam) throws Exception ;

	Object handle(Locale locale, Invocation invocation, MetaResultSetHandler metaResultSetHandler, Object orginData,Object i18nData) throws Exception ;

}
