package org.keycloak.tests.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Errors;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class PkceEnforcedClientTest {

    private static final String USERNAME = "pkce-user";
    private static final String PASSWORD = "password";

    @InjectRealm(config = PkceRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = PkceUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = PkceClientConfig.class)
    OAuthClient oauth;

    @InjectOAuthClient(ref = "no-pkce", config = NoPkceClientConfig.class)
    OAuthClient noPkceOauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordResetPage resetPasswordPage;

    @InjectEvents
    Events events;

    @Test
    public void codeFromAuthorizationEndpointIsExchangedWithCodeVerifier() {
        realm.cleanup().add(RealmResource::logoutAll);

        PkceGenerator pkce = PkceGenerator.s256();
        AuthorizationEndpointResponse authzResponse = oauth.loginForm().codeChallenge(pkce).doLogin(USERNAME, PASSWORD);
        assertTrue(authzResponse.isSuccess());

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authzResponse.getCode()).codeVerifier(pkce).send();
        assertEquals(200, tokenResponse.getStatusCode());
    }

    @Test
    public void codeFromDirectResetCredentialsLinkIsRejectedWithoutCodeChallenge() {
        realm.cleanup().add(RealmResource::logoutAll);

        String code = loginFromDirectResetCredentialsLink(oauth);
        events.clear();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenResponse.getError());
        assertEquals("PKCE code challenge not present in authorization", tokenResponse.getErrorDescription());

        EventAssertion.expectCodeToTokenError(events.poll())
                .clientId(oauth.getClientId())
                .userId(user.getId())
                .error(Errors.PKCE_VERIFICATION_FAILED);
    }

    @Test
    public void codeFromDirectResetCredentialsLinkIsAcceptedForClientWithoutPkce() {
        realm.cleanup().add(RealmResource::logoutAll);

        String code = loginFromDirectResetCredentialsLink(noPkceOauth);

        AccessTokenResponse tokenResponse = noPkceOauth.doAccessTokenRequest(code);
        assertEquals(200, tokenResponse.getStatusCode());
    }

    // logs in through the direct reset-credentials link and returns the code Keycloak issues.
    // this link skips the authorization endpoint, so no code challenge is ever sent
    private String loginFromDirectResetCredentialsLink(OAuthClient client) {
        driver.open(realm.getBaseUrl() + "/login-actions/reset-credentials?client_id=" + client.getClientId() + "&redirect_uri=" + client.getRedirectUri());
        resetPasswordPage.assertCurrent();
        resetPasswordPage.backToLogin();

        loginPage.assertCurrent();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();

        AuthorizationEndpointResponse authzResponse = client.parseLoginResponse();
        assertTrue(authzResponse.isSuccess());
        return authzResponse.getCode();
    }

    private static class PkceRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.resetPasswordAllowed(true);
        }
    }

    private static class PkceUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username(USERNAME)
                    .name("Pkce", "User")
                    .email("pkce-user@localhost")
                    .emailVerified(true)
                    .password(PASSWORD);
        }
    }

    private static class PkceClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("pkce-app")
                    .publicClient()
                    .attribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, OAuth2Constants.PKCE_METHOD_S256);
        }
    }

    private static class NoPkceClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("app").publicClient();
        }
    }
}
