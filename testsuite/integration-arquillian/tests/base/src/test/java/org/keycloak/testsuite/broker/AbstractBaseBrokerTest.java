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

import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Retry;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.AccountFederatedIdentityPage;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.IdpLinkEmailPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginExpiredPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.ProceedPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.TimeoutException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.encodeUrl;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * No test methods there. Just some useful common functionality
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public abstract class AbstractBaseBrokerTest extends AbstractKeycloakTest {

    protected static final String ATTRIBUTE_VALUE = "attribute.value";

    @Page
    protected AccountUpdateProfilePage accountUpdateProfilePage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected AccountPasswordPage accountPasswordPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @Page
    protected ProceedPage proceedPage;

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
    protected AccountFederatedIdentityPage accountFederatedIdentityPage;

    @Page
    protected AccountApplicationsPage accountApplicationsPage;

    @Page
    protected OAuthGrantPage grantPage;

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
        importRealm(bc.createConsumerRealm());
        importRealm(bc.createProviderRealm());
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
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);
    }

    // We are re-authenticating to the IDP. Hence it is assumed that "username" field is not visible on the login form on the IDP side
    protected void logInAsUserInIDPWithReAuthenticate() {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

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

    /** Logs in the IDP and updates account information */
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();
        updateAccountInformation();
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


    protected String getAccountPasswordUrl(String contextRoot, String realmName) {
        return contextRoot + "/auth/realms/" + realmName + "/account/password";
    }

    /**
     * Get the login page for an existing client in provided realm
     *
     * @param contextRoot
     * @param realmName Name of the realm
     * @param clientId ClientId of a client. Client has to exists in the realm.
     * @return Login URL
     */
    protected String getLoginUrl(String contextRoot, String realmName, String clientId) {
        List<ClientRepresentation> clients = adminClient.realm(realmName).clients().findByClientId(clientId);

        assertThat(clients, Matchers.is(Matchers.not(Matchers.empty())));

        String redirectURI = clients.get(0).getBaseUrl();

        return contextRoot + "/auth/realms/" + realmName + "/protocol/openid-connect/auth?client_id=" +
                clientId + "&redirect_uri=" + redirectURI + "&response_type=code&scope=openid";
    }

    protected void logoutFromRealm(String contextRoot, String realm) {
        logoutFromRealm(contextRoot, realm, null);
    }

    protected void logoutFromRealm(String contextRoot, String realm, String initiatingIdp) { logoutFromRealm(contextRoot, realm, initiatingIdp, null); }

    protected void logoutFromRealm(String contextRoot, String realm, String initiatingIdp, String tokenHint) {
        driver.navigate().to(contextRoot
                + "/auth/realms/" + realm
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(contextRoot, realm))
                + (!StringUtils.isBlank(initiatingIdp) ? "&initiating_idp=" + initiatingIdp : "")
                + (!StringUtils.isBlank(tokenHint) ? "&id_token_hint=" + tokenHint : "")
        );

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


    protected void assertLoggedInAccountManagement() {
        assertLoggedInAccountManagement(bc.getUserLogin(), bc.getUserEmail());
    }

    protected void assertLoggedInAccountManagement(String username, String email) {
        waitForAccountManagementTitle();
        Assert.assertTrue(accountUpdateProfilePage.isCurrent());
        Assert.assertEquals(accountUpdateProfilePage.getUsername(), username);
        Assert.assertEquals(accountUpdateProfilePage.getEmail(), email);
    }

    protected void waitForAccountManagementTitle() {
        final String title = getProjectName().toLowerCase() + " account management";
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
