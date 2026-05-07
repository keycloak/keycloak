package org.keycloak.tests.oauth;

import java.io.IOException;

import org.keycloak.OAuthErrorException;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest
public class TokenInputValidationTest {

    // {"alg":"none","typ":"JWT"}.{"sub":"attacker","exp":9999999999}
    private static final String ALG_NONE_TOKEN =
            "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJhdHRhY2tlciIsImV4cCI6OTk5OTk5OTk5OX0.";

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void userInfoRejectsAlgNoneToken() {
        UserInfoResponse response = oauth.doUserInfoRequest(ALG_NONE_TOKEN);

        assertEquals(401, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals(OAuthErrorException.INVALID_TOKEN, response.getError());
    }

    @Test
    public void introspectionRejectsAlgNoneToken() throws IOException {
        IntrospectionResponse response = oauth.doIntrospectionAccessTokenRequest(ALG_NONE_TOKEN);

        assertFalse(response.asJsonNode().get("active").asBoolean());
    }

}
