/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.broker;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UIUtils;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for scenarios where "Identity Provider Authenticator" is set with "default identity provider" directly redirecting to provider realm
 */
public abstract class AbstractDefaultIdpTest extends AbstractInitializedBaseBrokerTest {

    @Override
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        // Require broker to show consent screen
        RealmResource brokeredRealm = adminClient.realm(bc.providerRealmName());
        List<ClientRepresentation> clients = brokeredRealm.clients().findByClientId(bc.getIDPClientIdInProviderRealm());
        org.junit.Assert.assertEquals(1, clients.size());
        ClientRepresentation brokerApp = clients.get(0);
        brokerApp.setConsentRequired(true);
        brokeredRealm.clients().get(brokerApp.getId()).update(brokerApp);
    }

    @Test
    public void testDefaultIdpNotSet() {
        // Set the Default Identity Provider option for the Identity Provider Redirector to null
        configureFlow(null);

        // Navigate to the auth page
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        waitForPage(driver, "sign in to", true);

        Assert.assertTrue("Driver should be on the initial page and nothing should have happened",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }

    @Test
    public void testDefaultIdpSet() {
        // Set the Default Identity Provider option to the remote IdP name
        configureFlow(getBrokerConfiguration().getIDPAlias());

        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        // Navigate to the auth page
        oauth.clientId("broker-app");
        loginPage.open(bc.providerRealmName());

        waitForPage(driver, "sign in to", true);

        // Make sure we got redirected to the remote IdP automatically
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
    }

    protected void testDefaultIdpSetTriedAndReturnedError(String expectedErrorMessageOnLoginScreen) {
        // Set the Default Identity Provider option to the remote IdP name
        configureFlow(getBrokerConfiguration().getIDPAlias());

        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        // Navigate to the auth page
        oauth.clientId("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        waitForPage(driver, "sign in to", true);

        // Make sure we got redirected to the remote IdP automatically
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        // Attempt login
        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // Deny user consent
        grantPage.assertCurrent();
        grantPage.cancel();

        waitForPage(driver, "sign in to", true);

        WebElement errorElement;
        try {
            errorElement = driver.findElement(By.className("pf-v5-c-alert"));
        } catch (NoSuchElementException e) {
            errorElement = driver.findElement(By.className("alert-error"));
        }

        assertNotNull("Page should show an error message but it's missing", errorElement);

        // Login to IDP failed due consent denied. Error message is displayed on the username/password screen of the consumer realm
        assertEquals(expectedErrorMessageOnLoginScreen, UIUtils.getTextFromElement(errorElement));
    }

    protected void testLoginHintForwarded() {
        // Set the Default Identity Provider option to the remote IdP name
        configureFlow(getBrokerConfiguration().getIDPAlias());

        String username = "all-info-set@localhost.com";
        String urlEncodedUsername = "all-info-set%40localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        // Navigate to the auth page of consumer realm
        oauth.realm(bc.consumerRealmName()).client("broker-app").loginForm().loginHint(username).open();

        waitForPage(driver, "sign in to", true);

        // Make sure we got redirected to the remote IdP (provider) automatically
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        Assert.assertTrue("Provider page should contain login_hint parameter",
                driver.getCurrentUrl().contains("login_hint=" + urlEncodedUsername));
    }

    protected void configureFlow(String defaultIdpValue) {
        String newFlowAlias;

        HashMap<String, String> defaultIdpConfig = new HashMap<String, String>();
        if (defaultIdpValue != null && !defaultIdpValue.isEmpty()) {
            defaultIdpConfig.put(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER, defaultIdpValue);
            newFlowAlias = "Browser - Default IdP " + defaultIdpValue;
        }
        else
            newFlowAlias = "Browser - Default IdP OFF";

        testingClient.server("consumer").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("consumer").run(session ->
                {
                    List<AuthenticationExecutionModel> executions = FlowUtil.inCurrentRealm(session)
                            .selectFlow(newFlowAlias)
                            .getExecutions();

                    int index = IntStream.range(0, executions.size())
                            .filter(t -> IdentityProviderAuthenticatorFactory.PROVIDER_ID.equals(executions.get(t).getAuthenticator()))
                            .findFirst()
                            .orElse(-1);

                    assertTrue("Identity Provider Redirector execution not found", index >= 0);

                    FlowUtil.inCurrentRealm(session)
                            .selectFlow(newFlowAlias)
                            .updateExecution(index,
                                    config -> {
                                        AuthenticatorConfigModel authConfig = new AuthenticatorConfigModel();
                                        authConfig.setId(UUID.randomUUID().toString());
                                        authConfig.setAlias("cfg" + authConfig.getId().hashCode());
                                        authConfig.setConfig(defaultIdpConfig);

                                        session.getContext().getRealm().addAuthenticatorConfig(authConfig);

                                        config.setAuthenticatorConfig(authConfig.getId());
                                    }
                            )
                            .defineAsBrowserFlow();
                }
        );
    }
}
