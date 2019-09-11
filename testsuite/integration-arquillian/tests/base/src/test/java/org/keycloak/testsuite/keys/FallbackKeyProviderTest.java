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

package org.keycloak.testsuite.keys;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.crypto.Algorithm;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FallbackKeyProviderTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void fallbackAfterDeletingAllKeysInRealm() {
        String realmId = realmsResouce().realm("test").toRepresentation().getId();

        List<ComponentRepresentation> providers = realmsResouce().realm("test").components().query(realmId, "org.keycloak.keys.KeyProvider");
        assertEquals(3, providers.size());

        for (ComponentRepresentation p : providers) {
            realmsResouce().realm("test").components().component(p.getId()).remove();
        }

        providers = realmsResouce().realm("test").components().query(realmId, "org.keycloak.keys.KeyProvider");
        assertEquals(0, providers.size());

        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertNotNull(response.getAccessToken());

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        providers = realmsResouce().realm("test").components().query(realmId, "org.keycloak.keys.KeyProvider");
        assertProviders(providers, "fallback-RS256", "fallback-HS256");
    }

    @Test
    public void differentAlgorithms() {
        String realmId = realmsResouce().realm("test").toRepresentation().getId();

        String[] algorithmsToTest = new String[] {
                Algorithm.RS384,
                Algorithm.RS512,
                Algorithm.PS256,
                Algorithm.PS384,
                Algorithm.PS512,
                Algorithm.ES256,
                Algorithm.ES384,
                Algorithm.ES512
        };

        oauth.doLogin("test-user@localhost", "password");

        for (String algorithm : algorithmsToTest) {
            RealmRepresentation rep = realmsResouce().realm("test").toRepresentation();
            rep.setDefaultSignatureAlgorithm(algorithm);
            realmsResouce().realm("test").update(rep);

            oauth.openLoginForm();

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
            assertNotNull(response.getAccessToken());
        }

        List<ComponentRepresentation> providers = realmsResouce().realm("test").components().query(realmId, "org.keycloak.keys.KeyProvider");

        List<String> expected = new LinkedList<>();
        expected.add("rsa");
        expected.add("hmac-generated");
        expected.add("aes-generated");

        for (String a : algorithmsToTest) {
            expected.add("fallback-" + a);
        }

        assertProviders(providers, expected.toArray(new String[providers.size()]));
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    private void assertProviders(List<ComponentRepresentation> providers, String... expected) {
        List<String> names = new LinkedList<>();
        for (ComponentRepresentation p : providers) {
            names.add(p.getName());
        }

        assertThat(names, hasSize(expected.length));
        assertThat(names, containsInAnyOrder(expected));
    }
}

