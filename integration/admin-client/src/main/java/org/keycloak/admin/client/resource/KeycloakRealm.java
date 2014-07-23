package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RealmRepresentation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface KeycloakRealm {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RealmRepresentation toRepresentation();

    @Path("applications")
    public KeycloakApplications applications();

    @Path("users")
    public KeycloakUsers users();

    @Path("oauth-clients")
    public KeycloakOAuthClient oAuthClients();

    @Path("roles")
    public KeycloakRoles roles();

}
