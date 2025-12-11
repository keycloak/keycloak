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
import org.keycloak.services.resources.KeycloakOpenAPI;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class WorkflowResource {

    private final WorkflowProvider provider;
    private final Workflow workflow;

    public WorkflowResource(WorkflowProvider provider, Workflow workflow) {
        this.provider = provider;
        this.workflow = workflow;
    }

    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.WORKFLOWS)
    @Operation(
            summary = "Delete workflow",
            description = "Delete the workflow and its configuration."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request")
    })
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
    @Consumes({YAMLMediaTypes.APPLICATION_JACKSON_YAML, MediaType.APPLICATION_JSON})
    @Tag(name = KeycloakOpenAPI.Admin.Tags.WORKFLOWS)
    @Operation(
            summary = "Update workflow",
            description = "Update the workflow configuration. This method does not update the workflow steps."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public void update(WorkflowRepresentation rep) {
        try {
            rep.setId(workflow.getId());
            provider.updateWorkflow(workflow, rep);
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Produces({YAMLMediaTypes.APPLICATION_JACKSON_YAML, MediaType.APPLICATION_JSON})
    @Tag(name = KeycloakOpenAPI.Admin.Tags.WORKFLOWS)
    @Operation(
            summary = "Get workflow",
            description = "Get the workflow representation. Optionally exclude the workflow id from the response."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = WorkflowRepresentation.class))),
            @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public WorkflowRepresentation toRepresentation(
            @Parameter(
                    description = "Indicates whether the workflow id should be included in the representation or not - defaults to true"
            )
            @QueryParam("includeId") Boolean includeId
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
     *                  step time configuration (after). The value is either an integer representing the seconds from now,
     *                  an integer followed by 'ms' representing milliseconds from now, or an ISO-8601 date string.
     */
    @POST
    @Path("activate/{type}/{resourceId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.WORKFLOWS)
    @Operation(
            summary = "Activate workflow for resource",
            description = "Activate the workflow for the given resource type and identifier. Optionally schedule the first step using the notBefore parameter."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public void activate(
            @Parameter(description = "Resource type")
            @PathParam("type") ResourceType type,
            @Parameter(description = "Resource identifier")
            @PathParam("resourceId") String resourceId,
            @Parameter(
                    description = "Optional value representing the time to schedule the first workflow step. " +
                            "The value is either an integer representing the seconds from now, " +
                            "an integer followed by 'ms' representing milliseconds from now, " +
                            "or an ISO-8601 date string."
            )
            @QueryParam("notBefore") String notBefore
    ) {
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.WORKFLOWS)
    @Operation(
            summary = "Deactivate workflow for resource",
            description = "Deactivate the workflow for the given resource type and identifier."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public void deactivate(
            @Parameter(description = "Resource type")
            @PathParam("type") ResourceType type,
            @Parameter(description = "Resource identifier")
            @PathParam("resourceId") String resourceId
    ) {
        Object resource = provider.getResourceTypeSelector(type).resolveResource(resourceId);

        if (resource == null) {
            throw new BadRequestException("Resource with id " + resourceId + " not found");
        }

        provider.deactivate(workflow, resourceId);
    }

}
