package org.keycloak.testsuite.oauth;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.protocol.oidc.LogoutTokenValidationCode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.broker.AbstractBaseBrokerTest;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.BrokerTestTools;
import org.keycloak.testsuite.broker.OidcBackchannelLogoutBrokerConfiguration;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.LogoutTokenUtil;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

public class BackchannelLogoutTest extends AbstractBaseBrokerTest {

    public static final String ACCOUNT_CLIENT_NAME = "account";
    public static final String BROKER_CLIENT_ID = "brokerapp";
    public static final String USER_PASSWORD_CONSUMER_REALM = "password";
    private static final KeyPair KEY_PAIR = KeyUtils.generateRsaKeyPair(2048);
    private String realmIdProviderRealm;
    private String userIdProviderRealm;
    private String realmIdConsumerRealm;
    private String accountClientIdConsumerRealm;
    private String providerId;

    private RealmManager providerRealmManager;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Drone
    @SecondBrowser
    WebDriver driver2;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return OidcBackchannelLogoutBrokerConfiguration.INSTANCE;
    }

    @Before
    public void createProviderRealmUser() {
        log.debug("creating user for realm " + bc.providerRealmName());

        final UserRepresentation userProviderRealm = new UserRepresentation();
        userProviderRealm.setUsername(bc.getUserLogin());
        userProviderRealm.setEmail(bc.getUserEmail());
        userProviderRealm.setEmailVerified(true);
        userProviderRealm.setEnabled(true);

        final RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        userIdProviderRealm = createUserWithAdminClient(realmResource, userProviderRealm);
        realmIdProviderRealm = realmResource.toRepresentation().getId();

        resetUserPassword(realmResource.users().get(userIdProviderRealm), bc.getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToConsumerRealm() {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());

        final RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().create(bc.setUpIdentityProvider()).close();
    }

    @Before
    public void addClients() {
        addClientsToProviderAndConsumer();
    }

    @Before
    public void fetchConsumerRealmDetails() {
        RealmResource realmResourceConsumerRealm = adminClient.realm(bc.consumerRealmName());
        realmIdConsumerRealm = realmResourceConsumerRealm.toRepresentation().getId();
        accountClientIdConsumerRealm =
                adminClient.realm(bc.consumerRealmName()).clients().findByClientId(ACCOUNT_CLIENT_NAME).get(0).getId();
    }

    @Before
    public void createNewRsaKeyForProviderRealm() {
        providerRealmManager = RealmManager.realm(adminClient.realm(bc.providerRealmName()));
        providerId = providerRealmManager.generateNewRsaKey(KEY_PAIR, "rsa-test-2");
    }

    @Test
    public void postBackchannelLogoutWithSessionId() throws Exception {
        logInAsUserInIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionIdConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm);

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }

        assertConsumerLogoutEvent(sessionIdConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionIdProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutSessionId() throws Exception {
        logInAsUserInIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionIdConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }

        assertConsumerLogoutEvent(sessionIdConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionIdProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutLogoutToken() throws Exception {
        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(null)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.BAD_REQUEST));
            assertThat(response, Matchers.bodyHC(containsString("No logout token")));
        }
        events.expectLogoutError(Errors.INVALID_TOKEN)
                .realm(realmIdConsumerRealm)
                .assertEvent();
    }

    @Test
    public void postBackchannelLogoutWithInvalidLogoutToken() throws Exception {
        String logoutTokenMissingContent =
                Base64Url.encode(JsonSerialization.writeValueAsBytes(JsonSerialization.createObjectNode()));

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenMissingContent)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.BAD_REQUEST));
            assertThat(response,
                    Matchers.bodyHC(containsString(LogoutTokenValidationCode.DECODE_TOKEN_FAILED.getErrorMessage())));
        }
        events.expectLogoutError(Errors.INVALID_TOKEN)
                .realm(realmIdConsumerRealm)
                .assertEvent();
    }

    @Test
    public void postBackchannelLogoutWithSessionIdUserNotLoggedIn() throws Exception {
        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, UUID.randomUUID().toString());

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdUserNotLoggedIn() throws Exception {
        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdUserDoesntExist() throws Exception {
        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(UUID.randomUUID().toString());

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }
    }

    @Test
    public void postBackchannelLogoutWithSessionIdMultipleOpenSession() throws Exception {
        logInAsUserInIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        OAuthClient oauth2 = new OAuthClient();
        oauth2.init(driver2);
        oauth2.realm(bc.consumerRealmName())
                .clientId(ACCOUNT_CLIENT_NAME)
                .redirectUri(getAuthServerRoot() + "realms/" + bc.consumerRealmName() + "/account")
                .doLoginSocial(bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionId1ProviderRealm);

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }

        assertConsumerLogoutEvent(sessionId1ConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdMultipleOpenSession() throws Exception {
        logInAsUserInIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        loginWithSecondBrowser(bc.getIDPAlias());

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }

        List<String> expectedSessionIdsInLogoutEvents = Arrays.asList(sessionId1ConsumerRealm, sessionId2ConsumerRealm);
        assertConsumerLogoutEvents(expectedSessionIdsInLogoutEvents, userIdConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithSessionIdMultipleOpenSessionDifferentIdentityProvider() throws Exception {

        IdentityProviderRepresentation identityProvider2 = addSecondIdentityProviderToConsumerRealm();

        logInAsUserInIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();
        adminClient.realm(bc.consumerRealmName()).users().get(userIdConsumerRealm)
                .resetPassword(CredentialBuilder.create().password(USER_PASSWORD_CONSUMER_REALM).build());

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        OAuthClient oauth2 = loginWithSecondBrowser(identityProvider2.getDisplayName());
        linkUsers(oauth2);

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm, BROKER_CLIENT_ID);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionId1ProviderRealm);

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }

        assertConsumerLogoutEvent(sessionId1ConsumerRealm, userIdConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutWithoutSessionIdMultipleOpenSessionDifferentIdentityProvider() throws Exception {

        IdentityProviderRepresentation identityProvider2 = addSecondIdentityProviderToConsumerRealm();

        logInAsUserInIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();
        adminClient.realm(bc.consumerRealmName()).users().get(userIdConsumerRealm)
                .resetPassword(CredentialBuilder.create().password(USER_PASSWORD_CONSUMER_REALM).build());

        String sessionId1ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionId1ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);

        OAuthClient oauth2 = loginWithSecondBrowser(identityProvider2.getDisplayName());
        linkUsers(oauth2);

        String sessionId2ProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm, BROKER_CLIENT_ID);
        String sessionId2ConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm);

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.OK));
        }

        List<String> expectedSessionIdsInLogoutEvents = Arrays.asList(sessionId1ConsumerRealm, sessionId2ConsumerRealm);
        assertConsumerLogoutEvents(expectedSessionIdsInLogoutEvents, userIdConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId1ConsumerRealm);
        assertNoSessionsInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionId2ConsumerRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId1ProviderRealm);
        assertActiveSessionInClient(bc.providerRealmName(), BROKER_CLIENT_ID, userIdProviderRealm,
                sessionId2ProviderRealm);
    }

    @Test
    public void postBackchannelLogoutOnDisabledClientReturnsNotImplemented() throws Exception {
        logInAsUserInIDPForFirstTime();
        String userIdConsumerRealm = getUserIdConsumerRealm();

        String sessionIdProviderRealm = assertProviderLoginEventIdpClient(userIdProviderRealm);
        String sessionIdConsumerRealm = assertConsumerLoginEventAccountManagement(userIdConsumerRealm);
        assertActiveSessionInClient(bc.consumerRealmName(), accountClientIdConsumerRealm, userIdConsumerRealm,
                sessionIdConsumerRealm);

        String logoutTokenEncoded = getLogoutTokenEncodedAndSigned(userIdProviderRealm, sessionIdProviderRealm);

        disableAccountClientConsumerRealm();

        oauth.realm(bc.consumerRealmName());
        try (CloseableHttpResponse response = oauth.doBackchannelLogout(logoutTokenEncoded)) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.NOT_IMPLEMENTED));
            assertThat(response, Matchers.bodyHC(containsString("There was an error in the local logout")));
        }

        events.expectLogoutError(Errors.LOGOUT_FAILED)
                .realm(realmIdConsumerRealm)
                .assertEvent();
    }

    private String getLogoutTokenEncodedAndSigned(String userId) throws IOException {
        return getLogoutTokenEncodedAndSigned(userId, null);
    }

    private String getLogoutTokenEncodedAndSigned(String userId, String sessionId) throws IOException {
        String keyId = adminClient.realm(bc.providerRealmName())
                .keys().getKeyMetadata().getKeys().stream()
                .filter(key -> providerId.equals(key.getProviderId()))
                .findFirst().get()
                .getKid();

        return LogoutTokenUtil.generateSignedLogoutToken(KEY_PAIR.getPrivate(),
                keyId,
                getConsumerRoot() + "/auth/realms/" + bc.providerRealmName(),
                bc.getIDPClientIdInProviderRealm(),
                userId,
                sessionId);
    }

    private String assertConsumerLoginEventAccountManagement(String userIdConsumerRealm) {
        String sessionId = events.expectLogin()
                .realm(realmIdConsumerRealm)
                .client(ACCOUNT_CLIENT_NAME)
                .user(userIdConsumerRealm)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent()
                .getSessionId();
        events.clear();
        return sessionId;
    }

    private String assertProviderLoginEventIdpClient(String userIdProviderRealm) {
        return assertProviderLoginEventIdpClient(userIdProviderRealm, BROKER_CLIENT_ID);
    }

    private String assertProviderLoginEventIdpClient(String userIdProviderRealm, String clientId) {
        String sessionId = events.expectLogin()
                .realm(realmIdProviderRealm)
                .client(clientId)
                .user(userIdProviderRealm)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent()
                .getSessionId();
        // These polls are used to remove 2 events (CODE_TO_TOKEN and USER_INFO_REQUEST) that occur during login
        events.poll();
        events.poll();
        return sessionId;
    }

    private void assertConsumerLogoutEvent(String sessionIdConsumerRealm, String userIdConsumerRealm) {
        events.expectLogout(sessionIdConsumerRealm)
                .realm(realmIdConsumerRealm)
                .user(userIdConsumerRealm)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
    }

    private void assertConsumerLogoutEvents(List<String> sessionIdsConsumerRealm, String userIdConsumerRealm) {

        List<EventRepresentation> consumerRealmEvents = adminClient.realm(bc.consumerRealmName()).getEvents();

        for (String sessionId : sessionIdsConsumerRealm) {
            Optional<EventRepresentation> logoutEventOptional = consumerRealmEvents.stream()
                    .filter(event -> sessionId.equals(event.getSessionId()))
                    .findAny();

            if (logoutEventOptional.isPresent()) {
                EventRepresentation logoutEvent = logoutEventOptional.get();
                this.events.expectLogout(sessionId)
                        .realm(realmIdConsumerRealm)
                        .user(userIdConsumerRealm)
                        .removeDetail(Details.REDIRECT_URI)
                        .assertEvent(logoutEvent);
            } else {
                fail("No Logout event found for session " + sessionId);
            }
        }
    }

    private String getUserIdConsumerRealm() {
        RealmResource realmResourceConsumerRealm = adminClient.realm(bc.consumerRealmName());
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

    private List<UserSessionRepresentation> getClientSessions(String realmName, String clientId, String userId,
            String sessionId) {
        return adminClient.realm(realmName)
                .clients()
                .get(clientId)
                .getUserSessions(0, 5)
                .stream()
                .filter(s -> s.getUserId().equals(userId) && s.getId().equals(sessionId))
                .collect(Collectors.toList());
    }

    private IdentityProviderRepresentation addSecondIdentityProviderToConsumerRealm() {
        log.debug("adding second identity provider to realm " + bc.consumerRealmName());

        IdentityProviderRepresentation identityProvider2 = bc.setUpIdentityProvider();
        identityProvider2.setAlias(identityProvider2.getAlias() + "2");
        identityProvider2.setDisplayName(identityProvider2.getDisplayName() + "2");
        Map<String, String> config = identityProvider2.getConfig();
        config.put("clientId", BROKER_CLIENT_ID);
        adminClient.realm(bc.consumerRealmName()).identityProviders().create(identityProvider2).close();

        ClientResource ipdClientResource = adminClient.realm(bc.providerRealmName()).clients()
                .get(bc.getIDPClientIdInProviderRealm());
        ClientRepresentation clientRepresentation = ipdClientResource.toRepresentation();
        clientRepresentation.getRedirectUris().add(getConsumerRoot() + "/auth/realms/" + bc.consumerRealmName()
                + "/broker/" + identityProvider2.getAlias() + "/endpoint/*");
        ipdClientResource.update(clientRepresentation);

        return identityProvider2;
    }

    private void disableAccountClientConsumerRealm() {
        ClientResource accountClient = adminClient.realm(bc.consumerRealmName())
                .clients()
                .get(accountClientIdConsumerRealm);
        ClientRepresentation clientRepresentation = accountClient.toRepresentation();
        clientRepresentation.setEnabled(false);
        accountClient.update(clientRepresentation);
    }

    private OAuthClient loginWithSecondBrowser(String identityProviderDisplayName) {
        OAuthClient oauth2 = new OAuthClient();
        oauth2.init(driver2);
        oauth2.realm(bc.consumerRealmName())
                .clientId(ACCOUNT_CLIENT_NAME)
                .redirectUri(getAuthServerRoot() + "realms/" + bc.consumerRealmName() + "/account")
                .doLoginSocial(identityProviderDisplayName, bc.getUserLogin(), bc.getUserPassword());
        return oauth2;
    }

    private void linkUsers(OAuthClient oauth) {
        oauth.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail());
        oauth.linkUsers(bc.getUserLogin(), USER_PASSWORD_CONSUMER_REALM);
    }
}