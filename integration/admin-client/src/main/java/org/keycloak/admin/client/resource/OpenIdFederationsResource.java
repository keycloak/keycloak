package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.representations.idm.OpenIdFederationRepresentation;
import java.util.List;

public interface OpenIdFederationsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<OpenIdFederationRepresentation> list() ;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(OpenIdFederationRepresentation representation) ;

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public OpenIdFederationRepresentation getOpenIdFederation(@PathParam("id") String internalId) ;

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") String internalId, OpenIdFederationRepresentation representation) ;

    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String internalId) ;

}
