package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;

import java.util.List;

public interface WorkflowStepsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<WorkflowStepRepresentation> list();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response create(WorkflowStepRepresentation stepRep, @QueryParam("position") Integer position);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response create(WorkflowStepRepresentation stepRep);

    @Path("{stepId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    WorkflowStepRepresentation get(@PathParam("stepId") String stepId);

    @Path("{stepId}")
    @DELETE
    Response delete(@PathParam("stepId") String stepId);
}