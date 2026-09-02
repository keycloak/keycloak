package org.keycloak.scim.client.authorization;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.representations.AccessTokenResponse;

import org.apache.http.HttpHeaders;

/**
 * An implementation of {@link AuthorizationMethod} that obtains an access token using the
 * OAuth 2.0 Client Credentials grant type and sets the appropriate Authorization header for requests.
 *
 * @param tokenEndpoint the token endpoint URL to obtain the access token from
 * @param clientId      the client ID
 * @param clientSecret  the client secret
 */
public record OAuth2Bearer(String tokenEndpoint, String clientId, String clientSecret) implements AuthorizationMethod {

    @Override
    public void onBefore(SimpleHttp http, SimpleHttpRequest request) {
        try {
            AccessTokenResponse response = http.doPost(tokenEndpoint)
                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS)
                    .param(OAuth2Constants.CLIENT_ID, clientId)
                    .param(OAuth2Constants.CLIENT_SECRET, clientSecret)
                    .asJson(AccessTokenResponse.class);
            String token = response.getToken();

            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain access token", e);
        }

    }
}
