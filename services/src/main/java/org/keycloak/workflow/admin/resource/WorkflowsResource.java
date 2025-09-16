package org.keycloak.workflow.admin.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.representations.workflows.WorkflowRepresentation;

import java.util.List;

public class WorkflowsResource {

    private final KeycloakSession session;
    private final WorkflowsManager manager;

    public WorkflowsResource(KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Feature.WORKFLOWS)) {
            throw new NotFoundException();
        }
        this.session = session;
        this.manager = new WorkflowsManager(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(WorkflowRepresentation rep) {
        Workflow workflow = manager.toModel(rep);
        return Response.created(session.getContext().getUri().getRequestUriBuilder().path(workflow.getId()).build()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAll(List<WorkflowRepresentation> reps) {
        for (WorkflowRepresentation workflow : reps) {
            manager.toModel(workflow);
        }
        return Response.created(session.getContext().getUri().getRequestUri()).build();
    }

    @Path("{id}")
    public WorkflowResource get(@PathParam("id") String id) {
        Workflow workflow = manager.getWorkflow(id);

        if (workflow == null) {
            throw new NotFoundException("Workflow with id " + id + " not found");
        }

        return new WorkflowResource(manager, workflow);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkflowRepresentation> list() {
        return manager.getWorkflows().stream().map(manager::toRepresentation).toList();
    }
}
