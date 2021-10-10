/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;
import java.util.List;


import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.federation.kerberos.KerberosFederationProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.KerberosEmbeddedServer;

/**
 * Test for the KerberosFederationProvider (kerberos without LDAP integration)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosStandaloneTest extends AbstractKerberosSingleRealmTest {

    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-standalone-connection.properties";

    @ClassRule
    public static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);


    @Override
    protected KerberosRule getKerberosRule() {
        return kerberosRule;
    }


    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new KerberosConfig(getUserStorageConfiguration());
    }

    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-standalone", KerberosFederationProviderFactory.PROVIDER_NAME);
    }


    @Test
    public void spnegoLoginTest() throws Exception {
        assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");

        // Assert user was imported and hasn't any required action on him. Profile info is NOT synced from LDAP. Just username is filled and email is "guessed"
        assertUser("hnelson", "hnelson@" + kerberosRule.getConfig().get(KerberosConstants.KERBEROS_REALM).toLowerCase(), null, null, false);
    }


    @Test
    public void updateProfileEnabledTest() throws Exception {
        // Switch updateProfileOnFirstLogin to on
        List<ComponentRepresentation> reps = testRealmResource().components().query("test", UserStorageProvider.class.getName());
        org.keycloak.testsuite.Assert.assertEquals(1, reps.size());
        ComponentRepresentation kerberosProvider = reps.get(0);
        kerberosProvider.getConfig().putSingle(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "true");
        testRealmResource().components().component(kerberosProvider.getId()).update(kerberosProvider);

        // Assert update profile page is displayed
        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        Assert.assertEquals(200, spnegoResponse.getStatus());
        String responseText = spnegoResponse.readEntity(String.class);
        Assert.assertTrue(responseText.contains("You need to update your user profile to activate your account."));
        Assert.assertTrue(responseText.contains("hnelson@" + kerberosRule.getConfig().get(KerberosConstants.KERBEROS_REALM).toLowerCase()));
        spnegoResponse.close();

        // Assert user was imported and has required action on him
        assertUser("hnelson", "hnelson@" + kerberosRule.getConfig().get(KerberosConstants.KERBEROS_REALM).toLowerCase(), null, null, true);

        // Switch updateProfileOnFirstLogin to off
        kerberosProvider.getConfig().putSingle(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "false");
        testRealmResource().components().component(kerberosProvider.getId()).update(kerberosProvider);
    }


    /**
     * KEYCLOAK-3451
     *
     * Test that if there is no User Storage Provider that can handle kerberos we can still login
     *
     * @throws Exception
     */
    @Test
    public void noProvider() throws Exception {
        List<ComponentRepresentation> reps = testRealmResource().components().query("test", UserStorageProvider.class.getName());
        org.keycloak.testsuite.Assert.assertEquals(1, reps.size());
        ComponentRepresentation kerberosProvider = reps.get(0);
        testRealmResource().components().component(kerberosProvider.getId()).remove();

        /*
         To do this we do a valid kerberos login.  The authenticator will obtain a valid token, but there will
         be no user storage provider that can process it.  This means we should be on the login page.
         We do this through a JAX-RS client request.  We extract the action URL from the login page, and stuff it
         into selenium then just perform a regular login.
         */
        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        String context = spnegoResponse.readEntity(String.class);
        spnegoResponse.close();

        Assert.assertTrue(context.contains("Sign in to test"));

        String url = ActionURIUtils.getActionURIFromPageSource(context);


        // Follow login with HttpClient. Improve if needed
        MultivaluedMap<String, String> params = new javax.ws.rs.core.MultivaluedHashMap<>();
        params.putSingle("username", "test-user@localhost");
        params.putSingle("password", "password");
        Response response = client.target(url).request()
                .post(Entity.form(params));

        URI redirectUri = response.getLocation();
        assertAuthenticationSuccess(redirectUri.toString());

        events.clear();
        testRealmResource().components().add(kerberosProvider);
    }


    /**
     * KEYCLOAK-4178
     *
     * Assert it's handled when kerberos realm is unreachable
     *
     * @throws Exception
     */
    @Test
    @UncaughtServerErrorExpected
    public void handleUnknownKerberosRealm() throws Exception {
        // Switch kerberos realm to "unavailable"
        List<ComponentRepresentation> reps = testRealmResource().components().query("test", UserStorageProvider.class.getName());
        org.keycloak.testsuite.Assert.assertEquals(1, reps.size());
        ComponentRepresentation kerberosProvider = reps.get(0);
        kerberosProvider.getConfig().putSingle(KerberosConstants.KERBEROS_REALM, "unavailable");
        testRealmResource().components().component(kerberosProvider.getId()).update(kerberosProvider);

        // Try register new user and assert it failed
        UserRepresentation john = new UserRepresentation();
        john.setUsername("john");
        Response response = testRealmResource().users().create(john);
        Assert.assertEquals(500, response.getStatus());
        response.close();
    }

}
