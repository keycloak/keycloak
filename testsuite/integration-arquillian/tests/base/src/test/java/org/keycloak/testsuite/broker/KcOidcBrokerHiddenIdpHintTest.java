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

import java.util.Map;

import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.Assert;

import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * Migrated from old testsuite.  Previous version by Pedro Igor.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 * @author pedroigor
 */
public class KcOidcBrokerHiddenIdpHintTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcHiddenBrokerConfiguration();
    }

    private class KcOidcHiddenBrokerConfiguration extends KcOidcBrokerConfiguration {

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            idp.setHideOnLogin(true);
            return idp;
        }
    }

    @Test
    public void testSuccessfulRedirectToProviderHiddenOnLoginPage() {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        waitForPage(driver, "sign in to", true);
        String url = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias();
        driver.navigate().to(url);
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // authenticated and redirected to app
        Assert.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }

}
