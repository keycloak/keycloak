/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * Migrated from old testsuite.  Previous version by Pedro Igor.
 * 
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 * @author pedroigor
 */
public class KcOidcBrokerIdpHintTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Test
    public void testSuccessfulRedirect() {
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        waitForPage(driver, "sign in to", true);
        String url = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias();
        driver.navigate().to(url);
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        
        // authenticated and redirected to app
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }
    
    // KEYCLOAK-5260
    @Test
    public void testSuccessfulRedirectToProviderAfterLoginPageShown() {
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        waitForPage(driver, "sign in to", true);
        
        String urlWithHint = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias();        
        driver.navigate().to(urlWithHint);
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");
        
        // do the same thing a second time
        driver.navigate().to(urlWithHint);
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");
        
        // redirect shouldn't happen
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"),
                "Driver should be on the consumer realm page");
    }
    
        @Test
    public void testInvalidIdentityProviderHint() {
            oauth.client("broker-app");
            loginPage.open(bc.consumerRealmName());
        waitForPage(driver, "sign in to", true);
        String url = driver.getCurrentUrl() + "&kc_idp_hint=bogus-idp";
        driver.navigate().to(url);
        waitForPage(driver, "sign in to", true);

        // Still on consumer login page
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }

    @Test
    public void testIdpHintWithErrorResponseReturnsToLoginPage() {
        // Configure broker client to require consent so we can test error scenario
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        List<ClientRepresentation> clients = providerRealm.clients().findByClientId(bc.getIDPClientIdInProviderRealm());
        Assertions.assertEquals(1, clients.size());
        ClientRepresentation brokerClient = clients.get(0);
        brokerClient.setConsentRequired(true);
        providerRealm.clients().get(brokerClient.getId()).update(brokerClient);

        try {
            oauth.client("broker-app");
            loginPage.open(bc.consumerRealmName());
            waitForPage(driver, "sign in to", true);

            // Add kc_idp_hint parameter to redirect to IdP
            String url = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias();
            driver.navigate().to(url);

            // Should be redirected to provider realm
            waitForPage(driver, "sign in to", true);
            Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                    "Driver should be on the provider realm page right now");

            log.debug("Logging in");
            loginPage.login(bc.getUserLogin(), bc.getUserPassword());

            // Deny user consent on the grant page
            grantPage.assertCurrent();
            grantPage.cancel();

            // Should return to consumer login page (not infinite loop back to IdP)
            waitForPage(driver, "sign in to", true);
            Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"),
                    "Driver should be back on consumer login page after denial");

            Assertions.assertTrue(driver.getPageSource().contains("Access denied"),
                    "Error message should be displayed");
        } finally {
            // Restore consent setting
            brokerClient.setConsentRequired(false);
            providerRealm.clients().get(brokerClient.getId()).update(brokerClient);
        }
    }

}
