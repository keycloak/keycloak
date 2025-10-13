package org.keycloak.workflow.admin.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.BadRequestException;
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
import org.keycloak.models.ModelException;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.services.ErrorResponse;

public class WorkflowResource {

    private final WorkflowsManager manager;
    private final Workflow workflow;

    public WorkflowResource(WorkflowsManager manager, Workflow workflow) {
        this.manager = manager;
        this.workflow = workflow;
    }

    @DELETE
    public void delete() {
        try {
            manager.removeWorkflow(workflow.getId());
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Update the workflow configuration. The method does not update the workflow steps.
     */
    @PUT
    public void update(WorkflowRepresentation rep) {
        try {
            manager.updateWorkflow(workflow, rep);
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Produces(APPLICATION_JSON)
    public WorkflowRepresentation toRepresentation() {
        return manager.toRepresentation(workflow);
    }

    /**
     * Bind the workflow to the resource.
     *
     * @param type the resource type
     * @param resourceId the resource id
     * @param notBefore optional notBefore time in milliseconds to schedule the first workflow step,
     *                  it overrides the first workflow step time configuration (after).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("bind/{type}/{resourceId}")
    public void bind(@PathParam("type") ResourceType type, @PathParam("resourceId") String resourceId, Long notBefore) {
        Object resource = manager.resolveResource(type, resourceId);

        if (resource == null) {
            throw new BadRequestException("Resource with id " + resourceId + " not found");
        }

        if (notBefore != null) {
            workflow.setNotBefore(notBefore);
        }

        manager.bind(workflow, type, resourceId);
    }

    @Path("steps")
    public WorkflowStepsResource steps() {
        return new WorkflowStepsResource(manager, workflow);
    }
}
