/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.forms.account.freemarker.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.RealmModel;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class AccountRealmBeanTest {

	@Test
	public void exposeRealmAttributesTest() {
		Map<String, String> map = new HashMap<String, String>();
	    RealmModel realmModel = (RealmModel) Proxy.newProxyInstance(AccountRealmBeanTest.class.getClassLoader(), new Class[]{RealmModel.class}, (proxy, method, args) -> {

	        if (method.getName().matches("getAttributes")) {
	            return map;
	        }

	        return null;
	    });
	    RealmBean realmBean = new RealmBean(realmModel);

	    Assert.assertEquals(map,realmBean.getAttributes());
	}
}