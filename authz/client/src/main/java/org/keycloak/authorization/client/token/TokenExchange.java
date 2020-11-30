package org.keycloak.authorization.client.token;

import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.authorization.client.util.HttpMethod;
import org.keycloak.authorization.client.util.Throwables;
import org.keycloak.authorization.client.util.TokenCallable;
import org.keycloak.representations.AccessTokenResponse;

import java.util.concurrent.Callable;

/**
 * An entry point for exchange tokens.
 * @link https://www.keycloak.org/docs/latest/securing_apps/#_token-exchange
 *
 * @author <a href="mailto:f.burgos.valdes@gmail.com">Francisco Burgos</a>
 */
public class TokenExchange {

    private Configuration configuration;
    private ServerConfiguration serverConfiguration;
    private Http http;
    private TokenCallable token;

    public TokenExchange(Configuration configuration, ServerConfiguration serverConfiguration, Http http, TokenCallable token) {
        this.configuration = configuration;
        this.serverConfiguration = serverConfiguration;
        this.http = http;
        this.token = token;
    }

    /**
     * Token exchange is the process of using a set of credentials or token to obtain an entirely different token {@link TokenExchangeRequest}.
     *
     * @param request an {@link TokenExchangeRequest} (not {@code null})
     * @return an {@link AccessTokenResponse} with new tokens
     * @throws AuthorizationDeniedException in case the request was denied by the server
     */
    public AccessTokenResponse exchange(final TokenExchangeRequest request) throws AuthorizationDeniedException {
        if (request == null) {
            throw new IllegalArgumentException("Token exchange request must not be null");
        }

        Callable<AccessTokenResponse> callable = new Callable<AccessTokenResponse>() {
            @Override
            public AccessTokenResponse call() throws Exception {
                if (request.getAudience() == null) {
                    throw new IllegalArgumentException("Audience (target) cannot be null");
                }

                HttpMethod<AccessTokenResponse> method = http.<AccessTokenResponse>post(serverConfiguration.getTokenEndpoint());

                return method
                        .params(OAuth2Constants.CLIENT_ID, request.getClientId())
                        .params(OAuth2Constants.CLIENT_SECRET, request.getClientSecret())
                        .params(OAuth2Constants.GRANT_TYPE, request.getGrantType())
                        .params(OAuth2Constants.SUBJECT_TOKEN, request.getSubjectToken())
                        .params(OAuth2Constants.REQUESTED_TOKEN_TYPE, request.getRequestedTokenType())
                        .params(OAuth2Constants.AUDIENCE, request.getAudience())
                        .response()
                        .json(AccessTokenResponse.class)
                        .execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, token, "Failed to obtain authorization data", cause);
        }
    }
}
