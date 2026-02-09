package org.keycloak.testsuite.broker;

import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.IDToken;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.Assert.assertEquals;

public class KcOidcBrokerLogoutFrontChannelTest extends AbstractKcOidcBrokerLogoutTest {
    @Rule public AssertEvents events = new AssertEvents(this);

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationIdpLogoutFrontChannel();
    }

    private static class KcOidcBrokerConfigurationIdpLogoutFrontChannel
        extends KcOidcBrokerConfiguration {

        @Override
        protected void applyDefaultConfiguration(
            Map<String, String> config, IdentityProviderSyncMode syncMode) {
            super.applyDefaultConfiguration(config, syncMode);
            config.put("backchannelSupported", "false");
        }
    }

    @Test
    public void logoutAfterIdpTokenExpired() throws VerificationException {
        driver.navigate().to(getLoginUrl(getConsumerRoot(), bc.consumerRealmName(), "broker-app"));
        logInWithBroker(bc);
        updateAccountInformation();

        // Exchange code from "broker-app" client of "consumer" realm for the tokens
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response =
            oauth
                .realm(bc.consumerRealmName())
                .client("broker-app", "broker-app-secret")
                .redirectUri(getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/app")
                .doAccessTokenRequest(code);
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
            getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/app");

        oauth.clientId("account");
        oauth.redirectUri(getConsumerRoot() + "/auth/realms/" + REALM_PROV_NAME + "/account");
        loginPage.open(REALM_PROV_NAME);

        waitForPage(driver, "sign in to provider", true);
    }
}
