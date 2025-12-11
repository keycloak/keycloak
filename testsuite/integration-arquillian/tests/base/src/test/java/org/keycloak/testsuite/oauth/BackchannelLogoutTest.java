package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.LogoutTokenValidationCode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.broker.AbstractNestedBrokerTest;
import org.keycloak.testsuite.broker.NestedBrokerConfiguration;
import org.keycloak.testsuite.broker.OidcBackchannelLogoutBrokerConfiguration;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.LogoutTokenUtil;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.BackchannelLogoutResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BackchannelLogoutTest extends AbstractNestedBrokerTest {

    public static final String ACCOUNT_CLIENT_NAME = "account";
    public static final String BROKER_CLIENT_ID = "brokerapp";
    public static final String USER_PASSWORD_CONSUMER_REALM = "password";
    private static final KeyPair KEY_PAIR = KeyUtils.generateRsaKeyPair(2048);
    private String userIdProviderRealm;
    private String realmIdConsumerRealm;
    private String accountClientIdConsumerRealm;
    private String accountClientIdSubConsumerRealm;
    private String providerId;

    private RealmManager providerRealmManager;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Drone
    @SecondBrowser
    WebDriver driver2;

    @Override
    protected NestedBrokerConfiguration getNestedBrokerConfiguration() {
        return OidcBackchannelLogoutBrokerConfiguration.INSTANCE;
    }

    @Before
    public void createProviderRealmUser() {
        log.debug("creating user for realm " + nbc.providerRealmName());

        final UserRepresentation userProviderRealm = new UserRepresentation();
        userProviderRealm.setUsername(nbc.getUserLogin());
        userProviderRealm.setEmail(nbc.getUserEmail());
        userProviderRealm.setFirstName("f");
        userProviderRealm.setLastName("l");
        userProviderRealm.setEmailVerified(true);
        userProviderRealm.setEnabled(true);

        final RealmResource realmResource = adminClient.realm(nbc.providerRealmName());
        userIdProviderRealm = createUserWithAdminClient(realmResource, userProviderRealm);

        resetUserPassword(realmResource.users().get(userIdProviderRealm), nbc.getUserPassword(), false);
    }

    @Before
    public void addIdentityProviders() {
        log.debug("adding identity provider to realm " + nbc.consumerRealmName());
        RealmResource realm = adminClient.realm(nbc.consumerRealmName());
        realm.identityProviders().create(nbc.setUpIdentityProvider()).close();

        log.debug("adding identity provider to realm " + nbc.subConsumerRealmName());
        realm = adminClient.realm(nbc.subConsumerRealmName());
        realm.identityProviders().create(nbc.setUpConsumerIdentityProvider()).close();
    }

    @Before
    public void addClients() {
        addClientsToProviderAndConsumer();
        fetchConsumerRealmDetails();
    }

    private void fetchConsumerRealmDetails() {
        RealmResource realmResourceConsumerRealm = adminClient.realm(nbc.consumerRealmName());
        realmIdConsumerRealm = realmResourceConsumerRealm.toRepresentation().getId();
        accountClientIdConsumerRealm =
                adminClient.realm(nbc.consumerRealmName()).clients().findByClientId(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID).get(0).getId();

        RealmResource realmResourceSubConsumerRealm = adminClient.realm(nbc.subConsumerRealmName());
        accountClientIdSubConsumerRealm =
                adminClient.realm(nbc.subConsumerRealmName()).clients().findByClientId(ACCOUNT_CLIENT_NAME).get(0)
                        .getId();
    }

    @Before
    public void createNewRsaKeyForProviderRealm() {
        providerRealmManager = RealmManager.realm(adminClient.realm(nbc.providerRealmName()));
        providerId = providerRealmManager.generateNewRsaKey(KEY_PAIR, "rsa-test-2");
    }

    @Test
    public void postBackchannelLogoutWithSessionId() throws Exception {
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);
        logInAsUserInIDP(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionIdConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        assertConsumerLogoutEvent(sessionIdConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionIdProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutSessionId() throws Exception {
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        logInAsUserInIDP(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionIdConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        assertConsumerLogoutEvent(sessionIdConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionIdProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutLogoutToken() throws Exception {
        oauth.realm(nbc.consumerRealmName());
        BackchannelLogoutResponse response = oauth.doBackchannelLogout(null);
        assertEquals(400, response.getStatusCode());
        assertEquals("No logout token", response.getErrorDescription());
        events.expectLogoutError(Errors.INVALID_TOKEN)
                .realm(realmIdConsumerRealm)
                .assertEvent();
    }

    @Test
    public void postBackchannelLogoutWithInvalidLogoutToken() throws Exception {
        String logoutTokenMissingContent =
                Base64Url.encode(JsonSerialization.writeValueAsBytes(JsonSerialization.createObjectNode()));

        oauth.realm(nbc.consumerRealmName());
        BackchannelLogoutResponse response = oauth.doBackchannelLogout(logoutTokenMissingContent);
        assertEquals(400, response.getStatusCode());
        assertEquals(LogoutTokenValidationCode.DECODE_TOKEN_FAILED.getErrorMessage(), response.getErrorDescription());
        events.expectLogoutError(Errors.INVALID_TOKEN)
                .realm(realmIdConsumerRealm)
                .assertEvent();
    }

    @Test
    public void postBackchannelLogoutWithSessionIdUserNotLoggedIn() throws Exception {
        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, UUID.randomUUID().toString());

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdUserNotLoggedIn() throws Exception {
        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdUserDoesntExist() throws Exception {
        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(UUID.randomUUID().toString());

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());
    }

    @Test
    public void postBackchannelLogoutWithSessionIdMultipleOpenSession() throws Exception {
        logInAsUserInIDP(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String userIdConsumerRealm = getUserIdConsumerRealm();
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        OAuthClient oauth2 = oauth.newConfig().driver(driver2);
        oauth2.realm(nbc.consumerRealmName())
                .clientId(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID)
                .redirectUri(getAuthServerRoot() + "realms/" + nbc.consumerRealmName() + "/app");

        doLoginSocial(oauth2, nbc.getIDPAlias(), nbc.getUserLogin(), nbc.getUserPassword());

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionId1ProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        assertConsumerLogoutEvent(sessionId1ConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdMultipleOpenSession() throws Exception {
        logInAsUserInIDP(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String userIdConsumerRealm = getUserIdConsumerRealm();
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        loginWithSecondBrowser(nbc.getIDPAlias());

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        List<String> expectedSessionIdsInLogoutEvents = Arrays.asList(sessionId1ConsumerRealm, sessionId2ConsumerRealm);
        assertConsumerLogoutEvents(expectedSessionIdsInLogoutEvents, userIdConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithSessionIdMultipleOpenSessionDifferentIdentityProvider() throws Exception {

        IdentityProviderRepresentation identityProvider2 = addSecondIdentityProviderToConsumerRealm();
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        logInAsUserInIDP(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String userIdConsumerRealm = getUserIdConsumerRealm();
        adminClient.realm(nbc.consumerRealmName()).users().get(userIdConsumerRealm)
                .resetPassword(CredentialBuilder.create().password(USER_PASSWORD_CONSUMER_REALM).build());

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        OAuthClient oauth2 = loginWithSecondBrowser(identityProvider2.getDisplayName());
        linkUsers(oauth2);

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionId1ProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        assertConsumerLogoutEvent(sessionId1ConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdMultipleOpenSessionDifferentIdentityProvider() throws Exception {
        IdentityProviderRepresentation identityProvider2 = addSecondIdentityProviderToConsumerRealm();
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        logInAsUserInIDP(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String userIdConsumerRealm = getUserIdConsumerRealm();
        adminClient.realm(nbc.consumerRealmName()).users().get(userIdConsumerRealm)
                .resetPassword(CredentialBuilder.create().password(USER_PASSWORD_CONSUMER_REALM).build());

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        OAuthClient oauth2 = loginWithSecondBrowser(identityProvider2.getDisplayName());
        linkUsers(oauth2);

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        List<String> expectedSessionIdsInLogoutEvents = Arrays.asList(sessionId1ConsumerRealm, sessionId2ConsumerRealm);
        assertConsumerLogoutEvents(expectedSessionIdsInLogoutEvents, userIdConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertNoSessionsInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutOnDisabledClientReturnsNotImplemented() throws Exception {
        logInAsUserInIDP(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionIdConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(nbc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm);

        disableClient(nbc.consumerRealmName(), accountClientIdConsumerRealm);

        oauth.realm(nbc.consumerRealmName());
        BackchannelLogoutResponse response = oauth.doBackchannelLogout(logoutTokenEncoded);
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());

        assertLogoutErrorEvent(nbc.consumerRealmName());
    }

    @Test
    public void postBackchannelLogoutNestedBrokering() throws Exception {
        String consumerClientId = getClientId(nbc.consumerRealmName(), OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        logInAsUserInNestedIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();
        String userIdSubConsumerRealm = getUserIdSubConsumerRealm();
        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);

        String sessionIdConsumerRealm = assertConsumerLoginEvent(userIdConsumerRealm, OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        assertActiveSessionInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String sessionIdSubConsumerRealm =
                assertLoginEvent(userIdSubConsumerRealm, ACCOUNT_CLIENT_NAME, nbc.subConsumerRealmName());
        assertActiveSessionInClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm, userIdSubConsumerRealm,
                sessionIdSubConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        assertConsumerLogoutEvent(sessionIdConsumerRealm, userIdConsumerRealm);
        assertLogoutEvent(sessionIdSubConsumerRealm, userIdSubConsumerRealm, nbc.subConsumerRealmName());

        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertNoSessionsInClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm, userIdSubConsumerRealm,
                sessionIdSubConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionIdProviderRealm);
    }

    @Test
    public void postBackchannelLogoutNestedBrokeringDownstreamLogoutOfSubConsumerFails() throws Exception {
        String consumerClientId = getClientId(nbc.consumerRealmName(), OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);

        logInAsUserInNestedIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();
        String userIdSubConsumerRealm = getUserIdSubConsumerRealm();
        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);

        String sessionIdConsumerRealm = assertConsumerLoginEvent(userIdConsumerRealm, OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        assertActiveSessionInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String sessionIdSubConsumerRealm =
                assertLoginEvent(userIdSubConsumerRealm, ACCOUNT_CLIENT_NAME, nbc.subConsumerRealmName());
        assertActiveSessionInClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm, userIdSubConsumerRealm,
                sessionIdSubConsumerRealm);

        disableClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm);

        oauth.realm(nbc.consumerRealmName());
        BackchannelLogoutResponse response = oauth.doBackchannelLogout(logoutTokenEncoded);
        assertEquals(Response.Status.GATEWAY_TIMEOUT.getStatusCode(), response.getStatusCode());

        assertLogoutErrorEvent(nbc.subConsumerRealmName());
        assertConsumerLogoutEvent(sessionIdConsumerRealm, userIdConsumerRealm);

        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
    }

    @Test
    public void postBackchannelLogoutNestedBrokeringRevokeOfflineSessions() throws Exception {
        String consumerClientId = getClientId(nbc.consumerRealmName(), OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        subConsumerIdpRequestsOfflineSessions();

        logInAsUserInNestedIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();
        String userIdSubConsumerRealm = getUserIdSubConsumerRealm();
        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);

        String sessionIdConsumerRealm = assertConsumerLoginEvent(userIdConsumerRealm, OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveOfflineSessionInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm);

        String sessionIdSubConsumerRealm =
                assertLoginEvent(userIdSubConsumerRealm, ACCOUNT_CLIENT_NAME, nbc.subConsumerRealmName());
        assertActiveSessionInClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm, userIdSubConsumerRealm,
                sessionIdSubConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm, true);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        // no logout event as there is no online session now
        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertNoOfflineSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm);
        assertActiveSessionInClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm, userIdSubConsumerRealm,
                sessionIdSubConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionIdProviderRealm);
    }

    @Test
    public void postBackchannelLogoutNestedBrokeringDoNotRevokeOfflineSessions() throws Exception {
        String consumerClientId = getClientId(nbc.consumerRealmName(), OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        String brokerClientIdProviderRealm = getClientId(nbc.providerRealmName(), BROKER_CLIENT_ID);

        subConsumerIdpRequestsOfflineSessions();

        logInAsUserInNestedIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();
        String userIdSubConsumerRealm = getUserIdSubConsumerRealm();
        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);

        String sessionIdConsumerRealm = assertConsumerLoginEvent(userIdConsumerRealm, OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveOfflineSessionInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm);

        String sessionIdSubConsumerRealm =
                assertLoginEvent(userIdSubConsumerRealm, ACCOUNT_CLIENT_NAME, nbc.subConsumerRealmName());
        assertActiveSessionInClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm, userIdSubConsumerRealm,
                sessionIdSubConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm, false);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        // no logout event as there is no online session now
        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveOfflineSessionInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm);
        assertActiveSessionInClient(nbc.subConsumerRealmName(), accountClientIdSubConsumerRealm, userIdSubConsumerRealm,
                sessionIdSubConsumerRealm);
        assertActiveSessionInClient(nbc.providerRealmName(), brokerClientIdProviderRealm, userIdProviderRealm,
                sessionIdProviderRealm);
    }

    @Test
    public void postBackchannelLogoutNestedBrokeringRevokeOfflineSessionsWithoutActiveUserSession() throws Exception {
        String consumerClientId =
                getClientId(nbc.consumerRealmName(), OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        subConsumerIdpRequestsOfflineSessions();

        logInAsUserInNestedIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();
        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);

        String sessionIdConsumerRealm = assertConsumerLoginEvent(userIdConsumerRealm,
                OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveOfflineSessionInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm);

        executeLogoutFromRealm(getConsumerRoot(), nbc.consumerRealmName(), null, null, OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID, null);
        confirmLogout();
        assertNoSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveOfflineSessionInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm, true);

        oauth.realm(nbc.consumerRealmName());
        assertTrue(oauth.doBackchannelLogout(logoutTokenEncoded).isSuccess());

        assertNoOfflineSessionsInClient(nbc.consumerRealmName(), consumerClientId, userIdConsumerRealm);
    }

    private void subConsumerIdpRequestsOfflineSessions() {
        IdentityProviderResource subConsumerIDPResource = adminClient.realm(nbc.subConsumerRealmName())
                .identityProviders().get(nbc.getSubConsumerIDPDisplayName());

        IdentityProviderRepresentation subConsumerIDP = subConsumerIDPResource.toRepresentation();
        Map<String, String> config = subConsumerIDP.getConfig();
        config.put("defaultScope", config.get("defaultScope") + " " + OAuth2Constants.OFFLINE_ACCESS);

        subConsumerIDPResource.update(subConsumerIDP);
    }

    private String getLogoutTokenEncodedAndSigned(String userId) throws IOException {
        return getLogoutTokenEncodedAndSigned(userId, null);
    }

    private String getLogoutTokenEncodedAndSigned(String userId, String sessionId) throws IOException {
        return getLogoutTokenEncodedAndSigned(userId, sessionId, false);
    }

    private String getLogoutTokenEncodedAndSigned(String userId, String sessionId, boolean revokeOfflineSessions)
            throws IOException {
        String keyId = adminClient.realm(nbc.providerRealmName())
                .keys().getKeyMetadata().getKeys().stream()
                .filter(key -> providerId.equals(key.getProviderId()))
                .findFirst().get()
                .getKid();

        return LogoutTokenUtil.generateSignedLogoutToken(KEY_PAIR.getPrivate(),
                keyId,
                getConsumerRoot() + "/auth/realms/" + nbc.providerRealmName(),
                nbc.getIDPClientIdInProviderRealm(),
                userId,
                sessionId,
                revokeOfflineSessions);
    }

    private String assertConsumerLoginEventAccountManagement(String userIdConsumerRealm) {
        return assertConsumerLoginEvent(userIdConsumerRealm, OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID);
    }

    private String assertConsumerLoginEvent(String userIdConsumerRealm, String clientId) {
        return assertLoginEvent(userIdConsumerRealm, clientId, nbc.consumerRealmName());
    }

    private String assertLoginEvent(String userId, String clientId, String realmName) {
        String sessionId = null;
        String realmId = adminClient.realm(realmName).toRepresentation().getId();

        List<EventRepresentation> eventList = adminClient.realm(realmName).getEvents();

        Optional<EventRepresentation> loginEventOptional = eventList.stream()
                .filter(event -> userId.equals(event.getUserId()))
                .filter(event -> clientId.equals(event.getClientId()))
                .filter(event -> event.getType().equals(EventType.LOGIN.name()))
                .findAny();

        if (loginEventOptional.isPresent()) {
            EventRepresentation loginEvent = loginEventOptional.get();
            this.events.expectLogin()
                    .realm(realmId)
                    .client(clientId)
                    .user(userId)
                    .removeDetail(Details.CODE_ID)
                    .removeDetail(Details.REDIRECT_URI)
                    .removeDetail(Details.CONSENT)
                    .assertEvent(loginEvent);
            sessionId = loginEvent.getSessionId();
        } else {
            fail("No Login event found for user " + userId);
        }

        return sessionId;
    }

    private String assertProviderLoginEventIdpClient(String userIdProviderRealm) {
        return assertLoginEvent(userIdProviderRealm, BROKER_CLIENT_ID, nbc.providerRealmName());
    }

    private void assertConsumerLogoutEvent(String sessionIdConsumerRealm, String userIdConsumerRealm) {
        assertLogoutEvent(sessionIdConsumerRealm, userIdConsumerRealm, nbc.consumerRealmName());
    }

    private void assertLogoutEvent(String sessionId, String userId, String realmName) {

        String realmId = adminClient.realm(realmName).toRepresentation().getId();

        List<EventRepresentation> eventList = adminClient.realm(realmName).getEvents();

        Optional<EventRepresentation> logoutEventOptional = eventList.stream()
                .filter(event -> sessionId.equals(event.getSessionId()))
                .findAny();

        if (logoutEventOptional.isPresent()) {
            EventRepresentation logoutEvent = logoutEventOptional.get();
            this.events.expectLogout(sessionId)
                    .realm(realmId)
                    .user(userId)
                    .client((String) null)
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent(logoutEvent);
        } else {
            fail("No Logout event found for session " + sessionId);
        }
    }

    private void assertLogoutErrorEvent(String realmName) {

        String realmId = adminClient.realm(realmName).toRepresentation().getId();

        List<EventRepresentation> eventList = adminClient.realm(realmName).getEvents();

        Optional<EventRepresentation> logoutErrorEventOptional = eventList.stream()
                .filter(event -> event.getError().equals(Errors.LOGOUT_FAILED))
                .findAny();

        if (logoutErrorEventOptional.isPresent()) {
            EventRepresentation logoutEvent = logoutErrorEventOptional.get();
            this.events.expectLogoutError(Errors.LOGOUT_FAILED)
                    .realm(realmId)
                    .assertEvent(logoutEvent);
        } else {
            fail("No Logout error event found in realm " + realmName);
        }
    }

    private void assertConsumerLogoutEvents(List<String> sessionIdsConsumerRealm, String userIdConsumerRealm) {

        List<EventRepresentation> consumerRealmEvents = adminClient.realm(nbc.consumerRealmName()).getEvents();

        for (String sessionId : sessionIdsConsumerRealm) {
            Optional<EventRepresentation> logoutEventOptional = consumerRealmEvents.stream()
                    .filter(event -> sessionId.equals(event.getSessionId()))
                    .findAny();

            if (logoutEventOptional.isPresent()) {
                EventRepresentation logoutEvent = logoutEventOptional.get();
                this.events.expectLogout(sessionId)
                        .realm(realmIdConsumerRealm)
                        .user(userIdConsumerRealm)
                        .client((String) null)
                        .removeDetail(Details.REDIRECT_URI)
                        .assertEvent(logoutEvent);
            } else {
                fail("No Logout event found for session " + sessionId);
            }
        }
    }

    private String getUserIdConsumerRealm() {
        return getUserId(nbc.consumerRealmName());
    }

    private String getUserIdSubConsumerRealm() {
        return getUserId(nbc.subConsumerRealmName());
    }

    private String getUserId(String realmName) {
        RealmResource realmResourceConsumerRealm = adminClient.realm(realmName);
        return realmResourceConsumerRealm.users().list().get(0).getId();
    }

    private void assertActiveSessionInClient(String realmName, String clientId, String userId,
            String sessionId) {
        List<UserSessionRepresentation> sessions = getClientSessions(realmName, clientId, userId, sessionId);
        assertThat(sessions.size(), is(1));
    }

    private void assertNoSessionsInClient(String realmName, String clientId, String userId, String sessionId) {
        List<UserSessionRepresentation> sessions = getClientSessions(realmName, clientId, userId, sessionId);
        assertThat(sessions.size(), is(0));
    }

    private List<UserSessionRepresentation> getClientSessions(String realmName, String clientUuid, String userId,
            String sessionId) {
        return adminClient.realm(realmName)
                .clients()
                .get(clientUuid)
                .getUserSessions(0, 5)
                .stream()
                .filter(s -> s.getUserId().equals(userId) && s.getId().equals(sessionId))
                .collect(Collectors.toList());
    }

    private void assertActiveOfflineSessionInClient(String realmName, String clientId, String userId) {
        List<UserSessionRepresentation> sessions = getOfflineClientSessions(realmName, clientId, userId);
        assertThat(sessions.size(), is(1));
    }

    private void assertNoOfflineSessionsInClient(String realmName, String clientId, String userId) {
        List<UserSessionRepresentation> sessions = getOfflineClientSessions(realmName, clientId, userId);
        assertThat(sessions.size(), is(0));
    }

    private List<UserSessionRepresentation> getOfflineClientSessions(String realmName, String clientUuid, String userId) {
        return adminClient.realm(realmName)
                .clients()
                .get(clientUuid)
                .getOfflineUserSessions(0, 5)
                .stream()
                .filter(s -> s.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    private IdentityProviderRepresentation addSecondIdentityProviderToConsumerRealm() {
        log.debug("adding second identity provider to realm " + nbc.consumerRealmName());

        IdentityProviderRepresentation identityProvider2 = nbc.setUpIdentityProvider();
        identityProvider2.setAlias(identityProvider2.getAlias() + "2");
        identityProvider2.setDisplayName(identityProvider2.getDisplayName() + "2");
        Map<String, String> config = identityProvider2.getConfig();
        config.put("clientId", BROKER_CLIENT_ID);
        adminClient.realm(nbc.consumerRealmName()).identityProviders().create(identityProvider2).close();

        ClientResource ipdClientResource = getByClientId(nbc.providerRealmName(), nbc.getIDPClientIdInProviderRealm());
        ClientRepresentation clientRepresentation = ipdClientResource.toRepresentation();
        clientRepresentation.getRedirectUris().add(getConsumerRoot() + "/auth/realms/" + nbc.consumerRealmName()
                + "/broker/" + identityProvider2.getAlias() + "/endpoint/*");
        ipdClientResource.update(clientRepresentation);

        return identityProvider2;
    }

    private ClientResource getByClientId(String realmName, String clientId) {
        final ClientsResource c = adminClient.realm(realmName).clients();
        ClientResource ipdClientResource = c.findByClientId(clientId).stream()
          .findAny()
          .map(rep -> c.get(rep.getId()))
          .orElseThrow(IllegalArgumentException::new);
        return ipdClientResource;
    }

    private void disableClient(String realmName, String clientUuid) {
        ClientResource accountClient = adminClient.realm(realmName)
                .clients()
                .get(clientUuid);
        ClientRepresentation clientRepresentation = accountClient.toRepresentation();
        clientRepresentation.setEnabled(false);
        accountClient.update(clientRepresentation);
    }

    private OAuthClient loginWithSecondBrowser(String identityProviderDisplayName) {
        OAuthClient oauth2 = oauth.newConfig().driver(driver2);
        oauth2.realm(nbc.consumerRealmName())
                .clientId(OidcBackchannelLogoutBrokerConfiguration.CONSUMER_CLIENT_ID)
                .redirectUri(getAuthServerRoot() + "realms/" + nbc.consumerRealmName() + "/app");
        doLoginSocial(oauth2, identityProviderDisplayName, nbc.getUserLogin(), nbc.getUserPassword());
        return oauth2;
    }

    private void linkUsers(OAuthClient oauth) {
        WaitUtils.waitForPageToLoad();
        WebElement linkAccountButton = oauth.getDriver().findElement(By.id("linkAccount"));
        waitUntilElement(linkAccountButton).is().clickable();
        linkAccountButton.click();

        WaitUtils.waitForPageToLoad();
        oauth.fillLoginForm(nbc.getUserLogin(), USER_PASSWORD_CONSUMER_REALM);
    }

    private String getClientId(String realm, String clientId) {
        return adminClient.realm(realm).clients().findByClientId(clientId).stream()
         .findAny()
         .map(ClientRepresentation::getId)
         .orElse(null);
   }
}
