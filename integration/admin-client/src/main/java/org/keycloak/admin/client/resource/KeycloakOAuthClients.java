package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.OAuthClientRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface KeycloakOAuthClients {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<OAuthClientRepresentation> findAll();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(OAuthClientRepresentation oAuthClientRepresentation);

    @Path("{oAuthClientId}")
    public KeycloakOAuthClient get(@PathParam("oAuthClientId") String oAuthClientId);

}
