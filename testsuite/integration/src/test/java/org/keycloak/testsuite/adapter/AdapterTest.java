/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.adapter;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

import java.net.URL;
import java.security.PublicKey;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class AdapterTest {

    public static PublicKey realmPublicKey;
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            RealmModel realm = AdapterTestStrategy.baseAdapterTestInitialization(session, manager, adminRealm, getClass());
            realmPublicKey = realm.getPublicKey();

            URL url = getClass().getResource("/adapter-test/cust-app-keycloak.json");
            createApplicationDeployment()
                    .name("customer-portal").contextPath("/customer-portal")
                    .servletClass(CustomerServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();

            url = getClass().getResource("/adapter-test/secure-portal-keycloak.json");
            createApplicationDeployment()
                    .name("secure-portal").contextPath("/secure-portal")
                    .servletClass(CallAuthenticatedServlet.class).adapterConfigPath(url.getPath())
                    .role("user")
                    .isConstrained(false).deployApplication();

            url = getClass().getResource("/adapter-test/customer-db-keycloak.json");
            createApplicationDeployment()
                    .name("customer-db").contextPath("/customer-db")
                    .servletClass(CustomerDatabaseServlet.class).adapterConfigPath(url.getPath())
                    .role("user")
                    .errorPage(null).deployApplication();

            createApplicationDeployment()
                    .name("customer-db-error-page").contextPath("/customer-db-error-page")
                    .servletClass(CustomerDatabaseServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();

            url = getClass().getResource("/adapter-test/product-keycloak.json");
            createApplicationDeployment()
                    .name("product-portal").contextPath("/product-portal")
                    .servletClass(ProductServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();

            // Test that replacing system properties works for adapters
            System.setProperty("app.server.base.url", "http://localhost:8081");
            System.setProperty("my.host.name", "localhost");
            url = getClass().getResource("/adapter-test/session-keycloak.json");
            createApplicationDeployment()
                    .name("session-portal").contextPath("/session-portal")
                    .servletClass(SessionServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();

            url = getClass().getResource("/adapter-test/input-keycloak.json");
            createApplicationDeployment()
                    .name("input-portal").contextPath("/input-portal")
                    .servletClass(InputServlet.class).adapterConfigPath(url.getPath())
                    .role("user").constraintUrl("/secured/*").deployApplication();
        }
    };

    @Rule
    public AdapterTestStrategy testStrategy = new AdapterTestStrategy("http://localhost:8081/auth", "http://localhost:8081", keycloakRule);

    @Test
    public void testLoginSSOAndLogout() throws Exception {
        testStrategy.testLoginSSOAndLogout();
    }

    @Test
    public void testSavedPostRequest() throws Exception {
        testStrategy.testSavedPostRequest();
    }

    @Test
    public void testServletRequestLogout() throws Exception {
        testStrategy.testServletRequestLogout();
    }

    @Test
    public void testLoginSSOIdle() throws Exception {
        testStrategy.testLoginSSOIdle();

    }

    @Test
    public void testLoginSSOIdleRemoveExpiredUserSessions() throws Exception {
        testStrategy.testLoginSSOIdleRemoveExpiredUserSessions();
    }

    @Test
    public void testLoginSSOMax() throws Exception {
        testStrategy.testLoginSSOMax();
    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    @Test
    public void testNullBearerToken() throws Exception {
        testStrategy.testNullBearerToken();
    }

    /**
     * KEYCLOAK-1368
     * @throws Exception
     */
    @Test
    public void testNullBearerTokenCustomErrorPage() throws Exception {
        testStrategy.testNullBearerTokenCustomErrorPage();
    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    @Test
    public void testBadUser() throws Exception {
        testStrategy.testBadUser();
    }

    @Test
    public void testVersion() throws Exception {
        testStrategy.testVersion();
    }

    @Test
    public void testAuthenticated() throws Exception {
        testStrategy.testAuthenticated();
    }

    /**
     * KEYCLOAK-732
     *
     * @throws Throwable
     */
    @Test
    public void testSingleSessionInvalidated() throws Throwable {
        testStrategy.testSingleSessionInvalidated();
    }

    /**
     * KEYCLOAK-741
     */
    @Test
    public void testSessionInvalidatedAfterFailedRefresh() throws Throwable {
        testStrategy.testSessionInvalidatedAfterFailedRefresh();

    }

    /**
     * KEYCLOAK-942
     */
    @Test
    public void testAdminApplicationLogout() throws Throwable {
        testStrategy.testAdminApplicationLogout();
    }

    /**
     * KEYCLOAK-1216
     */
    @Test
    public void testAccountManagementSessionsLogout() throws Throwable {
        testStrategy.testAccountManagementSessionsLogout();
    }

}
