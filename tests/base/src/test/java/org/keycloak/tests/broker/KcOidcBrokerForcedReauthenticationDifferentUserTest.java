package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.IdpLinkAction;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
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
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.IdpConfirmLinkPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Forced re-authentication (eg. {@code prompt=login}) through an identity provider that would complete as a user other
 * than the one holding the current session is rejected before first-broker-login creates the user. See #54344.
 * <p>
 * The first user must already be linked to the identity provider: on a forced re-authentication the user is
 * already known, so {@code IdentityProviderBean} narrows the login page to the identity providers that user
 * is federated with. Without an existing link no identity provider button is rendered at all and the
 * scenario cannot be reached.
 */
@KeycloakIntegrationTest
public class KcOidcBrokerForcedReauthenticationDifferentUserTest {

    private static final String IDP_ALIAS = "forced-reauth-idp";
    private static final String SECOND_IDP_ALIAS = "forced-reauth-idp-2";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";

    // Logs in through the broker first, so it ends up linked to the IdP and holding the consumer session
    // that the forced re-authentication then runs against.
    private static final String FIRST_USER = "first-user";

    // Only exists at the provider, so the consumer would run first-broker-login and create it.
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

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    IdpConfirmLinkPage idpConfirmLinkPage;

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
    public void differentUserIsRejectedAndNotCreated() {
        String firstUserSessionId = loginFirstUserAndStartForcedReauthentication();

        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();

        errorPage.assertCurrent();
        assertTrue(errorPage.getError().contains("already authenticated as different user"),
                "Unexpected error: " + errorPage.getError());

        // The user used to be committed by first-broker-login before the login was rejected, without the identity
        // link, which left an account that could never log in again.
        assertEquals(List.of(), consumerRealm.admin().users().search(SECOND_USER, true),
                "The rejected user must not be created");
        assertEquals(List.of(firstUserSessionId), userSessionIds(FIRST_USER),
                "The session of the user holding it must be left alone");
    }

    @Test // #54344
    public void rejectionIsRecordedAsDifferentUserAuthenticated() {
        loginFirstUserAndStartForcedReauthentication();

        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();
        errorPage.assertCurrent();

        List<EventRepresentation> events = consumerRealm.admin().getEvents();
        EventRepresentation rejection = events.stream()
                .filter(e -> Errors.DIFFERENT_USER_AUTHENTICATED.equals(e.getError()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + Errors.DIFFERENT_USER_AUTHENTICATED + " event among " + events));
        assertEquals(consumerUserId(FIRST_USER), rejection.getDetails().get(Details.EXISTING_USER),
                "The event must name the user holding the session");
    }

    @Test // #54344
    public void rejectedUserCanLogInAfterSigningOut() {
        loginFirstUserAndStartForcedReauthentication();

        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();
        errorPage.assertCurrent();

        AccountHelper.logout(consumerRealm.admin(), FIRST_USER);
        AccountHelper.logout(providerRealm.admin(), SECOND_USER);

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();

        // This is what the orphaned account used to block: the duplicate-account form that the user can never complete.
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The user must be able to log in after signing out");
        assertEquals(1, federatedIdentityCount(SECOND_USER), "The user must be linked to the identity provider");
    }

