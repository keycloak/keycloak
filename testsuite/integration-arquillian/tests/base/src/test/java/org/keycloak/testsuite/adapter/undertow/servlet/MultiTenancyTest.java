/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.undertow.servlet;

import java.util.List;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.servlet.ErrorServlet;
import org.keycloak.testsuite.adapter.servlet.MultiTenantResolver;
import org.keycloak.testsuite.adapter.servlet.MultiTenantServlet;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_CONTAINER_DEFAULT;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import org.keycloak.testsuite.util.URLAssert;
import org.keycloak.testsuite.util.WaitUtils;

/**
 * note: migrated from old testsuite
 * 
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
@AppServerContainer(AUTH_SERVER_CONTAINER_DEFAULT)
public class MultiTenancyTest extends AbstractServletsAdapterTest {
    
    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/tenant1-realm.json"));
        testRealms.add(loadRealm("/adapter-test/tenant2-realm.json"));
    }
    
    @Override
    protected boolean isImportAfterEachMethod() {
        return false;
    }
    
    @Deployment(name = "multi-tenant")
    protected static WebArchive multiTenant() {
        return servletDeploymentMultiTenant("multi-tenant", MultiTenantServlet.class, ErrorServlet.class, MultiTenantResolver.class);
    }
    
    @After
    public void afterTest() {
        driver.manage().deleteAllCookies();
    }
    
    /**
     * Simplest scenario: one user, one realm. The user is not logged in at
     * any other realm
     */
    @Test
    public void testTenantsLoggingOut() {
        doTenantRequests("tenant1", true);
        doTenantRequests("tenant2", true);
    }
    
    /**
     * This tests the adapter's ability to deal with multiple sessions
     * from the same user, one for each realm. It should not mixup and return
     * a session from tenant1 to tenant2
     */
    @Test
    public void testTenantsWithoutLoggingOut() {
        doTenantRequests("tenant1", true);
        doTenantRequests("tenant2", true);

        doTenantRequests("tenant1", false);
        doTenantRequests("tenant2", true);
    }
    
    /**
     * This test simulates an user that is not logged in yet, and tries to login
     * into tenant1 using an account from tenant2.
     * On this scenario, the user should be shown the login page again.
     */
    @Test
    public void testUnauthorizedAccessNotLoggedIn() {
        String keycloakServerBaseUrl = authServerPage.toString();
        
        driver.navigate().to(authServerContextRootPage + "/multi-tenant/tenant1");
        WaitUtils.waitForPageToLoad();
        URLAssert.assertCurrentUrlStartsWith(keycloakServerBaseUrl);
        
        String currentUrl = driver.getCurrentUrl();
        String toString = testRealmLoginPage.toString();
        testRealmLoginPage.form().login("user-tenant2", "user-tenant2");
        URLAssert.assertCurrentUrlStartsWith(keycloakServerBaseUrl);
    }
    
    /**
     * This test simulates an user which is already logged in into tenant1
     * and tries to access a resource on tenant2.
     * On this scenario, the user should be shown the login page again.
     */
    @Test
    public void testUnauthorizedAccessLoggedIn() {
        doTenantRequests("tenant1", false);
        
        driver.navigate().to(authServerContextRootPage + "/multi-tenant/tenant2");
        URLAssert.assertCurrentUrlStartsWith(authServerPage.toString());
    }
    
    private void doTenantRequests(String tenant, boolean logout) {
        String tenantLoginUrl = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(authServerPage.getAuthRoot())).build(tenant).toString();
        String tenantUrl = authServerContextRootPage + "/multi-tenant/" + tenant;
        
        driver.navigate().to(tenantUrl);
        URLAssert.assertCurrentUrlStartsWith(tenantLoginUrl);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        log.debug("Current url: " + driver.getCurrentUrl());
        
        URLAssert.assertCurrentUrlStartsWith(tenantUrl);
        String pageSource = driver.getPageSource();
        log.debug(pageSource);
        
        Assert.assertTrue(pageSource.contains("Username: bburke@redhat.com"));
        Assert.assertTrue(pageSource.contains("Realm: " + tenant));

        if (logout) {
            driver.navigate().to(authServerContextRootPage + "/multi-tenant/" + tenant + "/logout");
            Assert.assertFalse(driver.getPageSource().contains("Username: bburke@redhat.com"));
            Assert.assertTrue(driver.getCurrentUrl().startsWith(tenantLoginUrl));
        }
        log.debug("---------------------------------------------------------------------------------------");
    }
}
