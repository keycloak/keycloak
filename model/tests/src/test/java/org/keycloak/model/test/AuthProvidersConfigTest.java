package org.keycloak.model.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.spi.authentication.AuthProviderConstants;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthProvidersConfigTest extends AbstractModelTest {

    @Test
    public void testConfiguration() {
        // Create realm and add some providers and ldap config. Then commit
        RealmModel realm = realmManager.createRealm("test");

        Map<String, String> ldapConfig = new HashMap<String,String>();
        ldapConfig.put("connectionUrl", "ldap://localhost:10389");
        ldapConfig.put("baseDn", "dc=keycloak,dc=org");
        realm.setLdapServerConfig(ldapConfig);

        AuthenticationProviderModel ap1 = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_MODEL, true, Collections.EMPTY_MAP);
        AuthenticationProviderModel ap2 = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_EXTERNAL_MODEL, true, Collections.EMPTY_MAP);
        AuthenticationProviderModel ap3 = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_PICKETLINK, true, Collections.EMPTY_MAP);

        List<AuthenticationProviderModel> authProviders = new ArrayList<AuthenticationProviderModel>();
        authProviders.add(ap1);
        authProviders.add(ap2);
        authProviders.add(ap3);
        realm.setAuthenticationProviders(authProviders);

        commit();

        // Assert ldap config are same
        RealmModel persisted = realmManager.getRealm(realm.getId());
        Assert.assertEquals(persisted.getLdapServerConfig(), ldapConfig);

        // Assert providers are same and in same order
        List<AuthenticationProviderModel> persProviders = persisted.getAuthenticationProviders();
        Assert.assertEquals(persProviders.size(), 3);
        assertProviderEquals(persProviders.get(0), ap1);
        assertProviderEquals(persProviders.get(1), ap2);
        assertProviderEquals(persProviders.get(2), ap3);

        // Update providers
        authProviders = new ArrayList<AuthenticationProviderModel>();
        authProviders.add(ap3);
        authProviders.add(ap2);
        authProviders.add(ap1);
        persisted.setAuthenticationProviders(authProviders);

        commit();

        // Assert providers are same and in same order
        persisted = realmManager.getRealm(realm.getId());
        persProviders = persisted.getAuthenticationProviders();
        Assert.assertEquals(persProviders.size(), 3);
        assertProviderEquals(persProviders.get(0), ap3);
        assertProviderEquals(persProviders.get(1), ap2);
        assertProviderEquals(persProviders.get(2), ap1);
    }

    private void assertProviderEquals(AuthenticationProviderModel prov1, AuthenticationProviderModel prov2) {
        Assert.assertEquals(prov1.getProviderName(), prov2.getProviderName());
        Assert.assertEquals(prov1.isPasswordUpdateSupported(), prov2.isPasswordUpdateSupported());
        Assert.assertEquals(prov1.getConfig(), prov2.getConfig());
    }
}
