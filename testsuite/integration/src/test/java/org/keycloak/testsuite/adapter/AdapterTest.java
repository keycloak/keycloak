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
package org.keycloak.testsuite.adapter;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.Encode;
import org.keycloak.common.util.KeycloakUriBuilder;
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

    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            AdapterTestStrategy.baseAdapterTestInitialization(session, manager, adminRealm, getClass());

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
           
            url = getClass().getResource("/adapter-test/product-autodetect-bearer-only-keycloak.json");
            createApplicationDeployment()
                    .name("product-portal-autodetect-bearer-only").contextPath("/product-portal-autodetect-bearer-only")
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

    //@Test
    public void testUi() throws Exception {
        Thread.sleep(1000000000);
    }

    @Test
    public void testLoginSSOAndLogout() throws Exception {
        testStrategy.testLoginSSOMax();

        testStrategy.testLoginSSOAndLogout();
    }

    @Test
    public void testLoginEncodedRedirectUri() throws Exception {
        testStrategy.testLoginEncodedRedirectUri();
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

    @Test
    public void testAutodetectBearerOnly() throws Exception {
        testStrategy.testAutodetectBearerOnly();
    }

    @Test
    public void testBasicAuthErrorHandling() throws Exception {
        testStrategy.testBasicAuthErrorHandling();
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

    /**
     * KEYCLOAK-1733
     */
    @Test
    public void testNullQueryParameterAccessToken() throws Exception {
        testStrategy.testNullQueryParameterAccessToken();
    }

    @Test
    public void testRestCallWithAccessTokenAsQueryParameter() throws Exception {
        testStrategy.testRestCallWithAccessTokenAsQueryParameter();

    }
}
