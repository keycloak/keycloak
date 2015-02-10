package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author pedroigor
 */
public interface IdentityProviderResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    IdentityProviderRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(IdentityProviderRepresentation identityProviderRepresentation);

    @DELETE
    void remove();
}