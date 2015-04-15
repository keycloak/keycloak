package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.token.TokenManager;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class BearerAuthFilter implements ClientRequestFilter {

    private final String tokenString;
    private final TokenManager tokenManager;

    public BearerAuthFilter(String tokenString) {
        this.tokenString = tokenString;
        this.tokenManager = null;
    }

    public BearerAuthFilter(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.tokenString = null;
    }


    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String authHeader = "Bearer " + (tokenManager != null ? tokenManager.getAccessTokenString() : tokenString);

        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
    }

}
