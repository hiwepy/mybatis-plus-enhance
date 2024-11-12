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
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

public class ResourceBundleEnumeration implements Enumeration<String> {

	private final Iterator<String> ite;

	public ResourceBundleEnumeration(ResourceBundle ...bundles){
		this(null, bundles);
	}

	public ResourceBundleEnumeration(ResourceBundle parent,ResourceBundle ...bundles) {
		Set<String> keys = new HashSet<String>();
		if(parent != null){
			keys.addAll(parent.keySet());
		}
		for (ResourceBundle bundle : bundles) {
			if(bundle == null){
				continue;
			}
			keys.addAll(bundle.keySet());
		}
		this.ite = keys.iterator();
	}

	@Override
	public boolean hasMoreElements() {
		return ite.hasNext();
	}

	@Override
	public String nextElement() {
		return ite.next();
	}

}
