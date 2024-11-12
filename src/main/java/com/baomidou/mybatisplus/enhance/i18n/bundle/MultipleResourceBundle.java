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
package com.baomidou.mybatisplus.enhance.i18n.bundle;

import java.util.Enumeration;
import java.util.ResourceBundle;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultipleResourceBundle extends ResourceBundle {

	protected ResourceBundle[] bundles;

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
                return bundle.getObject(key);
            } catch (Exception e) {
				// ingrone e
				log.warn(e.getMessage());
			}
		}
		return null;
	}

	@Override
	public Enumeration<String> getKeys() {
        return new ResourceBundleEnumeration( this.parent, this.bundles);
	}

};
