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

package org.keycloak.testsuite.broker;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Retry;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.IdpConfirmOverrideLinkPage;
import org.keycloak.testsuite.pages.IdpLinkEmailPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginExpiredPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.ProceedPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LogoutUrlBuilder;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.encodeUrl;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

/**
 * No test methods there. Just some useful common functionality
 */
public abstract class AbstractBaseBrokerTest extends AbstractKeycloakTest {

    protected static final String ATTRIBUTE_VALUE = "attribute.value";

    @Page
    protected LoginPage loginPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @Page
    protected IdpConfirmOverrideLinkPage idpConfirmOverrideLinkPage;

    @Page
    protected ProceedPage proceedPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected IdpLinkEmailPage idpLinkEmailPage;

    @Page
    protected LoginExpiredPage loginExpiredPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginConfigTotpPage totpPage;

    @Page
    protected LoginPasswordResetPage loginPasswordResetPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected AppPage appPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();

    protected BrokerConfiguration bc = getBrokerConfiguration();

    protected String userId;

    /**
     * Returns a broker configuration. Return value should not change between calls.
     * @return
     */
    protected abstract BrokerConfiguration getBrokerConfiguration();

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

    }

    protected void configureSMTPServer() {
        MailServer.start();
        MailServer.createEmailAccount(USER_EMAIL, "password");
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation master = realm.toRepresentation();
        master.setSmtpServer(suiteContext.getSmtpServer());
        realm.update(master);
    }

    protected void removeSMTPConfiguration(RealmResource consumerRealm) {
        RealmRepresentation master = consumerRealm.toRepresentation();
        master.setSmtpServer(Collections.emptyMap());
        consumerRealm.update(master);
    }

    protected void addClientsToProviderAndConsumer() {
        List<ClientRepresentation> clients = bc.createProviderClients();
        final RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        for (ClientRepresentation client : clients) {
            log.debug("adding client " + client.getClientId() + " to realm " + bc.providerRealmName());

            final Response resp = providerRealm.clients().create(client);
            resp.close();
        }

        clients = bc.createConsumerClients();
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getClientId() + " to realm " + bc.consumerRealmName());

                Response resp = consumerRealm.clients().create(client);
                resp.close();
            }
        }
    }

    @Before
    public void beforeBrokerTest() {
        RealmRepresentation consumerRealm = bc.createConsumerRealm();
        RealmRepresentation providerRealm = bc.createProviderRealm();
        importRealm(consumerRealm);
        importRealm(providerRealm);

        UserProfileUtil.enableUnmanagedAttributes(adminClient.realm(consumerRealm.getRealm()).users().userProfile());
        UserProfileUtil.enableUnmanagedAttributes(adminClient.realm(providerRealm.getRealm()).users().userProfile());
    }

    @After
    public void cleanupUsers() {
        adminClient.realm(bc.consumerRealmName()).remove();
        adminClient.realm(bc.providerRealmName()).remove();
        MailServer.stop();
    }

    protected String createUser(String username, String email) {
        UserRepresentation newUser = UserBuilder.create().username(username).email(email).enabled(true).build();
        String userId = createUserWithAdminClient(adminClient.realm(bc.consumerRealmName()), newUser);
        resetUserPassword(adminClient.realm(bc.consumerRealmName()).users().get(userId), "password", false);
        return userId;
    }

    protected String createUser(String username) {
        return createUser(username, USER_EMAIL);
    }


    protected void assertNumFederatedIdentities(String userId, int expected) {
        assertEquals(expected, adminClient.realm(bc.consumerRealmName()).users().get(userId).getFederatedIdentity().size());
    }

    protected void logInAsUserInIDP() {
        logInAsUserInIDP("broker-app");
    }

    protected void logInAsUserInIDP(String clientId) {
        oauth.clientId(clientId);
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
    }

    // We are re-authenticating to the IDP. Hence it is assumed that "username" field is not visible on the login form on the IDP side
    protected void logInAsUserInIDPWithReAuthenticate() {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);

        // We are re-authenticating. Username field not visible
        log.debug("Reauthenticating");
        Assert.assertFalse(loginPage.isUsernameInputPresent());
        loginPage.login(bc.getUserPassword());
    }

    protected void logInWithBroker(BrokerConfiguration bc) {
        logInWithIdp(bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());
    }

    protected void logInWithIdp(String idpAlias, String username, String password) {
        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + idpAlias);
        loginPage.clickSocial(idpAlias);
        waitForPage(driver, "sign in to", true);
        log.debug("Logging in");
        loginPage.login(username, password);
    }

    protected AuthorizationEndpointResponse doLoginSocial(OAuthClient oauth, String brokerId, String username, String password) {
        return doLoginSocial(oauth, brokerId, username, password, null);
    }

    protected AuthorizationEndpointResponse doLoginSocial(OAuthClient oauth, String brokerId, String username, String password, String nonce) {
        oauth.loginForm().nonce(nonce).open();
        WaitUtils.waitForPageToLoad();

        oauth.getDriver().findElement(By.id("social-" + brokerId)).click();
        oauth.fillLoginForm(username, password);

        if (updateAccountInformationPage.isCurrent()) {
            log.debug("Updating info on updateAccount page");
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
        }

        return oauth.parseLoginResponse();
    }

    /** Logs in the IDP and updates account information */
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();
        updateAccountInformation();
    }

    protected void logInAsUserInIDPForFirstTimeAndAssertSuccess() {
        logInAsUserInIDPForFirstTime();
        appPage.assertCurrent();
    }

    protected void updateAccountInformation() {
        waitForPage(driver, "update account information", false);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
    }


    protected String getAccountUrl(String contextRoot, String realmName) {
        return contextRoot + "/auth/realms/" + realmName + "/account";
    }

    /**
     * Get the login page for an existing client in provided realm
     *
     * @param contextRoot server base url without /auth
     * @param realmName Name of the realm
     * @param clientId ClientId of a client. Client has to exists in the realm.
     * @return Login URL
     */
    protected String getLoginUrl(String contextRoot, String realmName, String clientId) {
        List<ClientRepresentation> clients = adminClient.realm(realmName).clients().findByClientId(clientId);

        assertThat(clients, Matchers.is(Matchers.not(Matchers.empty())));

        String redirectURI = clients.get(0).getBaseUrl();
        if (redirectURI.startsWith("/")) {
            redirectURI = contextRoot + "/auth" + redirectURI;
        }

        return contextRoot + "/auth/realms/" + realmName + "/protocol/openid-connect/auth?client_id=" +
                clientId + "&redirect_uri=" + redirectURI + "&response_type=code&scope=openid";
    }

    protected void logoutFromRealm(String contextRoot, String realm) {
        logoutFromRealm(contextRoot, realm, null);
    }

    protected void logoutFromRealm(String contextRoot, String realm, String initiatingIdp) {
        logoutFromRealm(contextRoot, realm, initiatingIdp, null);
    }

    protected void logoutFromRealm(String contextRoot, String realm, String initiatingIdp, String idTokenHint) {
        logoutFromRealm(contextRoot, realm, initiatingIdp, idTokenHint, null);
    }

    protected void logoutFromRealm(String contextRoot, String realm, String initiatingIdp, String idTokenHint, String clientId) {
        logoutFromRealm(contextRoot, realm, initiatingIdp, idTokenHint, clientId, null);
    }

    // Completely logout from realm and confirm logout if present
    protected void logoutFromRealm(String contextRoot, String realm, String initiatingIdp, String idTokenHint, String clientId, String redirectUri) {
        final String defaultRedirectUri = redirectUri != null ? redirectUri : oauth.loginForm().build();
        final String defaultClientId = (idTokenHint == null && clientId == null) ? "test-app" : clientId;

        executeLogoutFromRealm(contextRoot, realm, initiatingIdp, idTokenHint, defaultClientId, defaultRedirectUri);
        checkLogoutConfirmation(realm, idTokenHint, defaultClientId);
    }

    // Only execute the logout without logout confirmation
    protected void executeLogoutFromRealm(String contextRoot, String realm, String initiatingIdp, String idTokenHint, String clientId, String redirectUri) {
        final boolean isDifferentContext = !Objects.equals(OAuthClient.SERVER_ROOT, removeDefaultPorts(contextRoot));

        try {
            if (isDifferentContext) {
                OAuthClient.updateURLs(contextRoot);
                OAuthClient.updateAppRootRealm(realm);
                oauth.init();
            }

            final LogoutUrlBuilder builder = oauth.realm(realm).logoutForm()
                    .idTokenHint(idTokenHint)
                    .initiatingIdp(initiatingIdp);

            if (clientId != null) {
                builder.withClientId();
            }

            if (redirectUri != null && (clientId != null || idTokenHint != null)) {
                builder.postLogoutRedirectUri(encodeUrl(redirectUri));
            }

            builder.open();
        } finally {
            if (isDifferentContext) {
                OAuthClient.updateURLs(getAuthServerContextRoot());
                oauth.init();
            }
        }
    }

    // Check whether the logout confirmation is present; if yes, confirm the logout and verify the current page
    private void checkLogoutConfirmation(String realm, String idTokenHint, String clientId) {
        if (logoutConfirmPage.isCurrent()) {
            confirmLogout();
            if (idTokenHint != null || clientId != null) {
                assertLoginPage(realm);
            } else {
                infoPage.assertCurrent();
            }
        }
    }

    protected void confirmLogout() {
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
    }

    protected void assertLoginPage(String realm) {
        try {
            Retry.execute(() -> {
                try {
                    waitForPage(driver, "sign in to " + realm, true);
                } catch (TimeoutException ex) {
                    driver.navigate().refresh();
                    log.debug("[Retriable] Timed out waiting for login page");
                    throw ex;
                }
            }, 10, 100);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }
    }

    protected void waitForAccountManagementTitle() {
        final String title = "Keycloak account management";
        waitForPage(driver, title, true);
    }

    protected void assertErrorPage(String expectedError) {
        errorPage.assertCurrent();
        Assert.assertEquals(expectedError, errorPage.getError());
    }


    protected URI getConsumerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return getSamlEndpoint(getConsumerRoot(), realm);
    }

    protected URI getProviderSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return getSamlEndpoint(getProviderRoot(), realm);
    }

    protected URI getSamlEndpoint(String fromUri, String realm) {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(fromUri).path("auth"))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }
}
