/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap;

import org.keycloak.component.ComponentModel;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LDAPStorageProviderTest {

	private LDAPStorageProvider provider;

	@Before
	public void setUp() {
		ComponentModel model = new ComponentModel();
		LDAPConfig config = new LDAPConfig(model.getConfig());
		LDAPIdentityStore identityStore = new LDAPIdentityStore(null, config);

		provider = new LDAPStorageProvider(null, null, model, identityStore);
	}

	@Test
	public void shouldResolvePlainUsername() {
		String[] result = provider.resolveUsernameLookup(
			"sAMAccountName",
			"alice");

		Assert.assertArrayEquals(
			new String[] { "sAMAccountName", "alice" },
			result);
	}

	@Test
	public void shouldResolveDerivedUsername() {
		String mapping = "{sAMAccountName}@example.com";

		Assert.assertArrayEquals(
			new String[] { "sAMAccountName", "alice" },
			provider.resolveUsernameLookup(
				mapping, "alice@example.com"));

		Assert.assertNull(
			provider.resolveUsernameLookup(
				mapping, "alice@other.com"));

		Assert.assertNull(
			provider.resolveUsernameLookup(
				mapping, "alice"));
	}
}
