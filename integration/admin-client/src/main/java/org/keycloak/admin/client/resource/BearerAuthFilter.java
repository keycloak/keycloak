package org.keycloak.admin.client.resource;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class BearerAuthFilter implements ClientRequestFilter {

    private final String tokenString;

    public BearerAuthFilter(String tokenString) {
        this.tokenString = tokenString;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String authHeader = "Bearer " + tokenString;
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
    }

}
