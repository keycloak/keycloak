package org.keycloak.admin.client.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

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
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;

public interface RealmResourcePolicy {

    @DELETE
    Response delete();

    @PUT
    @Consumes(APPLICATION_JSON)
    Response update(ResourcePolicyRepresentation policy);

    @GET
    @Produces(APPLICATION_JSON)
    ResourcePolicyRepresentation toRepresentation();

    @Path("bind/{type}/{resourceId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void bind(@PathParam("type") String type, @PathParam("resourceId") String resourceId);

    @Path("bind/{type}/{resourceId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void bind(@PathParam("type") String type, @PathParam("resourceId") String resourceId, Long milliseconds);
}
