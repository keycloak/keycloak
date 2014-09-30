package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RealmRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RealmResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RealmRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(RealmRepresentation realmRepresentation);

    @Path("applications")
    public ApplicationsResource applications();

    @Path("users")
    public UsersResource users();

    @Path("oauth-clients")
    public OAuthClientsResource oAuthClients();

    @Path("roles")
    public RolesResource roles();

    @DELETE
    public void remove();

}
