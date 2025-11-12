package org.keycloak.workflow.admin.resource;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ModelException;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.services.ErrorResponse;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class WorkflowResource {

    private final WorkflowProvider provider;
    private final Workflow workflow;

    public WorkflowResource(WorkflowProvider provider, Workflow workflow) {
        this.provider = provider;
        this.workflow = workflow;
    }

    @DELETE
    public void delete() {
        try {
            provider.removeWorkflow(workflow);
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
            provider.updateWorkflow(workflow, rep);
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public WorkflowRepresentation toRepresentation(
            @Parameter(description = "Indicates whether the workflow id should be included in the representation or not - defaults to true") @QueryParam("includeId") Boolean includeId
    ) {
        WorkflowRepresentation rep = provider.toRepresentation(workflow);
        if (Boolean.FALSE.equals(includeId)) {
            rep.setId(null);
        }
        return rep;
    }

    /**
     * Activate the workflow for the resource.
     *
     * @param type the resource type
     * @param resourceId the resource id
     * @param notBefore optional value representing the time to schedule the first workflow step, overriding the first
     *                 step time configuration (after). The value is either an integer representing the seconds from now,
     *                 an integer followed by 'ms' representing milliseconds from now, or an ISO-8601 date string.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("activate/{type}/{resourceId}")
    public void activate(@PathParam("type") ResourceType type, @PathParam("resourceId") String resourceId, String notBefore) {
        Object resource = provider.getResourceTypeSelector(type).resolveResource(resourceId);

        if (resource == null) {
            throw new BadRequestException("Resource with id " + resourceId + " not found");
        }

        if (notBefore != null) {
            workflow.setNotBefore(notBefore);
        }

        provider.activate(workflow, type, resourceId);
    }

    /**
     * Deactivate the workflow for the resource.
     *
     * @param type the resource type
     * @param resourceId the resource id
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("deactivate/{type}/{resourceId}")
    public void deactivate(@PathParam("type") ResourceType type, @PathParam("resourceId") String resourceId) {
        Object resource = provider.getResourceTypeSelector(type).resolveResource(resourceId);

        if (resource == null) {
            throw new BadRequestException("Resource with id " + resourceId + " not found");
        }

        provider.deactivate(workflow, resourceId);
    }

}
