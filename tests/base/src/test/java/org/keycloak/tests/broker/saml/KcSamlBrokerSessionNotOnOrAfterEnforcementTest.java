package org.keycloak.tests.broker.saml;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class KcSamlBrokerSessionNotOnOrAfterEnforcementTest extends AbstractKcSamlBrokerTest {

    private static final int SHORT_SESSION_LIFESPAN_SECONDS = 120;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void testSessionInvalidatedAfterUpstreamSessionNotOnOrAfter() {
        getProviderRealm().updateWithCleanup(r -> r.ssoSessionMaxLifespan(SHORT_SESSION_LIFESPAN_SECONDS));

        logInAsUserInIDP();
        updateAccountInformation();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertEquals(200, tokenResponse.getStatusCode());
        assertNotNull(tokenResponse.getAccessToken());

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(200, refreshResponse.getStatusCode());
        assertNull(refreshResponse.getError());

        timeOffSet.set(SHORT_SESSION_LIFESPAN_SECONDS + 30);

        AccessTokenResponse failedRefresh = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken());
        assertEquals(400, failedRefresh.getStatusCode(),
                "Session should be expired due to upstream SessionNotOnOrAfter");
    }

    @Test
    public void testSessionStillValidBeforeUpstreamSessionNotOnOrAfter() {
        getProviderRealm().updateWithCleanup(r -> r.ssoSessionMaxLifespan(SHORT_SESSION_LIFESPAN_SECONDS));

        logInAsUserInIDP();
        updateAccountInformation();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertEquals(200, tokenResponse.getStatusCode());

        timeOffSet.set(SHORT_SESSION_LIFESPAN_SECONDS - 30);

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(200, refreshResponse.getStatusCode(),
                "Session should still be valid before upstream SessionNotOnOrAfter");
        assertNotNull(refreshResponse.getAccessToken());
    }

    @Test
    public void testLocalMaxLifespanHonouredWhenShorterThanUpstream() {
        int shortLocalLifespan = 180;
        int longUpstreamLifespan = 600;

        getProviderRealm().updateWithCleanup(r -> r.ssoSessionMaxLifespan(longUpstreamLifespan));
        getConsumerRealm().updateWithCleanup(r -> r.ssoSessionMaxLifespan(shortLocalLifespan));

        logInAsUserInIDP();
        updateAccountInformation();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertEquals(200, tokenResponse.getStatusCode());

        timeOffSet.set(shortLocalLifespan + 30);

        AccessTokenResponse failedRefresh = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, failedRefresh.getStatusCode(),
                "Session should expire based on local max lifespan when shorter than upstream");
    }
}
