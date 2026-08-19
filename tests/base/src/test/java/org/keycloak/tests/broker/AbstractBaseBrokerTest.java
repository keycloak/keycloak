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

package org.keycloak.tests.broker;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Retry;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.IdpConfirmLinkPage;
import org.keycloak.testframework.ui.page.IdpConfirmOverrideLinkPage;
import org.keycloak.testframework.ui.page.IdpLinkEmailPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LoginExpiredPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.testframework.ui.page.UpdateAccountInformationPage;
import org.keycloak.testframework.ui.page.VerifyEmailPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LogoutUrlBuilder;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.tests.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.tests.broker.BrokerTestTools.encodeUrl;
import static org.keycloak.tests.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.tests.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.tests.broker.BrokerTestTools.waitForPage;
import static org.keycloak.tests.utils.admin.AdminApiUtil.createUserWithAdminClient;
import static org.keycloak.tests.utils.admin.AdminApiUtil.resetUserPassword;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * No test methods there. Just some useful common functionality
 */
@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public abstract class AbstractBaseBrokerTest {

    protected final Logger log = Logger.getLogger(getClass());

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    protected KeycloakTestingClient testingClient;
    protected final CompatibilitySuiteContext suiteContext = new CompatibilitySuiteContext();
    protected final CompatibilityTestContext testContext = new CompatibilityTestContext();
    protected final AtomicInteger timeOffSet = new AtomicInteger();
    private final Map<String, TestCleanupSupport> cleanups = new HashMap<>();

    protected static final String ATTRIBUTE_VALUE = "attribute.value";

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @InjectPage
    protected IdpConfirmOverrideLinkPage idpConfirmOverrideLinkPage;

    @InjectPage
    protected ProceedPage proceedPage;

    @InjectPage
    protected LogoutConfirmPage logoutConfirmPage;

    @InjectPage
    protected InfoPage infoPage;

    @InjectPage
    protected IdpLinkEmailPage idpLinkEmailPage;

    @InjectPage
    protected LoginExpiredPage loginExpiredPage;

    @InjectPage
    protected LoginTotpPage loginTotpPage;

    @InjectPage
    protected LoginConfigTotpPage totpPage;

    @InjectPage
    protected LoginPasswordResetPage loginPasswordResetPage;

    @InjectPage
    protected VerifyEmailPage verifyEmailPage;

    @InjectPage
    protected OAuthGrantPage grantPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();

    protected BrokerConfiguration bc = getBrokerConfiguration();

    protected String userId;

    /**
     * Returns a broker configuration. Return value should not change between calls.
     * @return
     */
    protected abstract BrokerConfiguration getBrokerConfiguration();

    public void addTestRealms(List<RealmRepresentation> testRealms) {

    }

    protected void importRealm(RealmRepresentation realmRepresentation) {
        try {
            adminClient.realm(realmRepresentation.getRealm()).remove();
        } catch (NotFoundException ignored) {
            // expected if realm does not exist yet
        }
        adminClient.realms().create(realmRepresentation);
    }

    protected void deleteAllCookiesForRealm(String realm) {
        driver.driver().manage().deleteAllCookies();
    }

    protected void configureSMTPServer() {
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

    @BeforeEach
    public void beforeBrokerTest() {
        if (testingClient != null) {
            testingClient.close();
        }
        String authServerRoot = resolveAuthServerContextRoot();
        testingClient = KeycloakTestingClient.getInstance(authServerRoot);
        OAuthClient.updateURLs(authServerRoot);
        oauth.init();

        RealmRepresentation consumerRealm = bc.createConsumerRealm();
        RealmRepresentation providerRealm = bc.createProviderRealm();
        importRealm(consumerRealm);
        importRealm(providerRealm);

        UserProfileUtil.enableUnmanagedAttributes(adminClient.realm(consumerRealm.getRealm()).users().userProfile());
        UserProfileUtil.enableUnmanagedAttributes(adminClient.realm(providerRealm.getRealm()).users().userProfile());
    }

    @AfterEach
    public void cleanupUsers() {
        cleanups.values().forEach(cleanup -> cleanup.execute(adminClient));
        cleanups.clear();
        if (testingClient != null) {
            testingClient.close();
            testingClient = null;
        }
        deleteAllCookiesForRealm(bc.consumerRealmName());
        adminClient.realm(bc.consumerRealmName()).remove();
        adminClient.realm(bc.providerRealmName()).remove();
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

    public String createUser(String realm, String username, String password, String... requiredActions) {
        UserRepresentation user = UserBuilder.create().username(username).enabled(true).build();
        user.setRequiredActions(Arrays.asList(requiredActions));
        String createdUserId = createUserWithAdminClient(adminClient.realm(realm), user);
        resetUserPassword(adminClient.realm(realm).users().get(createdUserId), password, false);
        return createdUserId;
    }

    public String createUser(String realm, String username, String password, String firstName) {
        return createUser(realm, username, password, firstName, null, null);
    }

    public String createUser(String realm, String username, String password, String firstName, String lastName) {
        return createUser(realm, username, password, firstName, lastName, null);
    }

    protected String createUser(String realm, String username, String password, String firstName, String lastName, String email) {
        UserRepresentation newUser = UserBuilder.create()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .build();
        String createdUserId = createUserWithAdminClient(adminClient.realm(realm), newUser);
        resetUserPassword(adminClient.realm(realm).users().get(createdUserId), password, false);
        return createdUserId;
    }

    public String createUser(String realm, String username, String password, String firstName, String lastName, String email,
            Consumer<UserRepresentation> customizer) {
        UserRepresentation newUser = UserBuilder.create()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .build();
        customizer.accept(newUser);
        String createdUserId = createUserWithAdminClient(adminClient.realm(realm), newUser);
        resetUserPassword(adminClient.realm(realm).users().get(createdUserId), password, false);
        return createdUserId;
    }


    protected void assertNumFederatedIdentities(String userId, int expected) {
        assertEquals(expected, adminClient.realm(bc.consumerRealmName()).users().get(userId).getFederatedIdentity().size());
    }

    protected void logInAsUserInIDP() {
        logInAsUserInIDP("broker-app");
    }

    protected void logInAsUserInIDP(String clientId) {
        oauth.client(clientId);
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        logInWithBroker(bc);
    }

    // We are re-authenticating to the IDP. Hence it is assumed that "username" field is not visible on the login form on the IDP side
    protected void logInAsUserInIDPWithReAuthenticate() {
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);

        // We are re-authenticating. Username field not visible
        log.debug("Reauthenticating");
        Assertions.assertFalse(loginPage.isUsernameInputPresent());
        loginPage.login(bc.getUserPassword());
    }

    protected void logInWithBroker(BrokerConfiguration bc) {
        logInWithIdp(bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());
    }

    protected void logInWithIdp(String idpAlias, String username, String password) {
        waitForPage(driver, "sign in to", true);
        log.debug("Clicking social " + idpAlias);
        loginPage.clickSocial(idpAlias);
        new WebDriverWait(driver.driver(), Duration.ofSeconds(10)).until(webDriver ->
                !webDriver.findElements(By.id("username")).isEmpty()
                        || !webDriver.findElements(By.id("password")).isEmpty()
                        || webDriver.getCurrentUrl().contains("/broker/" + idpAlias + "/endpoint")
                        || webDriver.getCurrentUrl().contains("/login-actions/first-broker-login"));
        if (loginPage.isUsernameInputPresent()) {
            log.debug("Logging in with username and password");
            loginPage.login(username, password);
        } else if (loginPage.isPasswordInputPresent()) {
            log.debug("Logging in with password-only form");
            loginPage.login(password);
        } else {
            log.debugf("No login form present after social redirect (URL: %s). Continuing.", driver.getCurrentUrl());
        }
    }

    protected AuthorizationEndpointResponse doLoginSocial(OAuthClient oauth, String brokerId, String username, String password) {
        return doLoginSocial(oauth, brokerId, username, password, null);
    }

    protected AuthorizationEndpointResponse doLoginSocial(OAuthClient oauth, String brokerId, String username, String password, String nonce) {
        oauth.loginForm().nonce(nonce).open();
        WaitUtils.waitForPageToLoad();

        oauth.getDriver().findElement(By.id("social-" + brokerId)).click();
        oauth.fillLoginForm(username, password);

        return oauth.parseLoginResponse();
    }

    /** Logs in the IDP and updates account information */
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();
        updateAccountInformation();
    }

    protected void logInAsUserInIDPForFirstTimeAndAssertSuccess() {
        logInAsUserInIDPForFirstTime();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    protected void updateAccountInformation() {
        waitForPage(driver, "update account information", false);

        updateAccountInformationPage.assertCurrent();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"),
                "We must be on correct realm right now");

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
    }


    protected String getAccountUrl(String contextRoot, String realmName) {
        return contextRoot + "/auth/realms/" + realmName + "/account";
    }

    protected String getLoginUrl(String contextRoot, String realmName, String clientId) {
        return getLoginUrl(contextRoot, realmName, clientId, "openid");
    }

    /**
     * Get the login page for an existing client in provided realm
     *
     * @param contextRoot server base url without /auth
     * @param realmName Name of the realm
     * @param clientId ClientId of a client. Client has to exists in the realm.
     * @param scope The scope parameter for the request
     * @return Login URL
     */
    protected String getLoginUrl(String contextRoot, String realmName, String clientId, String scope) {
        List<ClientRepresentation> clients = adminClient.realm(realmName).clients().findByClientId(clientId);

        assertThat(clients, Matchers.is(Matchers.not(Matchers.empty())));

        String redirectURI = clients.get(0).getBaseUrl();
        if (redirectURI.startsWith("/")) {
            redirectURI = contextRoot + "/auth" + redirectURI;
        }

        return contextRoot + "/auth/realms/" + realmName + "/protocol/openid-connect/auth?client_id=" +
                clientId + "&redirect_uri=" + redirectURI + "&response_type=code&scope=" + scope;
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
        String expectedPageId = driver.findElement(By.xpath("//body")).getAttribute("data-page-id");
        if (expectedPageId.equals(errorPage.getExpectedPageId())) {
            errorPage.assertCurrent();
        } else {
            logoutConfirmPage.assertCurrent();
            Assertions.assertEquals("Logging out", driver.getTitle());
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
            Assertions.fail("Timeout while waiting for login page");
        }
    }

    protected void waitForAccountManagementTitle() {
        final String title = "Keycloak account management";
        waitForPage(driver, title, true);
    }

    protected void assertErrorPage(String expectedError) {
        errorPage.assertCurrent();
        Assertions.assertEquals(expectedError, errorPage.getError());
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

    public org.keycloak.admin.client.resource.RealmsResource realmsResouce() {
        return adminClient.realms();
    }

    public URI getAuthServerRoot() {
        return URI.create(resolveAuthServerContextRoot() + "/auth/");
    }

    protected TestCleanupSupport getCleanup(String realmName) {
        return cleanups.computeIfAbsent(realmName, TestCleanupSupport::new);
    }

    protected TestCleanupSupport getCleanup() {
        return getCleanup("test");
    }

    protected void setOtpTimeOffset(int offsetSeconds, TimeBasedOTP otp) {
        timeOffSet.set(offsetSeconds);
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.SECOND, offsetSeconds);
        otp.setCalendar(calendar);
    }

    public Logger getLogger() {
        return log;
    }

    protected String resolveAuthServerContextRoot() {
        if (managedRealm != null && managedRealm.getBaseUrl() != null) {
            int realmsIndex = managedRealm.getBaseUrl().indexOf("/realms/");
            if (realmsIndex > 0) {
                String root = managedRealm.getBaseUrl().substring(0, realmsIndex);
                return root.endsWith("/auth") ? root.substring(0, root.length() - "/auth".length()) : root;
            }
        }
        if (OAuthClient.SERVER_ROOT != null && !OAuthClient.SERVER_ROOT.isBlank()) {
            return OAuthClient.SERVER_ROOT.replaceAll("/+$", "");
        }
        return getAuthServerContextRoot();
    }

    protected static class CompatibilityTestContext {

        private boolean initialized;

        public boolean isInitialized() {
            return initialized;
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }
    }

    protected class CompatibilitySuiteContext {

        private final AuthServerInfo authServerInfo = new AuthServerInfo();

        public Map<String, String> getSmtpServer() {
            Map<String, String> smtp = new HashMap<>();
            smtp.put("host", "localhost");
            smtp.put("port", "3025");
            smtp.put("from", "auto@keycloak.org");
            return smtp;
        }

        public AuthServerInfo getAuthServerInfo() {
            return authServerInfo;
        }
    }

    protected class AuthServerInfo {
        public URI getContextRoot() {
            try {
                return new URI(resolveAuthServerContextRoot());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid auth server context root", e);
            }
        }
    }

    protected static class TestCleanupSupport {

        private final String realmName;
        private final List<Runnable> cleanups = new ArrayList<>();
        private final List<String> userIds = new ArrayList<>();
        private final List<String> componentIds = new ArrayList<>();

        private TestCleanupSupport(String realmName) {
            this.realmName = realmName;
        }

        public TestCleanupSupport addCleanup(AutoCloseable closeable) {
            cleanups.add(() -> {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                }
            });
            return this;
        }

        public void addUserId(String userId) {
            userIds.add(userId);
        }

        public void addComponentId(String componentId) {
            componentIds.add(componentId);
        }

        private void execute(Keycloak adminClient) {
            RealmResource realm = adminClient.realm(realmName);
            cleanups.forEach(Runnable::run);
            for (String userId : userIds) {
                try {
                    realm.users().get(userId).remove();
                } catch (Exception ignored) {
                }
            }
            for (String componentId : componentIds) {
                try {
                    realm.components().component(componentId).remove();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
