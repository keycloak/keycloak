package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author pedroigor
 */
public interface IdentityProvidersResource {

    @Path("instances/{id}")
    IdentityProviderResource get(@PathParam("id") String id);

    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    List<IdentityProviderRepresentation> findAll();

    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    void create(IdentityProviderRepresentation identityProvider);
}
