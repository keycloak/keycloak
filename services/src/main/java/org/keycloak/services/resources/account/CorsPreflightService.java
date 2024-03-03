package org.keycloak.services.resources.account;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.http.HttpRequest;
import org.keycloak.services.cors.Cors;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.keycloak.services.resources.KeycloakOpenAPI;

/**
 * Created by st on 21/03/17.
 */
@Extension(name = KeycloakOpenAPI.Profiles.ACCOUNT, value = "")
public class CorsPreflightService {

    private final HttpRequest request;

    public CorsPreflightService(HttpRequest request) {
        this.request = request;
    }

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("{any:.*}")
    @OPTIONS
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ACCOUNT)
    @Operation(summary = "CORS preflight.")
    public Response preflight() {
        Cors cors = Cors.add(request, Response.ok()).auth().allowedMethods("GET", "POST", "DELETE", "PUT", "HEAD", "OPTIONS").preflight();
        return cors.build();
    }

}
