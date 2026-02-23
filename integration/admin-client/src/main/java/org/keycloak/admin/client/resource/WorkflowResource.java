package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.workflows.WorkflowRepresentation;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public interface WorkflowResource {

    @DELETE
    Response delete();

    @PUT
    @Consumes(APPLICATION_JSON)
    Response update(WorkflowRepresentation workflow);

    @GET
    @Produces(APPLICATION_JSON)
    WorkflowRepresentation toRepresentation();

    @Path("activate/{type}/{resourceId}")
    @POST
    void activate(@PathParam("type") String type, @PathParam("resourceId") String resourceId);

    @Path("activate/{type}/{resourceId}")
    @POST
    void activate(@PathParam("type") String type, @PathParam("resourceId") String resourceId, @QueryParam("notBefore") String notBefore);

    @Path("deactivate/{type}/{resourceId}")
    @POST
    void deactivate(@PathParam("type") String type, @PathParam("resourceId") String resourceId);

}
