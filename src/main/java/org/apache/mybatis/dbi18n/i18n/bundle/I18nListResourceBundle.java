/**
 * Copyright (c) 2018, vindell (https://github.com/vindell).
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
package org.apache.mybatis.dbi18n.i18n.bundle;

import java.util.List;
import java.util.ListResourceBundle;


public class I18nListResourceBundle extends ListResourceBundle {
	
	/**国际化信息集合repeatable*/
	protected List<KeyValuePair> i18nList;
	
	 public I18nListResourceBundle(List<KeyValuePair> i18nList) {
		this.i18nList = i18nList;
	}

	protected Object[][] getContents() {
		Object[][] objects = new Object[this.i18nList.size()][0];
		for (int i = 0; i < this.i18nList.size(); i++) {
			KeyValuePair pair = this.i18nList.get(i);
			objects[i] = new Object[]{pair.getKey(), pair.getValue()};
		}
	    return objects;
	}
};