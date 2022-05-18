package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.openqa.selenium.TimeoutException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configurePostBrokerLoginWithOTP;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.disablePostBrokerLoginFlow;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.grantReadTokenRole;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.revokeReadTokenRole;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForElementEnabled;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * Test of advanced functionalities related to brokering like:
 * - Account management linking
 * - Retrieve of broker token
 * - PostBrokerLoginFlow
 * - Single logout propagation to broker
 * - Disabled user
 * - etc
 */
public abstract class AbstractAdvancedBrokerTest extends AbstractBrokerTest {


    protected void createRoleMappersForConsumerRealm() {
        createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode.FORCE);
    }

    protected void createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode syncMode) {
        log.debug("adding mappers to identity provider in realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        IdentityProviderResource idpResource = realm.identityProviders().get(bc.getIDPAlias());
        for (IdentityProviderMapperRepresentation mapper : createIdentityProviderMappers(syncMode)) {
            mapper.setIdentityProviderAlias(bc.getIDPAlias());
            Response resp = idpResource.addMapper(mapper);
            resp.close();
        }
    }

    protected abstract Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode);

    protected abstract void createAdditionalMapperWithCustomSyncMode(IdentityProviderMapperSyncMode syncMode);

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testAccountManagementLinkIdentity
     */
    @Test
    public void testAccountManagementLinkIdentity() {
        createUser("consumer");
        // Login as pedroigor to account management
        accountFederatedIdentityPage.realm(bc.consumerRealmName());
        accountFederatedIdentityPage.open();
        loginPage.login("consumer", "password");
        assertTrue(accountFederatedIdentityPage.isCurrent());

        accountFederatedIdentityPage.clickAddProvider(bc.getIDPAlias());
        this.loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // Assert identity linked in account management
        assertTrue(accountFederatedIdentityPage.isCurrent());
        assertTrue(accountFederatedIdentityPage.isLinked(bc.getIDPAlias()));

        // Revoke grant in account mgmt
        accountFederatedIdentityPage.clickRemoveProvider(bc.getIDPAlias());

        // Logout from account management
        accountFederatedIdentityPage.logout();

        // Assert I am logged immediately to account management due to previously linked "test-user" identity
        logInWithBroker(bc);
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        loginPage.login(bc.getUserPassword());

        accountFederatedIdentityPage.assertCurrent();
        assertTrue(accountFederatedIdentityPage.isLinked(bc.getIDPAlias()));

        // Unlink my "test-user"
        accountFederatedIdentityPage.clickRemoveProvider(bc.getIDPAlias());
        assertFalse(accountFederatedIdentityPage.isLinked(bc.getIDPAlias()));

        // Logout from account management
        accountFederatedIdentityPage.logout();

        // Try to login. Previous link is not valid anymore, so now it should try to register new user
        loginPage.clickSocial(bc.getIDPAlias());
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testAccountManagementLinkedIdentityAlreadyExists
     */
    @Test
    public void testAccountManagementLinkedIdentityAlreadyExists() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        accountFederatedIdentityPage.realm(bc.consumerRealmName());
        accountFederatedIdentityPage.open();
        loginPage.login("consumer", "password");
        assertTrue(accountFederatedIdentityPage.isCurrent());

        accountFederatedIdentityPage.clickAddProvider(bc.getIDPAlias());
        this.loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        assertTrue(accountFederatedIdentityPage.isCurrent());
        assertEquals("Federated identity returned by " + bc.getIDPAlias() + " is already linked to another user.", accountFederatedIdentityPage.getError());
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testTokenStorageAndRetrievalByApplication
     */
    @Test
    public void testRetrieveToken() throws Exception {
        updateExecutions(AbstractBrokerTest::enableRequirePassword);
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();

        idpRep.setStoreToken(true);

        identityProviderResource.update(idpRep);

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);
        updatePasswordPage.updatePasswords("password", "password");
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

        String username = bc.getUserLogin();

        testingClient.server(bc.consumerRealmName()).run(grantReadTokenRole(username));

        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.realm(bc.consumerRealmName()).clientId("broker-app").doGrantAccessTokenRequest("broker-app-secret", bc.getUserLogin(), bc.getUserPassword());
        AtomicReference<String> accessToken = (AtomicReference<String>) new AtomicReference<>(accessTokenResponse.getAccessToken());
        Client client = javax.ws.rs.client.ClientBuilder.newBuilder().register((ClientRequestFilter) request -> request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.get())).build();

        try {
            WebTarget target = client.target(Urls.identityProviderRetrieveToken(URI.create(getConsumerRoot() + "/auth"), bc.getIDPAlias(), bc.consumerRealmName()));

            try (Response response = target.request().get()) {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertNotNull(response.readEntity(String.class));
            }

            testingClient.server(bc.consumerRealmName()).run(revokeReadTokenRole(username));

            accessTokenResponse = oauth.realm(bc.consumerRealmName()).clientId("broker-app").doGrantAccessTokenRequest("broker-app-secret", bc.getUserLogin(), bc.getUserPassword());
            accessToken.set(accessTokenResponse.getAccessToken());

            try (Response response = target.request().get()) {
                assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            }
        } finally {
            client.close();
        }
    }



    // KEYCLOAK-3267
    @Test
    public void loginWithExistingUserWithBruteForceEnabled() {
        adminClient.realm(bc.consumerRealmName()).update(RealmBuilder.create().bruteForceProtected(true).failureFactor(2).build());

        loginWithExistingUser();

        driver.navigate().to(getAccountPasswordUrl(getConsumerRoot(), bc.consumerRealmName()));

        accountPasswordPage.changePassword("password", "password");

        logoutFromRealm(getProviderRoot(), bc.providerRealmName());

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        try {
            waitForPage(driver, "sign in to", true);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }

        for (int i = 0; i < 3; i++) {
            try {
                waitForElementEnabled(driver, "login");
            } catch (TimeoutException e) {
                Assert.fail("Timeout while waiting for login element enabled");
            }

            loginPage.login(bc.getUserLogin(), "invalid");
        }

        assertEquals("Invalid username or password.", loginPage.getInputError());

        loginPage.clickSocial(bc.getIDPAlias());

        try {
            waitForPage(driver, "sign in to", true);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }

        Assert.assertTrue("Driver should be on the provider realm page right now", driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        assertEquals("Account is disabled, contact your administrator.", errorPage.getError());
    }

    // KEYCLOAK-4181
    @Test
    public void loginWithExistingUserWithErrorFromProviderIdP() {
        ClientRepresentation client = adminClient.realm(bc.providerRealmName())
                .clients()
                .findByClientId(bc.getIDPClientIdInProviderRealm())
                .get(0);

        adminClient.realm(bc.providerRealmName())
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client).consentRequired(true).build());

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.MINUTES);

        waitForPage(driver, "grant access", false);
        consentPage.cancel();

        waitForPage(driver, "sign in to", true);

        // Revert consentRequired
        adminClient.realm(bc.providerRealmName())
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client).consentRequired(false).build());

    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testDisabledUser
     */
    @Test
    public void testDisabledUser() {
        loginUser();
        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation userRep = realm.users().search(bc.getUserLogin()).get(0);
        UserResource user = realm.users().get(userRep.getId());

        userRep.setEnabled(false);

        user.update(userRep);

        logInWithBroker(bc);
        errorPage.assertCurrent();
        assertEquals("Account is disabled, contact your administrator.", errorPage.getError());
    }

    // KEYCLOAK-3987
    @Test
    public void mapperDoesNotGrantNewRoleFromTokenWithSyncModeImport() {
        testMapperAssigningRoles(IdentityProviderMapperSyncMode.IMPORT, false);
    }

    @Test
    public void mapperGrantsNewRoleFromTokenWithInheritedSyncModeForce() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().get(bc.getIDPAlias())
                .update(bc.setUpIdentityProvider(IdentityProviderSyncMode.FORCE));

        testMapperAssigningRoles(IdentityProviderMapperSyncMode.INHERIT, true);
    }

    @Test
    public void mapperDoesNotGrantNewRoleFromTokenWithInheritedSyncModeImport() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().get(bc.getIDPAlias())
                .update(bc.setUpIdentityProvider(IdentityProviderSyncMode.IMPORT));

        testMapperAssigningRoles(IdentityProviderMapperSyncMode.INHERIT, false);
    }

    private void testMapperAssigningRoles(IdentityProviderMapperSyncMode anImport, boolean isAssigned) {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm(anImport);

        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        logInAsUserInIDPForFirstTime();

        UserResource consumerUserResource = adminClient.realm(bc.consumerRealmName()).users().get(
                adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin()).get(0).getId());
        Set<String> currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER)));

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());


        userResource.roles().realmLevel().add(Collections.singletonList(userRole));

        logInAsUserInIDP();

        currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        if (isAssigned) {
            assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_USER));
        } else {
            assertThat(currentRoles, hasItems(ROLE_MANAGER));
            assertThat(currentRoles, not(hasItems(ROLE_USER)));
        }

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
    }

    @Test
    public void differentMappersCanHaveDifferentSyncModes() {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode.INHERIT);
        createAdditionalMapperWithCustomSyncMode(IdentityProviderMapperSyncMode.FORCE);


        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();
        RoleRepresentation friendlyManagerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_FRIENDLY_MANAGER).toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        logInAsUserInIDPForFirstTime();

        UserResource consumerUserResource = adminClient.realm(bc.consumerRealmName()).users().get(
                adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin()).get(0).getId());
        Set<String> currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER, ROLE_FRIENDLY_MANAGER)));

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());


        userResource.roles().realmLevel().add(Arrays.asList(userRole, friendlyManagerRole));

        logInAsUserInIDP();

        currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_FRIENDLY_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER)));

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
    }

    // KEYCLOAK-4016
    @Test
    public void testExpiredCode() {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        log.debug("Expire all browser cookies");
        driver.manage().deleteAllCookies();

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sorry", false);
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();
        Assert.assertTrue(link.endsWith("/auth/realms/consumer/account/"));
    }

    /**
     * Refers to in old testsuite: org.keycloak.testsuite.broker.PostBrokerFlowTest#testPostBrokerLoginWithOTP()
     */
    @Test
    public void testPostBrokerLoginFlowWithOTP() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        logInWithBroker(bc);

        totpPage.assertCurrent();
        String totpSecret = totpPage.getTotpSecret();
        totpPage.configure(totp.generateTOTP(totpSecret));
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        logInWithBroker(bc);

        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP(totpSecret));
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        testingClient.server(bc.consumerRealmName()).run(disablePostBrokerLoginFlow(bc.getIDPAlias()));
        logInWithBroker(bc);
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
    }

    // KEYCLOAK-12986
    @Test
    public void testPostBrokerLoginFlowWithOTP_bruteForceEnabled() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));

        // Enable brute force protector in cosumer realm
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation consumerRealmRep = realm.toRepresentation();
        consumerRealmRep.setBruteForceProtected(true);
        consumerRealmRep.setFailureFactor(2);
        consumerRealmRep.setMaxDeltaTimeSeconds(20);
        consumerRealmRep.setMaxFailureWaitSeconds(100);
        consumerRealmRep.setWaitIncrementSeconds(5);
        realm.update(consumerRealmRep);

        try {
            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

            logInWithBroker(bc);

            totpPage.assertCurrent();
            String totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));
            assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);
            logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

            logInWithBroker(bc);

            loginTotpPage.assertCurrent();

            // Login for 2 times with incorrect TOTP. This should temporarily disable the user
            loginTotpPage.login("bad-totp");
            Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

            loginTotpPage.login("bad-totp");
            Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

            // Login with valid TOTP. I should not be able to login
            loginTotpPage.login(totp.generateTOTP(totpSecret));
            Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

            // Clear login failures
            String userId = ApiUtil.findUserByUsername(realm, bc.getUserLogin()).getId();
            realm.attackDetection().clearBruteForceForUser(userId);

            loginTotpPage.login(totp.generateTOTP(totpSecret));
            waitForAccountManagementTitle();
            logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        } finally {
            testingClient.server(bc.consumerRealmName()).run(disablePostBrokerLoginFlow(bc.getIDPAlias()));

            // Disable brute force protector
            consumerRealmRep = realm.toRepresentation();
            consumerRealmRep.setBruteForceProtected(false);
            realm.update(consumerRealmRep);
        }
    }

    /**
     * Refers to in old testsuite: org.keycloak.testsuite.broker.OIDCKeyCloakServerBrokerBasicTest#testLogoutWorksWithTokenTimeout()
     */
    @Test
    public void testLogoutWorksWithTokenTimeout() {
        try {
            updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
            RealmRepresentation realm = adminClient.realm(bc.providerRealmName()).toRepresentation();
            assertNotNull(realm);
            realm.setAccessTokenLifespan(1);
            adminClient.realm(bc.providerRealmName()).update(realm);
            IdentityProviderRepresentation idp = adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).toRepresentation();
            idp.getConfig().put("backchannelSupported", "false");
            adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).update(idp);
            Time.setOffset(2);
            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
            logInWithBroker(bc);
            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.assertCurrent();
            updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
            accountPage.logOut();
            waitForPage(driver, "sign in to", true);
            log.debug("Logging in");
            assertTrue(this.driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/auth"));
        } finally {
            Time.setOffset(0);
        }
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testWithLinkedFederationProvider
     */
    @Test
    public void testWithLinkedFederationProvider() {
        // don't run this test when map storage is enabled, as map storage doesn't support the legacy style federation
        ProfileAssume.assumeFeatureDisabled(Profile.Feature.MAP_STORAGE);

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

            ComponentRepresentation component = new ComponentRepresentation();
            component.setName(DummyUserFederationProviderFactory.PROVIDER_NAME);
            component.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            component.setProviderType(UserStorageProvider.class.getName());

            adminClient.realm(bc.consumerRealmName()).components().add(component);

            createUser(bc.providerRealmName(), "test-user", "password", "FirstName", "LastName", "test-user@localhost.com");
            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
            loginPage.clickSocial(bc.getIDPAlias());
            loginPage.login("test-user", "password");
            waitForAccountManagementTitle();
            accountUpdateProfilePage.assertCurrent();

            accountPage.password();
            accountPasswordPage.changePassword("bad", "new-password", "new-password");
            assertEquals("Invalid existing password.", accountPasswordPage.getError());

            accountPasswordPage.changePassword("secret", "new-password", "new-password");
            assertEquals("Your password has been updated.", accountUpdateProfilePage.getSuccess());

            logoutFromRealm(getProviderRoot(), bc.providerRealmName());
            logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

            createUser(bc.providerRealmName(), "test-user-noemail", "password", "FirstName", "LastName", "test-user-noemail@localhost.com");
            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
            loginPage.clickSocial(bc.getIDPAlias());
            loginPage.login("test-user-noemail", "password");
            waitForAccountManagementTitle();
            accountUpdateProfilePage.assertCurrent();

            accountPage.password();
            accountPasswordPage.changePassword("new-password", "new-password");
            assertEquals("Your password has been updated.", accountUpdateProfilePage.getSuccess());
        } finally {
            removeUserByUsername(adminClient.realm(bc.consumerRealmName()), "test-user");
            removeUserByUsername(adminClient.realm(bc.consumerRealmName()), "test-user-noemail");
        }
    }
}
