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

import org.junit.Test;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import org.keycloak.testsuite.Assert;

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
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        waitForPage(driver, "log in to", true);
        String url = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias();
        driver.navigate().to(url);
        waitForPage(driver, "log in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        
        // authenticated and redirected to app
        Assert.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }
    
    // KEYCLOAK-5260
    @Test
    public void testSuccessfulRedirectToProviderAfterLoginPageShown() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        waitForPage(driver, "log in to", true);
        
        String urlWithHint = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias();        
        driver.navigate().to(urlWithHint);
        waitForPage(driver, "log in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        
        // do the same thing a second time
        driver.navigate().to(urlWithHint);
        waitForPage(driver, "log in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        
        // redirect shouldn't happen
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        waitForPage(driver, "log in to", true);
        Assert.assertTrue("Driver should be on the consumer realm page",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }
    
        @Test
    public void testInvalidIdentityProviderHint() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        waitForPage(driver, "log in to", true);
        String url = driver.getCurrentUrl() + "&kc_idp_hint=bogus-idp";
        driver.navigate().to(url);
        waitForPage(driver, "log in to", true);
        
        // Still on consumer login page
        Assert.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }
    
}