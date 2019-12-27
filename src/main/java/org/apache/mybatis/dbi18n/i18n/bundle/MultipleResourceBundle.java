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
package org.apache.mybatis.dbi18n.i18n.bundle;

import java.util.Enumeration;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultipleResourceBundle extends ResourceBundle {

	protected ResourceBundle[] bundles;
	protected static Logger LOG = LoggerFactory.getLogger(MultipleResourceBundle.class);
	
	public MultipleResourceBundle() {
    }
	
	public MultipleResourceBundle(ResourceBundle ...bundles){
		this.bundles = bundles;
	}
	
	@Override
	protected Object handleGetObject(String key) {
		if (key == null) {
            throw new NullPointerException("key is null ");
        }
		for (ResourceBundle bundle : bundles) {
			try {
				Object value = bundle.getObject(key);
				if(value != null){
					return value;
				}
			} catch (Exception e) {
				// ingrone e
				LOG.warn(e.getMessage());
			}
		}
		return null;
	}

	@Override
	public Enumeration<String> getKeys() {
        return new ResourceBundleEnumeration( this.parent, this.bundles);
	}
	
};
