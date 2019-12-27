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

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class DataI18nMapper implements Serializable {

	protected String primaryName;
	protected Map<String, String> mapper;

	public String getPrimaryName() {
		return primaryName;
	}

	public void setPrimaryName(String primaryName) {
		this.primaryName = primaryName;
	}

	public Map<String, String> getMapper() {
		return mapper;
	}

	public void setMapper(Map<String, String> mapper) {
		this.mapper = mapper;
	}

}
