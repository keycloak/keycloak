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

package org.keycloak.testsuite.cluster;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.StickySessionEncoderProvider;
import org.keycloak.sessions.StickySessionEncoderProviderFactory;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionClusterTest extends AbstractClusterTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;


    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Page
    protected AppPage appPage;


    @Before
    public void setup() {
        try {
            adminClient.realm("test").remove();
        } catch (Exception ignore) {
        }

        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm);
    }

    @After
    public void after() {
        adminClient.realm("test").remove();
    }


    @Test
    public void testAuthSessionCookieWithAttachedRoute() {
        OAuthClient oAuthClient = oauth;
        oAuthClient.baseUrl(UriBuilder.fromUri(backendNode(0).getUriBuilder().build() + "/auth").build("test").toString());

        String testAppLoginNode1URL = oAuthClient.loginForm().build();

        Set<String> visitedRoutes = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            driver.navigate().to(testAppLoginNode1URL);
            String authSessionCookie = AuthenticationSessionFailoverClusterTest.getAuthSessionCookieValue(driver);

            assertNotEquals( -1, authSessionCookie.indexOf("."));
            String route = authSessionCookie.substring(authSessionCookie.indexOf(".") + 1);
            visitedRoutes.add(route);

            getTestingClientFor(backendNode(0)).server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                session.getContext().setRealm(realm);
                StickySessionEncoderProvider provider = session.getProvider(StickySessionEncoderProvider.class);
                StickySessionEncoderProvider.SessionIdAndRoute sessionIdAndRoute = provider.decodeSessionIdAndRoute(authSessionCookie);
                assertThat(sessionIdAndRoute.route(), Matchers.startsWith("node1"));
                String decodedAuthSessionId = new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(sessionIdAndRoute.sessionId());
                assertTrue(sessionIdAndRoute.isSameRoute(provider.sessionIdRoute(decodedAuthSessionId)));
            });

            // Drop all cookies before continue
            driver.manage().deleteAllCookies();
        }

        assertThat(visitedRoutes, Matchers.contains(Matchers.startsWith("node1")));
    }


    @Test
    public void testAuthSessionCookieWithoutRoute() {
        OAuthClient oAuthClient = oauth;
        oAuthClient.baseUrl(UriBuilder.fromUri(backendNode(0).getUriBuilder().build() + "/auth").build("test").toString());

        String testAppLoginNode1URL = oAuthClient.loginForm().build();

        // Disable route on backend server
        getTestingClientFor(backendNode(0)).server().run(session -> {
            StickySessionEncoderProviderFactory factory = (StickySessionEncoderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(StickySessionEncoderProvider.class);
            factory.setShouldAttachRoute(false);
        });

        // Test routes
        for (int i = 0; i < 20; i++) {
            driver.navigate().to(testAppLoginNode1URL);
            String authSessionCookie = AuthenticationSessionFailoverClusterTest.getAuthSessionCookieValue(driver);

            assertEquals(-1, authSessionCookie.indexOf("."));

            // Drop all cookies before continue
            driver.manage().deleteAllCookies();

            // Check that route owner is always node1
            getTestingClientFor(backendNode(0)).server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                session.getContext().setRealm(realm);
                String decodedAuthSessionId = new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(authSessionCookie);
                assertNull(session.getProvider(StickySessionEncoderProvider.class).sessionIdRoute(decodedAuthSessionId));
            });
        }

        // Revert route on backend server
        getTestingClientFor(backendNode(0)).server().run(session -> {
            StickySessionEncoderProviderFactory factory = (StickySessionEncoderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(StickySessionEncoderProvider.class);
            factory.setShouldAttachRoute(true);
        });
    }
}
