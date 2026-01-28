package org.keycloak.testsuite.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.crypto.Algorithm;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;

/**
 * Final class as it's not intended to be overriden. Feel free to remove "final" if you really know what you are doing.
 */
public final class KcOidcBrokerTest extends AbstractAdvancedBrokerTest {
    private final static String USER_ATTRIBUTE_NAME = "user-attribute";
    private final static String USER_ATTRIBUTE_VALUE = "attribute-value";
    private final static String CLAIM_FILTER_REGEXP = ".*-value";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return BROKER_CONFIG_INSTANCE;
    }

    @Before
    public void setUpTotp() {
        totp = new TimeBasedOTP();
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("manager-role-mapper");
        attrMapper1.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", ROLE_MANAGER)
                .put("role", ROLE_MANAGER)
                .build());

        IdentityProviderMapperRepresentation attrMapper2 = new IdentityProviderMapperRepresentation();
        attrMapper2.setName("user-role-mapper");
        attrMapper2.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper2.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", ROLE_USER)
                .put("role", ROLE_USER)
                .build());

        return Lists.newArrayList(attrMapper1, attrMapper2);
    }

    @Override
    protected void createAdditionalMapperWithCustomSyncMode(IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation friendlyManagerMapper = new IdentityProviderMapperRepresentation();
        friendlyManagerMapper.setName("friendly-manager-role-mapper");
        friendlyManagerMapper.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        friendlyManagerMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("external.role", ROLE_FRIENDLY_MANAGER)
                .put("role", ROLE_FRIENDLY_MANAGER)
                .build());
        friendlyManagerMapper.setIdentityProviderAlias(bc.getIDPAlias());
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        IdentityProviderResource idpResource = realm.identityProviders().get(bc.getIDPAlias());
        idpResource.addMapper(friendlyManagerMapper).close();
    }

    @Test
    public void mapperDoesNothingForLegacyMode() {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode.LEGACY);

        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInAsUserInIDPForFirstTime();

        UserResource consumerUserResource = adminClient.realm(bc.consumerRealmName()).users().get(
                adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin()).get(0).getId());
        Set<String> currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER)));

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        userResource.roles().realmLevel().add(Collections.singletonList(userRole));

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInAsUserInIDP();

        currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER)));

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
    }

    @Test
    public void loginFetchingUserFromUserEndpoint() {
        loginFetchingUserFromUserEndpoint(false);
    }

    private void loginFetchingUserFromUserEndpoint(boolean loginIsDenied) {
        RealmResource realm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = realm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);

        try {
            IdentityProviderResource identityProviderResource = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
            IdentityProviderRepresentation idp = identityProviderResource.toRepresentation();

            idp.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/certs");
            identityProviderResource.update(idp);

            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS256);
            brokerApp.getAttributes().put("validateSignature", Boolean.TRUE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, loginIsDenied ? "We are sorry..." : "update account information", false);
            if (loginIsDenied) {
                return;
            }

            updateAccountInformationPage.assertCurrent();
            Assert.assertTrue("We must be on correct realm right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

            log.debug("Updating info on updateAccount page");
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

            UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

            int userCount = consumerUsers.count();
            Assert.assertTrue("There must be at least one user", userCount > 0);

            List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

            boolean isUserFound = false;
            for (UserRepresentation user : users) {
                if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                    isUserFound = true;
                    break;
                }
            }

            Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
                    isUserFound);
        } finally {
            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, null);
            brokerApp.getAttributes().put("validateSignature", Boolean.FALSE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);
        }
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.OIDCBrokerUserPropertyTest
     */
    @Test
    public void loginFetchingUserFromUserEndpointWithClaimMapper() {
        RealmResource realm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = realm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        clients.get(brokerApp.getId()).getProtocolMappers().createMapper(createHardcodedClaim("hard-coded", "hard-coded", "hard-coded", "String", true, true, true)).close();

        IdentityProviderMapperRepresentation hardCodedSessionNoteMapper = new IdentityProviderMapperRepresentation();

        hardCodedSessionNoteMapper.setName("hard-coded");
        hardCodedSessionNoteMapper.setIdentityProviderAlias(bc.getIDPAlias());
        hardCodedSessionNoteMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        hardCodedSessionNoteMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString())
                .put(UserAttributeMapper.USER_ATTRIBUTE, "hard-coded")
                .put(UserAttributeMapper.CLAIM, "hard-coded")
                .build());

        identityProviderResource.addMapper(hardCodedSessionNoteMapper).close();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        loginFetchingUserFromUserEndpoint();

        UserRepresentation user = getFederatedIdentity();

        Assert.assertEquals(1, user.getAttributes().size());
        Assert.assertEquals("hard-coded", user.getAttributes().get("hard-coded").get(0));
    }


    @Test
    public void testInvalidIssuedFor() {
        loginUser();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);

        RealmResource realm = adminClient.realm(bc.providerRealmName());
        ClientRepresentation rep = realm.clients().findByClientId(BrokerTestConstants.CLIENT_ID).get(0);
        ClientResource clientResource = realm.clients().get(rep.getId());
        ProtocolMapperRepresentation hardCodedAzp = createHardcodedClaim("hard", "azp", "invalid-azp", ProviderConfigProperty.STRING_TYPE, true, true, true);
        clientResource.getProtocolMappers().createMapper(hardCodedAzp);

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        errorPage.assertCurrent();
    }

    @Test
    public void testIdpRemovedAfterLoginInvalidatesUserSession() {
        loginUser();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        assertThat(loginPage.isSocialButtonPresent(bc.getIDPAlias()), is(true));
        logInWithBroker(bc);

        // remove the IDP while the user is logged in
        adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).remove();

        // user session should still be active, but checking if it is valid should fail as the associated IDP was removed
        testingClient.server(bc.consumerRealmName()).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, "broker-app");
            List<UserSessionModel> userSessions = session.sessions().getUserSessionsStream(realm, client).toList();
            assertThat(userSessions, hasSize(1));
            UserSessionModel userSession = userSessions.get(0);
            assertThat(AuthenticationManager.isSessionValid(realm, userSession), is(false));
        });

        // logout should work even after the IDP was removed
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());

        // session should have been removed now
        testingClient.server(bc.consumerRealmName()).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, "broker-app");
            List<UserSessionModel> userSessions = session.sessions().getUserSessionsStream(realm, client).toList();
            assertThat(userSessions, hasSize(0));
        });

        loginPage.open(bc.consumerRealmName());
        assertThat(loginPage.isSocialButtonPresent(bc.getIDPAlias()), is(false));
    }

    @Test
    public void testInvalidAudience() {
        loginUser();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);

        RealmResource realm = adminClient.realm(bc.providerRealmName());
        ClientRepresentation rep = realm.clients().findByClientId(BrokerTestConstants.CLIENT_ID).get(0);
        ClientResource clientResource = realm.clients().get(rep.getId());
        ProtocolMapperRepresentation hardCodedAzp = createHardcodedClaim("hard", "aud", "invalid-aud", ProviderConfigProperty.LIST_TYPE, true, true, true);
        clientResource.getProtocolMappers().createMapper(hardCodedAzp);

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        errorPage.assertCurrent();
    }

    @Test
    public void testIdPNotFound() {
        final String notExistingIdP = "not-exists";
        final String realmName = realmsResouce().realm(bc.providerRealmName()).toRepresentation().getRealm();
        assertThat(realmName, notNullValue());
        final String LINK = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + realmName + "/broker/" + notExistingIdP + "/endpoint";

        driver.navigate().to(LINK);

        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Page not found"));

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            SimpleHttpResponse simple = SimpleHttpDefault.doGet(LINK, client).asResponse();
            assertThat(simple, notNullValue());
            assertThat(simple.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));

            OAuth2ErrorRepresentation error = simple.asJson(OAuth2ErrorRepresentation.class);
            assertThat(error, notNullValue());
            assertThat(error.getError(), is("Identity Provider [" + notExistingIdP + "] not found."));
        } catch (IOException ex) {
            Assert.fail("Cannot create HTTP client. Details: " + ex.getMessage());
        }
    }

    @Test
    public void testIdPForceSyncUserAttributes() {
        checkUpdatedUserAttributesIdP(true, false);
    }

    @Test
    public void testIdPForceSyncTrustEmailUserAttributes() {
        checkUpdatedUserAttributesIdP(true, true);
    }

    @Test
    public void testIdPNotForceSyncUserAttributes() {
        checkUpdatedUserAttributesIdP(false, false);
    }

    @Test
    public void testIdPNotForceSyncTrustEmailUserAttributes() {
        checkUpdatedUserAttributesIdP(false, true);
    }

    @Test
    public void testTrustEmailBasedOnEmailVerifiedClaimSyncModeForce() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        List<UserRepresentation> users = providerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        UserRepresentation providerUser = users.get(0);
        assertThat(providerUser.isEmailVerified(), is(true));
        // first broker login
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = consumerRealm.toRepresentation();
        realmRep.setVerifyEmail(true);
        consumerRealm.update(realmRep);
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        idpRep.setTrustEmail(true);
        idpRep.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.name());
        identityProviderResource.update(idpRep);
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
        users = consumerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        List<String> requiredActions = users.get(0).getRequiredActions();
        assertEquals(0, requiredActions.size());
        assertThat(users.get(0).isEmailVerified(), is(true));

        // logout
        AccountHelper.logout(consumerRealm, bc.getUserLogin());
        AccountHelper.logout(providerRealm, bc.getUserLogin());

        // set the email to not verified at the provider realm
        providerUser.setEmailVerified(false);
        providerRealm.users().get(providerUser.getId()).update(providerUser);

        // user is forced to verify email because the account at the provider realm did not verify the email
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        users = consumerRealm.users().search(bc.getUserLogin());
        assertThat(users.get(0).isEmailVerified(), is(false));
        assertThat(appPage.isCurrent(), is(false));

        // set the email to verified at the provider realm to trust the verification and update the account at the consumer realm
        providerUser.setEmailVerified(true);
        providerRealm.users().get(providerUser.getId()).update(providerUser);
        AccountHelper.logout(consumerRealm, bc.getUserLogin());
        AccountHelper.logout(providerRealm, bc.getUserLogin());
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        appPage.assertCurrent();
    }

    @Test
    public void testVerifyEmailWhenUpdateProfileAndEmailVerifiedAtIdP() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        List<UserRepresentation> users = providerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        UserRepresentation providerUser = users.get(0);
        assertThat(providerUser.isEmailVerified(), is(true));
        // first broker login
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = consumerRealm.toRepresentation();
        realmRep.setVerifyEmail(true);
        consumerRealm.update(realmRep);
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        idpRep.setTrustEmail(true);
        idpRep.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.name());
        identityProviderResource.update(idpRep);
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("updated@keycloak.org", "FirstName", "LastName");
        users = consumerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        List<String> requiredActions = users.get(0).getRequiredActions();
        assertEquals(1, requiredActions.size());
        // email updated by the user, must verify the email
        assertThat(users.get(0).getEmail(), is("updated@keycloak.org"));
    }

    @Test
    public void testVerifyEmailWhenUpdateProfileSameEmailAndEmailVerifiedAtIdP() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        List<UserRepresentation> users = providerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        UserRepresentation providerUser = users.get(0);
        assertThat(providerUser.isEmailVerified(), is(true));
        // first broker login
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = consumerRealm.toRepresentation();
        realmRep.setVerifyEmail(true);
        consumerRealm.update(realmRep);
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        idpRep.setTrustEmail(true);
        idpRep.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.name());
        identityProviderResource.update(idpRep);
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation(bc.getUserEmail(), "FirstName", "LastName");
        users = consumerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        List<String> requiredActions = users.get(0).getRequiredActions();
        assertEquals(0, requiredActions.size());
        // email updated by the user, must verify the email
        assertThat(users.get(0).getEmail(), is(bc.getUserEmail()));
    }

    @Test
    public void testTrustEmailBasedOnEmailVerifiedClaimSyncModeImport() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        List<UserRepresentation> users = providerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        UserRepresentation providerUser = users.get(0);
        assertThat(providerUser.isEmailVerified(), is(true));
        // first broker login
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = consumerRealm.toRepresentation();
        realmRep.setVerifyEmail(true);
        consumerRealm.update(realmRep);
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        idpRep.setTrustEmail(true);
        idpRep.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.IMPORT.name());
        identityProviderResource.update(idpRep);
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
        users = consumerRealm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        List<String> requiredActions = users.get(0).getRequiredActions();
        assertEquals(0, requiredActions.size());
        assertThat(users.get(0).isEmailVerified(), is(true));

        // logout
        AccountHelper.logout(consumerRealm, bc.getUserLogin());
        AccountHelper.logout(providerRealm, bc.getUserLogin());

        // set the email to not verified at the provider realm to make sure email is still verified at the consumer realm because of import sync mode
        providerUser.setEmailVerified(false);
        providerRealm.users().get(providerUser.getId()).update(providerUser);
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        appPage.assertCurrent();
    }

    @Test
    public void loginWithClaimFilter() {
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        IdentityProviderRepresentation identityProvider = identityProviderResource.toRepresentation();
        updateIdPClaimFilter(identityProvider, identityProviderResource, true, USER_ATTRIBUTE_NAME, USER_ATTRIBUTE_VALUE);

        WaitUtils.waitForPageToLoad();

        loginFetchingUserFromUserEndpoint();

        UserRepresentation user = getFederatedIdentity();

        Assert.assertNotNull(user);
    }

    @Test
    public void loginWithClaimRegexpFilter() {
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        IdentityProviderRepresentation identityProvider = identityProviderResource.toRepresentation();
        updateIdPClaimFilter(identityProvider, identityProviderResource, true, USER_ATTRIBUTE_NAME, CLAIM_FILTER_REGEXP);

        WaitUtils.waitForPageToLoad();

        loginFetchingUserFromUserEndpoint();

        UserRepresentation user = getFederatedIdentity();

        Assert.assertNotNull(user);
    }

    @Test
    public void denyLoginWithClaimFilter() {
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        IdentityProviderRepresentation identityProvider = identityProviderResource.toRepresentation();
        updateIdPClaimFilter(identityProvider, identityProviderResource, true, "hardcoded-missing-claim", "hardcoded-missing-claim-value");
        WaitUtils.waitForPageToLoad();

        loginFetchingUserFromUserEndpoint(true);
        Assert.assertEquals("The ID token issued by the identity provider does not match the configured essential claim. Please contact your administrator.",
                loginPage.getInstruction());


        List<UserRepresentation> users = realmsResouce().realm(bc.consumerRealmName()).users().search(bc.getUserLogin());
        assertThat(users, Matchers.empty());
    }

    protected void postInitializeUser(UserRepresentation user) {
        user.setAttributes(ImmutableMap.<String, List<String>>builder()
                .put(USER_ATTRIBUTE_NAME, ImmutableList.<String>builder().add(USER_ATTRIBUTE_VALUE).build())
                .build());
    }


    private void updateIdPClaimFilter(IdentityProviderRepresentation idProvider, IdentityProviderResource idProviderResource, boolean filteredByClaim, String claimFilterName, String claimFilterValue) {
        assertThat(idProvider, Matchers.notNullValue());
        assertThat(idProviderResource, Matchers.notNullValue());
        assertThat(claimFilterName, Matchers.notNullValue());
        assertThat(claimFilterValue, Matchers.notNullValue());

        if (idProvider.getConfig().getOrDefault(IdentityProviderModel.FILTERED_BY_CLAIMS, "false").equals(Boolean.toString(filteredByClaim)) &&
                idProvider.getConfig().getOrDefault(IdentityProviderModel.CLAIM_FILTER_NAME, "").equals(claimFilterName) &&
                idProvider.getConfig().getOrDefault(IdentityProviderModel.CLAIM_FILTER_VALUE, "").equals(claimFilterValue)
        ) {
            return;
        }

        idProvider.getConfig().put(IdentityProviderModel.FILTERED_BY_CLAIMS, Boolean.toString(filteredByClaim));
        idProvider.getConfig().put(IdentityProviderModel.CLAIM_FILTER_NAME, claimFilterName);
        idProvider.getConfig().put(IdentityProviderModel.CLAIM_FILTER_VALUE, claimFilterValue);
        idProviderResource.update(idProvider);

        idProvider = idProviderResource.toRepresentation();
        assertThat("Cannot get Identity Provider", idProvider, Matchers.notNullValue());
        assertThat("Filtered by claim didn't change", idProvider.getConfig().get(IdentityProviderModel.FILTERED_BY_CLAIMS), Matchers.equalTo(Boolean.toString(filteredByClaim)));
        assertThat("Claim name didn't change", idProvider.getConfig().get(IdentityProviderModel.CLAIM_FILTER_NAME), Matchers.equalTo(claimFilterName));
        assertThat("Claim value didn't change", idProvider.getConfig().get(IdentityProviderModel.CLAIM_FILTER_VALUE), Matchers.equalTo(claimFilterValue));
    }

    private void checkUpdatedUserAttributesIdP(boolean isForceSync, boolean isTrustEmail) {
        final String IDP_NAME = getBrokerConfiguration().getIDPAlias();
        final String USERNAME = "demo-user";
        final String PASSWORD = "demo-pwd";
        final String NEW_USERNAME = "demo-user-new";

        final String FIRST_NAME = "John";
        final String LAST_NAME = "Doe";
        final String EMAIL = "mail@example.com";

        final String NEW_FIRST_NAME = "Jack";
        final String NEW_LAST_NAME = "Doee";
        final String NEW_EMAIL = "mail123@example.com";

        RealmResource providerRealmResource = realmsResouce().realm(bc.providerRealmName());
        allowUserEdit(providerRealmResource);

        UsersResource providerUsersResource = providerRealmResource.users();

        String providerUserID = createUser(bc.providerRealmName(), USERNAME, PASSWORD, FIRST_NAME, LAST_NAME, EMAIL,
                user -> user.setEmailVerified(true));
        UserResource providerUserResource = providerUsersResource.get(providerUserID);

        try {
            IdentityProviderResource consumerIdentityResource = getIdentityProviderResource();
            IdentityProviderRepresentation idProvider = consumerIdentityResource.toRepresentation();

            updateIdPSyncMode(idProvider, consumerIdentityResource,
                    isForceSync ? IdentityProviderSyncMode.FORCE : IdentityProviderSyncMode.IMPORT, isTrustEmail);

            // login to create the user in the consumer realm
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            WaitUtils.waitForPageToLoad();

            assertThat(driver.getTitle(), Matchers.containsString("Sign in to " + bc.consumerRealmName()));
            logInWithIdp(IDP_NAME, USERNAME, PASSWORD);

            UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.providerRealmName()), USERNAME);

            assertThat(userRepresentation.getUsername(), Matchers.equalTo(USERNAME));
            assertThat(userRepresentation.getEmail(), Matchers.equalTo(EMAIL));
            assertThat(userRepresentation.getFirstName(), Matchers.equalTo(FIRST_NAME));
            assertThat(userRepresentation.getLastName(), Matchers.equalTo(LAST_NAME));

            RealmResource consumerRealmResource = realmsResouce().realm(bc.consumerRealmName());
            List<UserRepresentation> foundUsers = consumerRealmResource.users().searchByUsername(USERNAME, true);
            assertThat(foundUsers, Matchers.hasSize(1));
            UserRepresentation consumerUser = foundUsers.get(0);
            assertThat(consumerUser, Matchers.notNullValue());
            String consumerUserID = consumerUser.getId();
            UserResource consumerUserResource = consumerRealmResource.users().get(consumerUserID);

            checkFederatedIdentityLink(consumerUserResource, providerUserID, USERNAME);
            assertThat(consumerUserResource.toRepresentation().isEmailVerified(), Matchers.equalTo(isTrustEmail));

            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), USERNAME);
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), USERNAME);

            // set email verified to true on the consumer resource
            consumerUser = consumerUserResource.toRepresentation();
            consumerUser.setEmailVerified(true);
            consumerUserResource.update(consumerUser);
            consumerUserResource = consumerRealmResource.users().get(consumerUserID);
            assertThat(consumerUserResource.toRepresentation().isEmailVerified(), Matchers.is(true));

            // modify provider user with the new values
            UserRepresentation providerUser = providerUserResource.toRepresentation();
            providerUser.setUsername(NEW_USERNAME);
            providerUser.setFirstName(NEW_FIRST_NAME);
            providerUser.setLastName(NEW_LAST_NAME);
            providerUser.setEmail(NEW_EMAIL);
            providerUser.setEmailVerified(true);
            providerUserResource.update(providerUser);

            // login again to force sync if force mode
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            WaitUtils.waitForPageToLoad();

            assertThat(driver.getTitle(), Matchers.containsString("Sign in to " + bc.consumerRealmName()));
            logInWithIdp(IDP_NAME, NEW_USERNAME, PASSWORD);

            userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), USERNAME);

            // consumer username stays the same, even when sync mode is force
            assertThat(userRepresentation.getUsername(), Matchers.equalTo(USERNAME));
            // other consumer attributes are updated, when sync mode is force
            assertThat(userRepresentation.getEmail(), Matchers.equalTo(isForceSync ? NEW_EMAIL : EMAIL));
            assertThat(userRepresentation.getFirstName(), Matchers.equalTo(isForceSync ? NEW_FIRST_NAME : FIRST_NAME));
            assertThat(userRepresentation.getLastName(), Matchers.equalTo(isForceSync ? NEW_LAST_NAME : LAST_NAME));

            consumerUserResource = consumerRealmResource.users().get(consumerUserID);
            checkFederatedIdentityLink(consumerUserResource, providerUserID, isForceSync ? NEW_USERNAME : USERNAME);
            // the email verified should be reverted to false if force-sync and not trust-email
            assertThat(consumerUserResource.toRepresentation().isEmailVerified(), Matchers.equalTo(!isForceSync || isTrustEmail));
        } finally {
            providerUsersResource.delete(providerUserID).close();
        }
    }

    @Test
    public void checkUpdatedEmailAttributeIdPSameValueDifferentCase() throws Exception {
        final String IDP_NAME = getBrokerConfiguration().getIDPAlias();
        final String USERNAME = "demo-user";
        final String PASSWORD = "demo-pwd";

        final String FIRST_NAME = "John";
        final String LAST_NAME = "Doe";
        final String EMAIL = "mail@example.com";

        RealmResource providerRealmResource = realmsResouce().realm(bc.providerRealmName());
        allowUserEdit(providerRealmResource);

        UsersResource providerUsersResource = providerRealmResource.users();

        String providerUserID = createUser(bc.providerRealmName(), USERNAME, PASSWORD, FIRST_NAME, LAST_NAME, EMAIL,
                user -> user.setEmailVerified(true));

        try {
            IdentityProviderResource consumerIdentityResource = getIdentityProviderResource();
            IdentityProviderRepresentation idProvider = consumerIdentityResource.toRepresentation();

            updateIdPSyncMode(idProvider, consumerIdentityResource, IdentityProviderSyncMode.FORCE, false);

            // login to create the user in the consumer realm
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            WaitUtils.waitForPageToLoad();

            assertThat(driver.getTitle(), Matchers.containsString("Sign in to " + bc.consumerRealmName()));
            logInWithIdp(IDP_NAME, USERNAME, PASSWORD);

            UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.providerRealmName()), USERNAME);

            assertThat(userRepresentation.getUsername(), Matchers.equalTo(USERNAME));
            assertThat(userRepresentation.getEmail(), Matchers.equalTo(EMAIL));
            assertThat(userRepresentation.getFirstName(), Matchers.equalTo(FIRST_NAME));
            assertThat(userRepresentation.getLastName(), Matchers.equalTo(LAST_NAME));

            RealmResource consumerRealmResource = realmsResouce().realm(bc.consumerRealmName());
            List<UserRepresentation> foundUsers = consumerRealmResource.users().searchByUsername(USERNAME, true);
            assertThat(foundUsers, Matchers.hasSize(1));
            UserRepresentation consumerUser = foundUsers.get(0);
            assertThat(consumerUser, Matchers.notNullValue());
            String consumerUserID = consumerUser.getId();
            UserResource consumerUserResource = consumerRealmResource.users().get(consumerUserID);

            checkFederatedIdentityLink(consumerUserResource, providerUserID, USERNAME);
            Assert.assertFalse(consumerUserResource.toRepresentation().isEmailVerified());

            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), USERNAME);
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), USERNAME);

            // set email verified to true on the consumer resource
            consumerUser = consumerUserResource.toRepresentation();
            consumerUser.setEmailVerified(true);
            consumerUserResource.update(consumerUser);
            Assert.assertTrue(consumerUserResource.toRepresentation().isEmailVerified());

            // Change the client scope for email to set the hardcoded email in capitals
            ProtocolMapperRepresentation hardcodedEmail = new ProtocolMapperRepresentation();
            hardcodedEmail.setName("email");
            hardcodedEmail.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            hardcodedEmail.setProtocolMapper(HardcodedClaim.PROVIDER_ID);
            hardcodedEmail.getConfig().put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "email");
            hardcodedEmail.getConfig().put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
            hardcodedEmail.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            hardcodedEmail.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            hardcodedEmail.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
            hardcodedEmail.getConfig().put(HardcodedClaim.CLAIM_VALUE, EMAIL.toUpperCase());
            ClientScopeResource emailClientScope = ApiUtil.findClientScopeByName(providerRealmResource, "email");
            ProtocolMapperRepresentation emailMapper = ApiUtil.findProtocolMapperByName(emailClientScope, "email");
            emailClientScope.getProtocolMappers().delete(emailMapper.getId());
            emailClientScope.getProtocolMappers().createMapper(hardcodedEmail).close();

            // login again to force sync
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            WaitUtils.waitForPageToLoad();

            assertThat(driver.getTitle(), Matchers.containsString("Sign in to " + bc.consumerRealmName()));
            logInWithIdp(IDP_NAME, USERNAME, PASSWORD);
            assertThat(driver.getCurrentUrl(), Matchers.containsString("/app/auth?"));

            consumerUserResource = consumerRealmResource.users().get(consumerUserID);
            checkFederatedIdentityLink(consumerUserResource, providerUserID, USERNAME);

            // the email should be verified as it's just a different case
            Assert.assertTrue(consumerUserResource.toRepresentation().isEmailVerified());
        } finally {
            providerUsersResource.delete(providerUserID).close();
        }
    }

    private void allowUserEdit(RealmResource realmResource) {
        RealmRepresentation realm = realmResource.toRepresentation();
        realm.setEditUsernameAllowed(true);
        realmResource.update(realm);
    }

    private void checkFederatedIdentityLink(UserResource userResource, String userID, String username) {
        List<FederatedIdentityRepresentation> federatedIdentities = userResource.getFederatedIdentity();
        assertThat(federatedIdentities, Matchers.hasSize(1));
        FederatedIdentityRepresentation federatedIdentity = federatedIdentities.get(0);
        assertThat(federatedIdentity.getIdentityProvider(), Matchers.equalTo(IDP_OIDC_ALIAS));
        assertThat(federatedIdentity.getUserId(), Matchers.equalTo(userID));
        assertThat(federatedIdentity.getUserName(), Matchers.equalTo(username));
    }

    private void updateIdPSyncMode(IdentityProviderRepresentation idProvider, IdentityProviderResource idProviderResource,
                                   IdentityProviderSyncMode syncMode, Boolean trustEmail) {
        assertThat(idProvider, Matchers.notNullValue());
        assertThat(idProviderResource, Matchers.notNullValue());
        assertThat(syncMode, Matchers.notNullValue());

        if (idProvider.getConfig().get(IdentityProviderModel.SYNC_MODE).equals(syncMode.name())
                && trustEmail.equals(idProvider.isTrustEmail())) {
            return;
        }

        idProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, syncMode.name());
        idProvider.setTrustEmail(trustEmail);
        idProviderResource.update(idProvider);

        idProvider = idProviderResource.toRepresentation();
        assertThat("Cannot get Identity Provider", idProvider, Matchers.notNullValue());
        assertThat("Sync mode didn't change", idProvider.getConfig().get(IdentityProviderModel.SYNC_MODE), Matchers.equalTo(syncMode.name()));
        assertThat("TrustEmail didn't change", idProvider.isTrustEmail(), Matchers.equalTo(trustEmail));
    }

    private UserRepresentation getFederatedIdentity() {
        List<UserRepresentation> users = realmsResouce().realm(bc.consumerRealmName()).users().search(bc.getUserLogin());

        Assert.assertEquals(1, users.size());

        return users.get(0);
    }

    private IdentityProviderResource getIdentityProviderResource() {
        return realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
    }

    private static final CustomKcOidcBrokerConfiguration BROKER_CONFIG_INSTANCE = new CustomKcOidcBrokerConfiguration();

    static class CustomKcOidcBrokerConfiguration extends KcOidcBrokerConfiguration {

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clients = super.createProviderClients();

            ClientRepresentation client = clients.get(0);
            ProtocolMapperRepresentation userAttrMapper = new ProtocolMapperRepresentation();
            userAttrMapper.setName(USER_ATTRIBUTE_NAME);
            userAttrMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            userAttrMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

            Map<String, String> userAttrMapperConfig = userAttrMapper.getConfig();
            userAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, USER_ATTRIBUTE_NAME);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, USER_ATTRIBUTE_NAME);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
            userAttrMapperConfig.put(ProtocolMapperUtils.MULTIVALUED, "false");
            userAttrMapperConfig.put(ProtocolMapperUtils.AGGREGATE_ATTRS, "false");
            List<ProtocolMapperRepresentation> mappers = new ArrayList<>(client.getProtocolMappers());
            mappers.add(userAttrMapper);
            client.setProtocolMappers(mappers);

            return clients;
        }
    }
}
