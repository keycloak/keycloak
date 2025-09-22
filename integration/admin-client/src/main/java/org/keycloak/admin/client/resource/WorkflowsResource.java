package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowSetRepresentation;

public interface WorkflowsResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(WorkflowRepresentation representation);

    @Path("set")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(WorkflowSetRepresentation representation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<WorkflowRepresentation> list();

    @Path("{id}")
    WorkflowResource workflow(@PathParam("id") String id);
}
