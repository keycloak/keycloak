package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.representations.workflows.WorkflowRepresentation;

import java.util.List;

public interface WorkflowsResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(WorkflowRepresentation representation);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(List<WorkflowRepresentation> representation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<WorkflowRepresentation> list();

    @Path("{id}")
    WorkflowResource workflow(@PathParam("id") String id);
}
