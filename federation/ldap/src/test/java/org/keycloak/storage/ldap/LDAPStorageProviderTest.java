package org.keycloak.storage.ldap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;

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
