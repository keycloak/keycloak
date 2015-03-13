/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.adapter;

import javax.ws.rs.core.UriBuilder;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testutils.KeycloakServer;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public class MultiTenancyTest {
    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected WebDriver driver;
    
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            RealmRepresentation tenant1 = KeycloakServer.loadJson(getClass().getResourceAsStream("/adapter-test/tenant1-realm.json"), RealmRepresentation.class);
            manager.importRealm(tenant1);

            RealmRepresentation tenant2 = KeycloakServer.loadJson(getClass().getResourceAsStream("/adapter-test/tenant2-realm.json"), RealmRepresentation.class);
            manager.importRealm(tenant2);

            deployApplication("multi-tenant", "/multi-tenant", MultiTenantServlet.class, null, "user", true, MultiTenantResolver.class);
        }

        protected String[] getTestRealms() {
            return new String[]{"test", "demo", "tenant1", "tenant2"};
        }
    };

    /**
     * Simplest scenario: one user, one realm. The user is not logged in at
     * any other realm
     * @throws Exception
     */
    @Test
    public void testTenantsLoggingOut() throws Exception {
        doTenantRequests("tenant1", true);
        doTenantRequests("tenant2", true);
    }

    /**
     * This tests the adapter's ability to deal with multiple sessions
     * from the same user, one for each realm. It should not mixup and return
     * a session from tenant1 to tenant2
     * @throws Exception
     */
    @Test
    public void testTenantsWithoutLoggingOut() throws Exception {
        doTenantRequests("tenant1", true);
        doTenantRequests("tenant2", true);

        doTenantRequests("tenant1", false);
        doTenantRequests("tenant2", true);
    }

    /**
     * This test simulates an user that is not logged in yet, and tris to login
     * into tenant1 using an account from tenant2.
     * On this scenario, the user should be shown the login page again.
     *
     * @throws Exception
     */
    @Test
    public void testUnauthorizedAccessNotLoggedIn() throws Exception {
        String keycloakServerBaseUrl = "http://localhost:8081/auth";

        driver.navigate().to("http://localhost:8081/multi-tenant?realm=tenant1");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(keycloakServerBaseUrl));

        loginPage.login("user-tenant2", "user-tenant2");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(keycloakServerBaseUrl));
    }

    /**
     * This test simulates an user which is already logged in into tenant1
     * and tries to access a resource on tenant2.
     * On this scenario, the user should be shown the login page again.
     * 
     * @throws Exception
     */
    @Test
    public void testUnauthorizedAccessLoggedIn() throws Exception {
        String keycloakServerBaseUrl = "http://localhost:8081/auth";
        doTenantRequests("tenant1", false);

        driver.navigate().to("http://localhost:8081/multi-tenant?realm=tenant2");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(keycloakServerBaseUrl));
    }

    private void doTenantRequests(String tenant, boolean logout) {
        String tenantLoginUrl = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri("http://localhost:8081/auth")).build(tenant).toString();

        driver.navigate().to("http://localhost:8081/multi-tenant?realm="+tenant);
        System.out.println("Current url: " + driver.getCurrentUrl());

        Assert.assertTrue(driver.getCurrentUrl().startsWith(tenantLoginUrl));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());

        Assert.assertEquals("http://localhost:8081/multi-tenant?realm="+tenant, driver.getCurrentUrl());
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);

        Assert.assertTrue(pageSource.contains("Username: bburke@redhat.com"));
        Assert.assertTrue(pageSource.contains("Realm: "+tenant));

        if (logout) {
            driver.manage().deleteAllCookies();
        }
    }
}
