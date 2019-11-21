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

import org.hamcrest.Matchers;
import org.infinispan.Cache;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.InfinispanStickySessionEncoderProviderFactory;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sessions.StickySessionEncoderProvider;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;

import javax.ws.rs.core.UriBuilder;
import java.util.HashSet;
import java.util.Set;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

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
    public void testAuthSessionCookieWithAttachedRoute() throws Exception {
        // TODO Maybe add compatibility between cluster and cross-dc tests regarding route name (jboss.node.name). Cross-dc tests use arquillian container qualifier when cluster tests just 'node1' .
//        String node1Route = backendNode(0).getArquillianContainer().getName();
//        String node2Route = backendNode(1).getArquillianContainer().getName();

        String accountServiceNode1URL = RealmsResource.accountUrl(UriBuilder.fromUri(backendNode(0).getUriBuilder().build() + "/auth")).build("test").toString();
        String accountServiceNode2URL = RealmsResource.accountUrl(UriBuilder.fromUri(backendNode(1).getUriBuilder().build() + "/auth")).build("test").toString();

        Set<String> visitedRoutes = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            driver.navigate().to(accountServiceNode1URL);
            String authSessionCookie = AuthenticationSessionFailoverClusterTest.getAuthSessionCookieValue(driver);

            Assert.assertThat(authSessionCookie.length(), Matchers.greaterThan(36));
            String route = authSessionCookie.substring(37);
            visitedRoutes.add(route);

            // Drop all cookies before continue
            driver.manage().deleteAllCookies();
        }

        Assert.assertThat(visitedRoutes, Matchers.containsInAnyOrder("node1", "node2"));
    }


    @Test
    public void testAuthSessionCookieWithoutRoute() throws Exception {
        String accountServiceNode1URL = RealmsResource.accountUrl(UriBuilder.fromUri(backendNode(0).getUriBuilder().build() + "/auth")).build("test").toString();
        String accountServiceNode2URL = RealmsResource.accountUrl(UriBuilder.fromUri(backendNode(1).getUriBuilder().build() + "/auth")).build("test").toString();

        // Disable route on backend server
        getTestingClientFor(backendNode(0)).server().run(session -> {
            InfinispanStickySessionEncoderProviderFactory factory = (InfinispanStickySessionEncoderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(StickySessionEncoderProvider.class, "infinispan");
            factory.setShouldAttachRoute(false);
        });

        // Test routes
        for (int i = 0; i < 20; i++) {
            driver.navigate().to(accountServiceNode1URL);
            String authSessionCookie = AuthenticationSessionFailoverClusterTest.getAuthSessionCookieValue(driver);

            Assert.assertEquals(36, authSessionCookie.length());

            // Drop all cookies before continue
            driver.manage().deleteAllCookies();

            // Check that route owner is always node1
            getTestingClientFor(backendNode(0)).server().run(session -> {
                Cache authSessionCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME);
                String keyOwner = InfinispanUtil.getTopologyInfo(session).getRouteName(authSessionCache, authSessionCookie);
                Assert.assertEquals("node1", keyOwner);
            });
        }

        // Revert route on backend server
        getTestingClientFor(backendNode(0)).server().run(session -> {
            InfinispanStickySessionEncoderProviderFactory factory = (InfinispanStickySessionEncoderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(StickySessionEncoderProvider.class, "infinispan");
            factory.setShouldAttachRoute(true);
        });
    }
}
