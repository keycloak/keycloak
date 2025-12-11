/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.kerberos;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.KerberosEmbeddedServer;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.util.TestAppHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KerberosLdapCrossRealmTrustTest extends AbstractKerberosTest {

    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-ldap-crt-connection.properties";

    @ClassRule
    public static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);

    @ClassRule
    public static KerberosRule kerberosRule2 = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM_2);


    @Override
    protected KerberosRule getKerberosRule() {
        return kerberosRule;
    }


    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new LDAPProviderKerberosConfig(getUserStorageConfiguration());
    }

    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-ldap", LDAPStorageProviderFactory.PROVIDER_NAME);
    }


    @Test
    public void test01SpnegoLoginCRTSuccess() throws Exception {
        // Login as user from realm KC2.COM . Realm KEYCLOAK.ORG will trust us
        AccessTokenResponse tokenResponse = assertSuccessfulSpnegoLogin("hnelson2@KC2.COM", "hnelson2", "secret");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());

        Assert.assertEquals(token.getEmail(), "hnelson2@kc2.com");
        assertUser("hnelson2", "hnelson2@kc2.com", "Horatio", "Nelson", "hnelson2@KC2.COM", false);

        // Logout
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
        events.poll();
    }


    // Issue 20045
    @Test
    public void test02SpnegoLoginCorrectKerberosPrincipalUserFound() throws Exception {
        // Login as kerberos user jduke@KC2.COM. Ensure I am logged as user "jduke2" from realm KC2.COM (not as user jduke@KEYCLOAK.ORG)
        AccessTokenResponse tokenResponse = assertSuccessfulSpnegoLogin("jduke@KC2.COM", "jduke2", "theduke2");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());

        Assert.assertEquals(token.getEmail(), "jduke2@kc2.com");
        assertUser("jduke2", "jduke2@kc2.com", "Java", "Duke", "jduke@KC2.COM", false);

        // Logout
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
        events.poll();

        // Another login to check the scenario when user is in local storage
        tokenResponse = assertSuccessfulSpnegoLogin("jduke@KC2.COM", "jduke2", "theduke2");
        token = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertEquals(token.getEmail(), "jduke2@kc2.com");

        // Logout
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
        events.poll();
    }

    // Issue 20045 - username/password form login
    @Test
    public void test03SpnegoLoginUsernamePassword() throws Exception {
        // User jduke@KC2.COM
        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);
        Assert.assertFalse(testAppHelper.login("jduke2", "theduke"));
        Assert.assertTrue(testAppHelper.login("jduke2", "theduke2"));
        Assert.assertTrue(testAppHelper.logout());

        // User jduke@KEYCLOAK.ORG
        Assert.assertTrue(testAppHelper.login("jduke", "theduke"));

        // Logout
        testAppHelper.logout();
        events.poll();
    }

    // Test with "Kerberos Principal attribute name" set to empty value (backwards compatibility).
    @Test
    public void test04SpnegoLoginWithoutKerberosPrincipalAttrConfigured() throws Exception {
        updateUserStorageProvider(kerberosProvider -> kerberosProvider.getConfig().putSingle(KerberosConstants.KERBEROS_PRINCIPAL_ATTRIBUTE, null));

        // Keycloak will lookup user just based on 1st part of kerberos principal. Hence for "jduke@KC2.COM", it will lookup user "jduke"
        AccessTokenResponse tokenResponse = assertSuccessfulSpnegoLogin("jduke@KC2.COM", "jduke", "theduke2");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());

        Assert.assertEquals(token.getEmail(), "jduke@keycloak.org");
        assertUser("jduke", "jduke@keycloak.org", "Java", "Duke", null, false);

        // Logout
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
        events.poll();

        // This refers to same user as above login
        tokenResponse = assertSuccessfulSpnegoLogin("jduke@KEYCLOAK.ORG", "jduke", "theduke");
        token = oauth.verifyToken(tokenResponse.getAccessToken());

        Assert.assertEquals(token.getEmail(), "jduke@keycloak.org");

        // Logout
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
        events.poll();
    }

    @Test
    public void test05DisableTrust() throws Exception {
        // Remove the LDAP entry corresponding to the Kerberos principal krbtgt/KEYCLOAK.ORG@KC2.COM
        // This will effectively disable kerberos cross-realm trust
        testingClient.testing().ldap("test").removeLDAPUser("krbtgt2");


        // There is no trust among kerberos realms anymore. SPNEGO shouldn't work. There would be failure even on Apache HTTP client side
        // as it's not possible to start GSS context ( initSecContext ) due the missing trust among realms.
        try {
            Response spnegoResponse = spnegoLogin("hnelson2@KC2.COM", "secret");
            Assert.fail("Not expected to successfully login");
        } catch (Exception e) {
            // Expected
        }
    }


}