    @Test // #54344
    public void sameUserCanReauthenticate() {
        String firstUserSessionId = loginFirstUserAndStartForcedReauthentication();

        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(FIRST_USER, PASSWORD);
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess(), "Re-authenticating as the same user must keep working");
        assertEquals(List.of(firstUserSessionId), userSessionIds(FIRST_USER),
                "Re-authenticating as the same user must keep the user session");
    }

    /**
     * Linking an identity provider to the logged in user (kc_action) also runs with a user session and an identity
     * that is not linked to anyone yet, which is the shape of the mismatch that is rejected. It must keep working.
     */
    @Test // #54344
    public void linkingIdentityProviderToTheLoggedInUserIsNotRejected() {
        String localUser = "local-user";
        UserRepresentation user = UserBuilder.create(localUser)
                .password(PASSWORD)
                .email(localUser + "@example.com")
                .emailVerified(true)
                .firstName("Local")
                .lastName("User")
                .build();
        try (Response response = consumerRealm.admin().users().create(user)) {
            assertEquals(201, response.getStatus());
            String userId = ApiUtil.getCreatedId(response);
            consumerRealm.cleanup().add(r -> r.users().get(userId).remove());
        }
        createProviderUser(SECOND_USER);

        oauth.openLoginForm();
        loginPage.fillLogin(localUser, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The local user should be logged in");
        assertEquals(0, federatedIdentityCount(localUser), "The local user starts out unlinked");

        oauth.loginForm().kcAction(IdpLinkAction.PROVIDER_ID + ":" + IDP_ALIAS).open();
        driver.findElement(By.id("kc-continue")).click();

        loginPage.fillLogin(SECOND_USER, PASSWORD);
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess(), "Linking the identity provider must succeed");
        assertEquals(1, federatedIdentityCount(localUser), "The identity must be linked to the logged in user");
        assertEquals(List.of(), consumerRealm.admin().users().search(SECOND_USER, true),
                "Linking must not create a user for the identity");
    }

    /**
     * A user that is already linked to one identity provider links a second one whose identity resolves to that very
     * user (same e-mail), so first-broker-login has to send them to the link confirmation page rather than create a user.
     * The identity is not linked yet, which must not be mistaken for it belonging to a different user.
     */
    @Test // #54344
    public void secondIdentityProviderResolvingToTheSessionUserCanBeLinked() {
        addSecondIdentityProvider();
        String firstUserSessionId = loginFirstUser();

        // The user was created by brokering, so it has no local password to verify the account with.
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(PASSWORD);
        credential.setTemporary(false);
        consumerRealm.admin().users().get(consumerUserId(FIRST_USER)).resetPassword(credential);

        oauth.loginForm()
                .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .param("kc_idp_hint", SECOND_IDP_ALIAS)
                .open();
        loginPage.fillLogin(FIRST_USER, PASSWORD);
        loginPage.submit();

        // The e-mail matches the first user, which is the user holding the session.
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        // No SMTP server is configured, so the existing account is verified by re-authentication.
        loginPage.fillPassword(PASSWORD);
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess(), "Linking the second identity provider must succeed");
        assertEquals(2, federatedIdentityCount(FIRST_USER), "Both identity providers must be linked to the user");
        assertEquals(List.of(firstUserSessionId), userSessionIds(FIRST_USER),
                "The user session must be kept");
    }

    /**
     * Logs the first user in through the broker, then opens a forced re-authentication and stops at the
     * consumer's login page.
     *
     * @return the ID of the first user's session.
     */
    private String loginFirstUserAndStartForcedReauthentication() {
        String sessionId = loginFirstUser();

        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        loginPage.assertCurrent();
        return sessionId;
    }

    /**
     * Logs the first user in through the broker and drops the provider-side session.
     *
     * @return the ID of the first user's session.
     */
    private String loginFirstUser() {
        createProviderUser(FIRST_USER);
        createProviderUser(SECOND_USER);

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(FIRST_USER, PASSWORD);
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess(), "The first user should be logged in");

        String sessionId = singleSessionId(FIRST_USER);

        // Drop only the provider-side session, so the identity provider prompts for credentials again while
        // the consumer session for the first user stays alive. That live consumer session is what the
        // re-authentication later collides with.
        AccountHelper.logout(providerRealm.admin(), FIRST_USER);

        consumerRealm.admin().clearEvents();
        return sessionId;
    }

    // Same provider as the first identity provider, under another alias, so the same provider user is not linked to it yet.
    private void addSecondIdentityProvider() {
        IdentityProviderRepresentation first = consumerRealm.admin().identityProviders().get(IDP_ALIAS).toRepresentation();
        Map<String, String> config = new HashMap<>(first.getConfig());
        // Admin GET masks the secret.
        config.put("clientSecret", CLIENT_SECRET);

        IdentityProviderRepresentation second = new IdentityProviderRepresentation();
        second.setAlias(SECOND_IDP_ALIAS);
        second.setProviderId(first.getProviderId());
        second.setEnabled(true);
        second.setTrustEmail(true);
        second.setConfig(config);
        try (Response response = consumerRealm.admin().identityProviders().create(second)) {
            assertEquals(201, response.getStatus());
        }
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
