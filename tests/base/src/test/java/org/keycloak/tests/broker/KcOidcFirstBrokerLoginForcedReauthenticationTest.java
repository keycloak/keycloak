package org.keycloak.tests.broker;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.BrowserTabUtils;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Forced re-authentication (eg. {@code prompt=login}) that completes through an identity provider as a user
 * other than the one holding the current session. See #54344.
 * <p>
 * The first user must already be linked to the identity provider: on a forced re-authentication the user is
 * already known, so {@code IdentityProviderBean} narrows the login page to the identity providers that user
 * is federated with. Without an existing link no identity provider button is rendered at all and the
 * scenario cannot be reached.
 */
@KeycloakIntegrationTest
public class KcOidcFirstBrokerLoginForcedReauthenticationTest {

    private static final String IDP_ALIAS = "forced-reauth-idp";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";

    // Logs in through the broker first, so it ends up linked to the IdP and holding the consumer session
    // that the forced re-authentication then runs against.
    private static final String FIRST_USER = "first-user";

    // Only exists at the provider, so the consumer runs first-broker-login and creates it.
    private static final String SECOND_USER = "second-user";

    private static final String PASSWORD = "password";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectPage
    LoginPage loginPage;

    // The provider's real base URL is only known once the realm exists, so the seeded placeholder endpoints
    // have to be rewritten here rather than hardcoded in the realm config (which would break whenever the
    // managed server is not plain http://localhost:8080).
    @BeforeEach
    void configureBrokerEndpoints() {
        String providerBaseUrl = providerRealm.getBaseUrl();
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(IDP_ALIAS).toRepresentation();
        Map<String, String> config = idp.getConfig();
        config.put("authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth");
        config.put("tokenUrl", providerBaseUrl + "/protocol/openid-connect/token");
        config.put("userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo");
        config.put("jwksUrl", providerBaseUrl + "/protocol/openid-connect/certs");
        // Admin GET masks the secret, and updating the endpoints with that mask is rejected.
        if (ComponentRepresentation.SECRET_VALUE.equals(config.get("clientSecret"))) {
            config.put("clientSecret", CLIENT_SECRET);
        }
        consumerRealm.admin().identityProviders().get(IDP_ALIAS).update(idp);
    }

    @Test // #54344
    public void forcedReauthenticationAsDifferentBrokeredUserLinksTheIdentity() {
        forcedReauthenticationThroughBrokerAsNewUser();

        // The federated identity link used to be rolled back along with the differentUserAuthenticated
        // error page, leaving a user that could never log in again.
        assertEquals(1, federatedIdentityCount(SECOND_USER),
                "First-broker-login must have linked the new user to the identity provider");
    }

    @Test // #54344
    public void forcedReauthenticationIssuesTokensForTheNewUser() {
        forcedReauthenticationThroughBrokerAsNewUser();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        IDToken idToken = oauth.parseToken(tokenResponse.getIdToken(), IDToken.class);

        // Rebinding the previous session in place left the session adapter serving the previous user, so
        // the identity cookie and the login event were still attributed to them.
        assertEquals(SECOND_USER, idToken.getPreferredUsername(),
                "The issued token must belong to the re-authenticated user");
        assertEquals(consumerUserId(SECOND_USER), idToken.getSubject(),
                "The token subject must be the re-authenticated user");
    }

    @Test // #54344
    public void forcedReauthenticationAttributesTheLoginToTheNewUser() {
        forcedReauthenticationThroughBrokerAsNewUser();

        // attachSession reports the login against the user session it ends up with. Rebinding the previous
        // session in place left the adapter serving the previous user, so the login was recorded against
        // them even though somebody else had authenticated. The token itself is unaffected, because it is
        // issued by a later request that reloads the session.
        List<EventRepresentation> logins = consumerRealm.admin()
                .getEvents(List.of(EventType.LOGIN.name()), null, null, null, null, null, null, null);
        assertEquals(1, logins.size(), "Expected exactly one LOGIN event");
        assertEquals(consumerUserId(SECOND_USER), logins.get(0).getUserId(),
                "The login must be recorded against the re-authenticated user");
    }

