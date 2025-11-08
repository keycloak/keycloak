/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.BrowserTabUtil;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcSamlMultipleTabsBrokerTest extends AbstractInitializedBaseBrokerTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    private String providerRealmId;
    private String consumerRealmId;

    // Similar to MultipleTabsLoginTest.multipleTabsParallelLogin but with IDP brokering test involved
    @Test
    public void testAuthenticationExpiredWithMoreBrowserTabs_loginExpiredInBothConsumerAndProvider() {
        assumeTrue("Since the JS engine in real browser does check the expiration regularly in all tabs, this test only works with HtmlUnit", driver instanceof HtmlUnitDriver);
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            // Open login page in tab1 and click "login with IDP"
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());
            loginPage.clickSocial(bc.getIDPAlias());

            // Open login page in tab 2
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
            Assert.assertTrue(loginPage.isCurrent("consumer"));
            getLogger().infof("URL in tab2: %s", driver.getCurrentUrl());

            setTimeOffset(7200000);

            // Finish login in tab2
            loginPage.clickSocial(bc.getIDPAlias());
            Assert.assertEquals(loginPage.getError(), "Your login attempt timed out. Login will start from the beginning.");
            logInWithBroker(bc);

            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.assertCurrent();
            Assert.assertTrue("We must be on consumer realm right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
            appPage.assertCurrent();
            events.clear();

            // Login in provider realm will redirect back to consumer with "authentication_expired" error. That one cannot redirect due the "clientData" missing in IdentityBrokerState for SAML brokers.
            // Hence need to display "You are already logged in" here
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));
            loginPage.login(bc.getUserLogin(), bc.getUserPassword());

            events.expectLogin().error(Errors.ALREADY_LOGGED_IN)
                    .realm(getProviderRealmId())
                    .client(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + bc.consumerRealmName())
                    .user((String) null)
                    .session((String) null)
                    .removeDetail(Details.CONSENT)
                    .removeDetail(Details.CODE_ID)
                    .detail(Details.REDIRECT_URI, Matchers.equalTo(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + bc.consumerRealmName() + "/broker/" + bc.getIDPAlias() + "/endpoint"))
                    .detail(Details.REDIRECTED_TO_CLIENT, "true")
                    .assertEvent();

            // Event for "already logged-in" in the consumer realm
            events.expect(EventType.IDENTITY_PROVIDER_LOGIN).error(Errors.ALREADY_LOGGED_IN)
                    .realm(getConsumerRealmId())
                    .client("broker-app")
                    .user((String) null)
                    .session((String) null)
                    .removeDetail(Details.REDIRECT_URI)
                    .detail(Details.REDIRECTED_TO_CLIENT, "false")
                    .assertEvent();

            // Being on "You are already logged-in" now. No way to redirect to client due "clientData" are null in RelayState of SAML IDP
            loginPage.assertCurrent("consumer");
            Assert.assertEquals("You are already logged in.", loginPage.getInstruction());
        }
    }

    @Test
    public void testAuthenticationExpiredWithMoreBrowserTabs_loginExpiredInProvider() throws Exception {
        assumeTrue("Since the JS engine in real browser does check the expiration regularly in all tabs, this test only works with HtmlUnit", driver instanceof HtmlUnitDriver);
        // Testing the scenario when authenticationSession expired only in "provider" realm and "consumer" is able to handle it  at IDP.
        // So need to increase authSession timeout on "consumer"
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver);
             AutoCloseable realmUpdater = new RealmAttributeUpdater(adminClient.realm(bc.consumerRealmName()))
                     .setAccessCodeLifespanLogin(7200)
                     .update()
        ) {
            // Open login page in tab1 and click "login with IDP"
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());
            loginPage.clickSocial(bc.getIDPAlias());

            // Open login page in tab 2
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
            Assert.assertTrue(loginPage.isCurrent("consumer"));
            getLogger().infof("URL in tab2: %s", driver.getCurrentUrl());

            setTimeOffset(3600);

            // Finish login in tab2
            logInWithBroker(bc);

            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.assertCurrent();
            Assert.assertTrue("We must be on consumer realm right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
            appPage.assertCurrent();
            events.clear();

            // Login in provider realm will redirect back to consumer with "authentication_expired" error. That one will redirect back to IDP (provider) as authenticationSession still exists on "consumer"
            // Then automatic SSO login on provider and then being redirected right away to consumer and finally to client
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));
            loginPage.login(bc.getUserLogin(), bc.getUserPassword());

            // Event 1: Already-logged-in on provider
            events.expectLogin().error(Errors.ALREADY_LOGGED_IN)
                    .realm(getProviderRealmId())
                    .client(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + bc.consumerRealmName())
                    .user((String) null)
                    .session((String) null)
                    .removeDetail(Details.CONSENT)
                    .removeDetail(Details.CODE_ID)
                    .detail(Details.REDIRECT_URI, Matchers.equalTo(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + bc.consumerRealmName() + "/broker/" + bc.getIDPAlias() + "/endpoint"))
                    .detail(Details.REDIRECTED_TO_CLIENT, "true")
                    .assertEvent();

            // Event 2: Consumer redirecting to "provider" IDP for retry login
            events.expect(EventType.IDENTITY_PROVIDER_LOGIN)
                    .realm(getConsumerRealmId())
                    .client("broker-app")
                    .user((String) null)
                    .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                    .detail(Details.LOGIN_RETRY, "true")
                    .assertEvent();

            // Event 3: Successful SSO login on "provider", which then redirects back to "consumer"
            events.expectLogin()
                    .realm(getProviderRealmId())
                    .client(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + bc.consumerRealmName())
                    .user(AssertEvents.isUUID())
                    .detail(Details.REDIRECT_URI, Matchers.equalTo(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + bc.consumerRealmName() + "/broker/" + bc.getIDPAlias() + "/endpoint"))
                    .assertEvent();

            // Event 4: Successful login on "consumer"
            events.expectLogin()
                    .realm(getConsumerRealmId())
                    .client("broker-app")
                    .user(AssertEvents.isUUID())
                    .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                    .assertEvent();

            // Authentication session on "consumer" realm is still valid, so no error here.
            appPage.assertCurrent();
            AuthorizationEndpointResponse authzResponse = oauth.parseLoginResponse();
            org.keycloak.testsuite.Assert.assertNotNull(authzResponse.getCode());
            org.keycloak.testsuite.Assert.assertNull(authzResponse.getError());
        }
    }

    private String getProviderRealmId() {
        if (providerRealmId != null) return providerRealmId;
        providerRealmId = adminClient.realm(bc.providerRealmName()).toRepresentation().getId();
        return providerRealmId;
    }

    private String getConsumerRealmId() {
        if (consumerRealmId != null) return consumerRealmId;
        consumerRealmId = adminClient.realm(bc.consumerRealmName()).toRepresentation().getId();
        return consumerRealmId;
    }
    
}
