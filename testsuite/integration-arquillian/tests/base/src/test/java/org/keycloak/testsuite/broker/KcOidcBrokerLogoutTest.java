package org.keycloak.testsuite.broker;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public class KcOidcBrokerLogoutTest extends AbstractKcOidcBrokerLogoutTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Test
    public void logoutWithoutInitiatingIdpLogsOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));
        waitForPage(driver, "sign in to provider", true);
    }

    @Test
    public void logoutWithActualIdpAsInitiatingIdpDoesNotLogOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName(), "kc-oidc-idp");
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));

        waitForAccountManagementTitle();
    }

    @Test
    public void logoutWithOtherIdpAsInitiatinIdpLogsOutOfIdp() {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName(), "something-else");
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));
        waitForPage(driver, "sign in to provider", true);
    }

    @Test
    public void logoutAfterBrowserRestart() {
        driver.navigate().to(getLoginUrl(getConsumerRoot(), bc.consumerRealmName(), "broker-app"));
        logInWithBroker(bc);
        updateAccountInformation();

        // Exchange code from "broker-app" client of "consumer" realm for the tokens
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.realm(bc.consumerRealmName())
                .clientId("broker-app")
                .redirectUri(getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/app")
                .doAccessTokenRequest(code, "broker-app-secret");
        assertEquals(200, response.getStatusCode());

        String idToken = response.getIdToken();

        // simulate browser restart by deleting an identity cookie
        log.debugf("Deleting %s and %s cookies", AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE,
                AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE + CookieHelper.LEGACY_COOKIE);
        driver.manage().deleteCookieNamed(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE);
        driver.manage().deleteCookieNamed(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE + CookieHelper.LEGACY_COOKIE);

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName(), null, idToken);
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));

        waitForPage(driver, "sign in to provider", true);
    }

    @Test
    public void logoutAfterIdpTokenExpired() throws VerificationException {
        driver.navigate().to(getLoginUrl(getConsumerRoot(), bc.consumerRealmName(), "broker-app"));
        logInWithBroker(bc);
        updateAccountInformation();

        // Exchange code from "broker-app" client of "consumer" realm for the tokens
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.realm(bc.consumerRealmName())
                .clientId("broker-app")
                .redirectUri(getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/app")
                .doAccessTokenRequest(code, "broker-app-secret");
        assertEquals(200, response.getStatusCode());

        String idTokenString = response.getIdToken();
        IDToken idToken = TokenVerifier.create(idTokenString, IDToken.class).getToken();
        int expiresInMs = (int) (idToken.getExp() - idToken.getIat());

        // simulate token expiration
        setTimeOffset(expiresInMs * 2);

        logoutFromRealm(
                getConsumerRoot(),
                bc.consumerRealmName(),
                "something-else",
                idTokenString,
                "broker-app",
                getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/app"
        );

        // user should be logged out successfully from the IDP even though the id_token_hint is expired
        driver.navigate().to(getAccountUrl(getProviderRoot(), REALM_PROV_NAME));
        waitForPage(driver, "sign in to provider", true);
    }
}