    @Test // #54344
    public void forcedReauthenticationReplacesThePreviousUserSession() {
        String previousSessionId = forcedReauthenticationThroughBrokerAsNewUser();

        assertEquals(List.of(), userSessionIds(FIRST_USER),
                "The previous user's session must have been removed by the re-authentication");

        List<String> newSessions = userSessionIds(SECOND_USER);
        assertEquals(1, newSessions.size(), "The re-authenticated user must own exactly one session");

        // A session ID must not carry over to a different user: credentials are resolved against the
        // session owner without comparing their subject, so a reused ID lets the previous user's
        // credentials resolve to the new one.
        assertNotEquals(previousSessionId, newSessions.get(0),
                "The new session must not reuse the previous user's session ID");
    }

    /**
     * The replaced session ID must stay retired. Browser tabs share a root authentication session whose ID is the
     * ID a user session is created with, so another tab finishing its own login would otherwise re-create a user
     * session under the ID that was just retired.
     */
    @Test // #54344
    public void concurrentTabCannotReviveTheReplacedSessionId() {
        BrowserTabUtils tabs = driver.tabs();

        createProviderUser(FIRST_USER);
        createProviderUser(SECOND_USER);

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(FIRST_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The first user should be logged in");

        String previousSessionId = singleSessionId(FIRST_USER);
        AccountHelper.logout(providerRealm.admin(), FIRST_USER);

        // Two tabs with a pending forced re-authentication, both in the root authentication session that
        // carries the first user's session ID.
        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        loginPage.assertCurrent();
        tabs.newTab(oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).build());
        loginPage.assertCurrent();

        // The root authentication session is re-created with the ID of the session the browser already holds,
        // which is what ties the first user's session ID to both tabs.
        assertEquals(2, authenticationSessionTabsCount(previousSessionId),
                "Both login forms should belong to the root authentication session named by the first user's session");

        tabs.switchToTab(0);
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(),
                "Re-authenticating as a different user through the broker should succeed");

        String replacementSessionId = singleSessionId(SECOND_USER);
        assertNotEquals(previousSessionId, replacementSessionId,
                "The new session must not reuse the previous user's session ID");

        // Replacing the session retires the other tab as well, which empties and removes the root
        // authentication session. Without that, the tab left behind keeps the retired ID alive.
        assertEquals(0, authenticationSessionTabsCount(previousSessionId),
                "The root authentication session holding the retired ID must be gone");

        // The second tab tries to finish its own login through the same broker. With its authentication
        // session retired there is nothing left to resume. Had the tab survived, the consumer would forward
        // prompt=login and the provider would ask for the password, which is the sequence that revives the
        // retired session ID.
        tabs.switchToTab(1);
        loginPage.clickSocial(IDP_ALIAS);
        assertFalse(loginPage.isPasswordInputPresent(),
                "The retired tab must not be able to continue its login through the broker");

