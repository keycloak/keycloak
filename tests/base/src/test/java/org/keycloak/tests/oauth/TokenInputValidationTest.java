package org.keycloak.tests.oauth;

import java.io.IOException;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest
public class TokenInputValidationTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void userInfoRejectsAlgNoneToken() {
        String noneToken = new JWSBuilder().jsonContent(createDefaultToken()).none();
        UserInfoResponse response = oauth.doUserInfoRequest(noneToken);

        assertEquals(401, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals(OAuthErrorException.INVALID_TOKEN, response.getError());
    }

    @Test
    public void userInfoRejectsTokensWithoutAlgorithmHeader() {
        assertUserInfoRejectsInvalidToken("eyJhbGciOm51bGx9.eyJzdWIiOiJ0ZXN0In0.ZmFrZQ");
        assertUserInfoRejectsInvalidToken("eyJ0eXAiOiJKV1QifQ.eyJzdWIiOiJ0ZXN0In0.ZmFrZQ");
        assertUserInfoRejectsInvalidToken("e30.eyJzdWIiOiJ0ZXN0In0.ZmFrZQ");
    }

    @Test
    public void introspectionRejectsAlgNoneToken() throws IOException {
        String noneToken = new JWSBuilder().jsonContent(createDefaultToken()).none();
        IntrospectionResponse response = oauth.doIntrospectionAccessTokenRequest(noneToken);

        assertFalse(response.asJsonNode().get("active").asBoolean());
    }

    private JsonWebToken createDefaultToken() {
        JsonWebToken token = new JsonWebToken();
        token.id(SecretGenerator.getInstance().generateSecureID());
        token.issuer("http://127.0.0.1:8500");
        token.audience(oauth.getEndpoints().getIssuer());
        token.iat((long) Time.currentTime());
        token.exp((long) (Time.currentTime() + 300));
        token.subject("attacker");
        return token;
    }

    private void assertUserInfoRejectsInvalidToken(String token) {
        UserInfoResponse response = oauth.doUserInfoRequest(token);

        assertEquals(401, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals(OAuthErrorException.INVALID_TOKEN, response.getError());
    }

}
