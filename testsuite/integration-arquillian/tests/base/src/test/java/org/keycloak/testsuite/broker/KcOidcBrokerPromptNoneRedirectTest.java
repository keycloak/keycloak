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
import java.util.Map;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.UserBuilder;

import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configurePostBrokerLoginWithOTP;
import static org.keycloak.testsuite.broker.BrokerTestConstants.CLIENT_ID;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;

/**
 * This class tests the propagation of the {@code prompt=none} request parameter to a default IDP (if one has been specified)
 * if that IDP supports {@code prompt=none} redirects.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class KcOidcBrokerPromptNoneRedirectTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerPromptNoneConfiguration();
    }

    /**
     * Tests the successful forwarding of an auth request with {@code prompt=none} to a default identity provider.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testSuccessfulRedirectToProviderWithPromptNone() throws Exception {
        /* we need to disable profile update for the prompt=none propagation to work. */
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        /* let's start by authenticating directly in the IDP so the test user is already authenticated there. */
        authenticateDirectlyInIDP();

        /* now send an auth request to the consumer realm including both the kc_idp_hint (to identify the default provider) and prompt=none.
           The presence of the default provider should cause the request with prompt=none to be propagated to the idp instead of resulting
           in a login required error because the user is not yet authenticated in the consumer realm. */
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        waitForPage(driver, "sign in to", true);
        String url = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias() + "&prompt=none";
        driver.navigate().to(url);

        /* no need to log in again, the idp should have been able to identify that the user is already logged in and the authenticated user should
           have been established in the consumer realm. Lastly, user must be redirected to the account app as expected. */
        waitForAccountManagementTitle();
        Assert.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/account"));
        accountUpdateProfilePage.assertCurrent();

        /* let's try logging out from the consumer realm and then send an auth request with only prompt=none. The absence of a default idp
           should result in a login required error because the user is not authenticated in the consumer realm and the request won't be propagated
           all the way to the idp where the user is authenticated. */
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName(), bc.getIDPAlias());
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        waitForPage(driver, "sign in to", true);
        url = driver.getCurrentUrl() + "&prompt=none";
        driver.navigate().to(url);
        Assert.assertTrue(driver.getCurrentUrl().contains(bc.consumerRealmName() + "/account/login-redirect?error=login_required"));
    }

    /**
     * Tests that an auth request with {@code prompt=none} that is forwarded to the default IDP returns a {@code login_required}
     * error message if the user is not currently authenticated in neither the initiating realm nor the IDP.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testUnauthenticatedUserReturnsLoginRequired() throws Exception {
        /* try sending an auth request to the consumer realm with prompt=none. As we have no user authenticated in both
           the consumer realm and the IDP, the IDP should return an error=login_required to the broker and the broker must
           in turn return the same error to the client. */
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        waitForPage(driver, "sign in to", true);
        String url = driver.getCurrentUrl() + "&prompt=none&kc_idp_hint=" + bc.getIDPAlias();
        driver.navigate().to(url);
        Assert.assertTrue(driver.getCurrentUrl().contains(bc.consumerRealmName() + "/account/login-redirect?error=login_required"));
    }

    /**
     * Tests that an auth request with {@code prompt=none} that is forwarded to a default IDP returns a {@code interaction_required}
     * error message if the user is required to update the imported profile as part of the first broker login flow. Per spec,
     * when {@code prompt=none} is used the server must not display any authentication or consent user interface pages.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testUpdateProfileReturnsInteractionRequired() throws Exception {
        /* for this test we don't disable the update profile page - we are expecting an interaction_required error. */
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
        /* verify that the interaction_required error is returned with sending auth request to the consumer realm with prompt=none. */
        checkAuthWithPromptNoneReturnsInteractionRequired();
    }

    /**
     * Tests that an auth request with {@code prompt=none} that is forwarded to a default IDP returns a {@code interaction_required}
     * error message if the user is required to update his password (which happens via a required action that might also
     * be triggered by the first broker login flow). Per spec, when {@code prompt=none} is used the server must not display any
     * authentication or consent user interface pages.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testRequirePasswordUpdateReturnsInteractionRequired() throws Exception {
        /* disable the update profile but add a required action to update password after registration */
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        updateExecutions(AbstractBrokerTest::enableRequirePassword);
        /* verify that the interaction_required error is returned with sending auth request to the consumer realm with prompt=none. */
        checkAuthWithPromptNoneReturnsInteractionRequired();
    }

    /**
     * Tests that an auth request with {@code prompt=none} that is forwarded to a default IDP returns a {@code interaction_required}
     * error message if the user is prompted to link an existing account as part of the first broker login flow. Per spec,
     * when {@code prompt=none} is used the server must not display any authentication or consent user interface pages.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testLinkExistingAccountReturnsInteractionRequired() throws Exception {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        /* create user in the consumer realm with same e-mail as the user in the idp */
        UserRepresentation newUser = UserBuilder.create().username("consumer").email(USER_EMAIL).enabled(true).build();
        String userId = createUserWithAdminClient(adminClient.realm(bc.consumerRealmName()), newUser);
        resetUserPassword(adminClient.realm(bc.consumerRealmName()).users().get(userId), "password", false);
        /* verify that the interaction_required error is returned with sending auth request to the consumer realm with prompt=none. */
        checkAuthWithPromptNoneReturnsInteractionRequired();
    }

    /**
     * Tests that an auth request with {@code prompt=none} that is forwarded to a default IDP returns a {@code interaction_required}
     * error message if the user is further required to login with an OTP as part of the post broker login flow. Per spec,
     * when {@code prompt=none} is used the server must not display any authentication or consent user interface pages.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testPostBrokerLoginWithOTPReturnsInteractionRequired() throws Exception {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        /* setup the post broker login flow with OTP. */
        testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));
        /* verify that the interaction_required error is returned with sending auth request to the consumer realm with prompt=none. */
        checkAuthWithPromptNoneReturnsInteractionRequired();
    }

    /**
     * Tests that an auth request with {@code prompt=none} that is forwarded to a default IDP returns a {@code interaction_required}
     * error message if the IDP requires consent as part of the authentication process. Per spec, when {@code prompt=none} is used
     * the server must not display any authentication or consent user interface pages.
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testRequireConsentReturnsInteractionRequired() throws Exception {
        RealmResource brokeredRealm = adminClient.realm(bc.providerRealmName());
        List<ClientRepresentation> clients = brokeredRealm.clients().findByClientId(CLIENT_ID);
        org.junit.Assert.assertEquals(1, clients.size());
        ClientRepresentation brokerApp = clients.get(0);
        brokerApp.setConsentRequired(true);
        brokeredRealm.clients().get(brokerApp.getId()).update(brokerApp);
        /* verify that the interaction_required error is returned with sending auth request to the consumer realm with prompt=none. */
        checkAuthWithPromptNoneReturnsInteractionRequired();
    }

    /**
     * Utility method that authenticates the broker user directly in the IDP to establish a session there. It then proceeds to
     * send an auth request to the account app in the consumer realm with {@code prompt=none}, checking that the resulting page
     * is an error page containing the {@code interaction_required} message.
     */
    protected void checkAuthWithPromptNoneReturnsInteractionRequired() {
        /* start by authenticating directly in the IDP so the test user is already authenticated there. */
        authenticateDirectlyInIDP();

        /* send an auth request to the consumer realm with prompt=none and a default provider. */
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        waitForPage(driver, "sign in to", true);
        String url = driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias() + "&prompt=none";
        driver.navigate().to(url);
        Assert.assertTrue(driver.getCurrentUrl().contains(bc.consumerRealmName() + "/account/login-redirect?error=interaction_required"));
    }

    /**
     * Authenticates the broker user directly in the IDP to establish a valid authenticated session there.
     */
    protected void authenticateDirectlyInIDP() {
        driver.navigate().to(getAccountUrl(getProviderRoot(), bc.providerRealmName()));
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForAccountManagementTitle();
        Assert.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/account"));
        accountUpdateProfilePage.assertCurrent();
    }

    private class KcOidcBrokerPromptNoneConfiguration extends KcOidcBrokerConfiguration {

        /**
         * Override the default configuration to unset the {@code prompt} parameter and specify that the IDP accepts forwarded
         * auth requests with {@code prompt=none}.
         */
        @Override
        protected void applyDefaultConfiguration(final Map<String, String> config, IdentityProviderSyncMode syncMode) {
            super.applyDefaultConfiguration(config, syncMode);
            config.remove("prompt");
            config.put("acceptsPromptNoneForwardFromClient", "true");
        }
    }

}