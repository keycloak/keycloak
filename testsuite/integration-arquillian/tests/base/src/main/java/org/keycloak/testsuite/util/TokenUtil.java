package org.keycloak.testsuite.util;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.keycloak.common.util.Time;

import static org.junit.Assert.fail;

/**
 * Created by st on 22/03/17.
 */
public class TokenUtil implements TestRule {

    private OAuthClient oauth;

    private String refreshToken;
    private String token;
    private int expires;

    public TokenUtil() {
        this.oauth = new OAuthClient();
        this.oauth.init(null, null);
        this.oauth.clientId("direct-grant");
    }

    @Override
    public Statement apply(final Statement base, org.junit.runner.Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }

    public String getToken() {
        if (refreshToken == null) {
            load();
        } else if (expires < Time.currentTime()) {
            refresh();
        }
        return token;
    }

    private void load() {
        try {
            System.out.println("TOKEN REQ");

            OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
            if (accessTokenResponse.getStatusCode() != 200) {
                fail("Failed to get token: " + accessTokenResponse.getErrorDescription());
            }

            this.refreshToken = accessTokenResponse.getRefreshToken();
            this.token = accessTokenResponse.getAccessToken();

            expires = Time.currentTime() + accessTokenResponse.getExpiresIn() - 20;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void refresh() {
        try {
            System.out.println("REFRESH REQ");

            OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doRefreshTokenRequest(refreshToken, "password");
            if (accessTokenResponse.getStatusCode() != 200) {
                fail("Failed to get token: " + accessTokenResponse.getErrorDescription());
            }

            this.refreshToken = accessTokenResponse.getRefreshToken();
            this.token = accessTokenResponse.getAccessToken();

            expires = Time.currentTime() + accessTokenResponse.getExpiresIn() - 20;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
