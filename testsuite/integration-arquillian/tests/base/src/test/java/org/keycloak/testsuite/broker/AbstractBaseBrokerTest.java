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
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.ProceedPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.TimeoutException;

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
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * No test methods there. Just some useful common functionality
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
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


    protected String createUser(String username) {
        UserRepresentation newUser = UserBuilder.create().username(username).email(USER_EMAIL).enabled(true).build();
        String userId = createUserWithAdminClient(adminClient.realm(bc.consumerRealmName()), newUser);
        resetUserPassword(adminClient.realm(bc.consumerRealmName()).users().get(userId), "password", false);
        return userId;
    }


    protected void assertNumFederatedIdentities(String userId, int expected) {
        assertEquals(expected, adminClient.realm(bc.consumerRealmName()).users().get(userId).getFederatedIdentity().size());
    }

    protected void logInAsUserInIDP() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to", true);

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
    }


    /** Logs in the IDP and updates account information */
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();

        waitForPage(driver, "update account information", false);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
    }


    protected String getAccountUrl(String realmName) {
        return BrokerTestTools.getAuthRoot(suiteContext) + "/auth/realms/" + realmName + "/account";
    }


    protected String getAccountPasswordUrl(String realmName) {
        return BrokerTestTools.getAuthRoot(suiteContext) + "/auth/realms/" + realmName + "/account/password";
    }

    /**
     * Get the login page for an existing client in provided realm
     * @param realmName Name of the realm
     * @param clientId ClientId of a client. Client has to exists in the realm.
     * @return Login URL
     */
    protected String getLoginUrl(String realmName, String clientId) {
        List<ClientRepresentation> clients = adminClient.realm(realmName).clients().findByClientId(clientId);

        assertThat(clients, Matchers.is(Matchers.not(Matchers.empty())));

        String redirectURI = clients.get(0).getBaseUrl();

        return BrokerTestTools.getAuthRoot(suiteContext) + "/auth/realms/" + realmName + "/protocol/openid-connect/auth?client_id=" +
                clientId + "&redirect_uri=" + redirectURI + "&response_type=code&scope=openid";
    }

    protected void logoutFromRealm(String realm) {
        logoutFromRealm(realm, null);
    }

    protected void logoutFromRealm(String realm, String initiatingIdp) { logoutFromRealm(realm, initiatingIdp, null); }

    protected void logoutFromRealm(String realm, String initiatingIdp, String tokenHint) {
        driver.navigate().to(BrokerTestTools.getAuthRoot(suiteContext)
                + "/auth/realms/" + realm
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(realm))
                + (!StringUtils.isBlank(initiatingIdp) ? "&initiating_idp=" + initiatingIdp : "")
                + (!StringUtils.isBlank(tokenHint) ? "&id_token_hint=" + tokenHint : "")
        );

        try {
            Retry.execute(() -> {
                try {
                    waitForPage(driver, "log in to " + realm, true);
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
        waitForAccountManagementTitle();
        Assert.assertTrue(accountUpdateProfilePage.isCurrent());
        Assert.assertEquals(accountUpdateProfilePage.getUsername(), bc.getUserLogin());
        Assert.assertEquals(accountUpdateProfilePage.getEmail(), bc.getUserEmail());
    }

    protected void waitForAccountManagementTitle() {
        boolean isProduct = adminClient.serverInfo().getInfo().getProfileInfo().getName().equals("product");
        String title = isProduct ? "rh-sso account management" : "keycloak account management";
        waitForPage(driver, title, true);
    }


    protected void assertErrorPage(String expectedError) {
        errorPage.assertCurrent();
        Assert.assertEquals(expectedError, errorPage.getError());
    }


    protected URI getAuthServerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }


}