        List<String> sessions = userSessionIds(SECOND_USER);
        assertFalse(sessions.contains(previousSessionId),
                "The second tab must not re-create a session under the retired session ID");
        assertEquals(List.of(replacementSessionId), sessions,
                "The session created by the re-authentication must be the only one left");
        assertEquals(List.of(), userSessionIds(FIRST_USER),
                "The previous user must not get a session back");
    }

    /**
     * A tab that has created a user through first-broker-login but has not yet been through {@code /after-first-broker-login}
     * still has to save the identity link. Retiring it would leave a user that can never log in again.
     */
    @Test // #54344
    public void tabWithPendingNewUserRegistrationIsNotRetired() {
        BrowserTabUtils tabs = driver.tabs();

        createProviderUser(FIRST_USER);
        createProviderUser(SECOND_USER);

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(FIRST_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The first user should be logged in");

        String previousSessionId = singleSessionId(FIRST_USER);
        AccountHelper.logout(providerRealm.admin(), FIRST_USER);

        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        loginPage.assertCurrent();
        List<String> firstTab = authenticationSessionTabIds(previousSessionId);
        assertEquals(1, firstTab.size());

        tabs.newTab(oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).build());
        loginPage.assertCurrent();
        List<String> pendingTab = authenticationSessionTabIds(previousSessionId).stream()
                .filter(tabId -> !firstTab.contains(tabId))
                .toList();
        assertEquals(1, pendingTab.size(), "The second login form must have its own authentication session");

        // The window between creating the user and saving the link is a single redirect, which a browser cannot be
        // made to stop in, so put the second tab in that state directly.
        markNewUserRegistrationPending(previousSessionId, pendingTab.get(0));

        tabs.switchToTab(0);
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(),
                "Re-authenticating as a different user through the broker should succeed");

        assertNotEquals(previousSessionId, singleSessionId(SECOND_USER),
                "The new session must not reuse the previous user's session ID");
        assertEquals(pendingTab, authenticationSessionTabIds(previousSessionId),
                "Only the tab with a pending registration may survive the replacement");
    }

    /**
     * The previous user's level-of-assurance map is copied into the authentication session when the forced
     * re-authentication starts. A post broker login flow stores that map on the new user session, so it must not
     * survive a change of user.
     */
    @Test // #54344
    public void forcedReauthenticationDoesNotCarryOverAssuranceLevelsOfThePreviousUser() {
        configurePostBrokerLoginFlow();
        createProviderUser(FIRST_USER);
        createProviderUser(SECOND_USER);

        // Link the second user first: the stale map can only reach the new session when the user already exists,
        // because first-broker-login resets the authentication session.
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The second user should be logged in");
        AccountHelper.logout(consumerRealm.admin(), SECOND_USER);
        AccountHelper.logout(providerRealm.admin(), SECOND_USER);

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(FIRST_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The first user should be logged in");
        AccountHelper.logout(providerRealm.admin(), FIRST_USER);

        // A level the post broker login flow never records, so it can only come from the first user's session.
        setUserSessionNote(singleSessionId(FIRST_USER), Constants.LOA_MAP, "{\"7\":1}");

        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(),
                "Re-authenticating as a different user through the broker should succeed");

        String loaMap = userSessionNote(singleSessionId(SECOND_USER), Constants.LOA_MAP);
        assertTrue(loaMap.contains("\"2\""), "The post broker login flow must have recorded its own level, but the map was " + loaMap);
        assertFalse(loaMap.contains("\"7\""), "The previous user's levels must not carry over, but the map was " + loaMap);
    }

    @Test // #54344
    public void brokeredUserCanLogInAgainAfterForcedReauthentication() {
        forcedReauthenticationThroughBrokerAsNewUser();

        AccountHelper.logout(consumerRealm.admin(), SECOND_USER);
        AccountHelper.logout(providerRealm.admin(), SECOND_USER);

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();

        // Without the identity link the consumer would detect a duplicate username here and show the
        // "account already exists, please log in to link" form, which this user can never complete
        // because it has no local credentials.
        assertTrue(oauth.parseLoginResponse().isSuccess(),
                "A subsequent brokered login must not be blocked by the account-already-exists form");
    }

    /**
     * Links the first user through the broker, then forces re-authentication and completes it through the
     * same broker as a user that does not yet exist in the consumer realm.
     *
     * @return the ID of the first user's session, which the re-authentication is expected to replace.
     */
    private String forcedReauthenticationThroughBrokerAsNewUser() {
        createProviderUser(FIRST_USER);
        createProviderUser(SECOND_USER);

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(FIRST_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The first user should be logged in");

        String previousSessionId = singleSessionId(FIRST_USER);

        // Drop only the provider-side session, so the identity provider prompts for credentials again while
        // the consumer session for the first user stays alive. That live consumer session is what the
        // re-authentication later collides with.
        AccountHelper.logout(providerRealm.admin(), FIRST_USER);

        // Drop the events recorded so far, so the only LOGIN event left is the re-authentication's.
        consumerRealm.admin().clearEvents();

        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess(),
                "Re-authenticating as a different user through the broker should succeed");

        return previousSessionId;
    }

    private int authenticationSessionTabsCount(String rootAuthSessionId) {
        String realmName = consumerRealm.getName();
        return Integer.parseInt(runOnServer.fetchString(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId);
            if (rootAuthSession == null) {
                return 0;
            }
            return rootAuthSession.getAuthenticationSessions().size();
        }));
    }

    private List<String> authenticationSessionTabIds(String rootAuthSessionId) {
        String realmName = consumerRealm.getName();
        String tabIds = runOnServer.fetchString(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId);
            return rootAuthSession == null ? "" : String.join(",", rootAuthSession.getAuthenticationSessions().keySet());
        });
        return tabIds.isEmpty() ? List.of() : List.of(tabIds.split(","));
    }

    private void markNewUserRegistrationPending(String rootAuthSessionId, String tabId) {
        String realmName = consumerRealm.getName();
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId);
            AuthenticationSessionModel tab = rootAuthSession.getAuthenticationSessions().get(tabId);
            tab.setAuthNote(AbstractIdentityProvider.BROKER_REGISTERED_NEW_USER, "true");
            tab.setAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE, "pending");
        });
    }

    private void setUserSessionNote(String userSessionId, String note, String value) {
        String realmName = consumerRealm.getName();
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            session.sessions().getUserSession(realm, userSessionId).setNote(note, value);
        });
    }

    private String userSessionNote(String userSessionId, String note) {
        String realmName = consumerRealm.getName();
        return runOnServer.fetchString(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            return String.valueOf(session.sessions().getUserSession(realm, userSessionId).getNote(note));
        });
    }

    // Post broker login flow that records level 2 for the consumer's logins, the way a step-up flow would.
    private void configurePostBrokerLoginFlow() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            AuthenticationFlowModel flow = new AuthenticationFlowModel();
            flow.setAlias("post-broker-login-level");
            flow.setProviderId(AuthenticationFlow.BASIC_FLOW);
            flow.setTopLevel(true);
            flow.setBuiltIn(false);
            flow = realm.addAuthenticationFlow(flow);

            FlowUtil.inCurrentRealm(session).selectFlow("post-broker-login-level")
                    .addSubFlowExecution(Requirement.CONDITIONAL, subflow -> subflow
                            .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                    })
                            .addAuthenticatorExecution(Requirement.REQUIRED, AllowAccessAuthenticatorFactory.PROVIDER_ID));

            IdentityProviderModel idp = session.identityProviders().getByAlias(IDP_ALIAS);
            idp.setPostBrokerLoginFlowId(flow.getId());
            session.identityProviders().update(idp);
        });
    }

    private String singleSessionId(String username) {
        List<String> sessions = userSessionIds(username);
        assertEquals(1, sessions.size(), username + " should hold exactly one consumer session");
        return sessions.get(0);
    }

    private String consumerUserId(String username) {
        List<UserRepresentation> users = consumerRealm.admin().users().search(username, true);
        assertEquals(1, users.size(), "Expected exactly one user named " + username + " in the consumer realm");
        return users.get(0).getId();
    }

    private int federatedIdentityCount(String username) {
        return consumerRealm.admin().users().get(consumerUserId(username)).getFederatedIdentity().size();
    }

    private List<String> userSessionIds(String username) {
        return consumerRealm.admin().users().get(consumerUserId(username)).getUserSessions().stream()
                .map(UserSessionRepresentation::getId)
                .toList();
    }

    private void createProviderUser(String username) {
        // A complete profile keeps first-broker-login from stopping at the review-profile page, so the
        // flow runs end to end without interactive steps.
        UserRepresentation user = UserBuilder.create(username)
                .password(PASSWORD)
                .email(username + "@example.com")
                .emailVerified(true)
                .firstName("Test")
                .lastName("User")
                .build();
        try (Response response = providerRealm.admin().users().create(user)) {
            assertEquals(201, response.getStatus());
            String userId = ApiUtil.getCreatedId(response);
            providerRealm.cleanup().add(r -> r.users().get(userId).remove());
        }
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(
                    ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*")
                            .directAccessGrantsEnabled()
            );
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // Endpoints are placeholders; configureBrokerEndpoints() rewrites them once the provider
            // realm's actual base URL is known.
            String providerBase = "http://localhost:8080/realms/provider";

            IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
            idp.setAlias(IDP_ALIAS);
            idp.setProviderId(OIDCIdentityProviderFactory.PROVIDER_ID);
            idp.setEnabled(true);
            idp.setTrustEmail(true);
            idp.setConfig(Map.of(
                    "clientId", CLIENT_ID,
                    "clientSecret", CLIENT_SECRET,
                    "authorizationUrl", providerBase + "/protocol/openid-connect/auth",
                    "tokenUrl", providerBase + "/protocol/openid-connect/token",
                    "userInfoUrl", providerBase + "/protocol/openid-connect/userinfo",
                    "jwksUrl", providerBase + "/protocol/openid-connect/certs",
                    "defaultScope", "openid email profile",
                    IdentityProviderModel.SYNC_MODE, "IMPORT"
            ));

            return realm.identityProviders(idp)
                    .eventsEnabled(true);
        }
    }
}
