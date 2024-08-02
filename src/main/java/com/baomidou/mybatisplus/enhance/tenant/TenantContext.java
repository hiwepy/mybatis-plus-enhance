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
package com.baomidou.mybatisplus.enhance.tenant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantContext {

	private static final String KEY_CURRENT_TENANT_ID = "KEY_CURRENT_PROVIDER_ID";
	private static final Map<String, Object> M_CONTEXT = new ConcurrentHashMap<>();

	public void setCurrentTenantId(Long tenantId) {
		M_CONTEXT.put(KEY_CURRENT_TENANT_ID, tenantId);
	}

	public Long getCurrentTenantId() {
		return (Long) M_CONTEXT.get(KEY_CURRENT_TENANT_ID);
	}

}
