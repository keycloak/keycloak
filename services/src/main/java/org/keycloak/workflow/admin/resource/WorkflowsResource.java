package org.keycloak.workflow.admin.resource;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class WorkflowsResource {

    private final KeycloakSession session;
    private final WorkflowProvider provider;
    private final AdminPermissionEvaluator auth;

    public WorkflowsResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        if (!Profile.isFeatureEnabled(Feature.WORKFLOWS)) {
            throw new NotFoundException();
        }
        this.session = session;
        this.provider = session.getProvider(WorkflowProvider.class);
        this.auth = auth;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response create(WorkflowRepresentation rep) {
        auth.realm().requireManageRealm();

        try {
            Workflow workflow = provider.toModel(rep);
            return Response.created(session.getContext().getUri().getRequestUriBuilder().path(workflow.getId()).build()).build();
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @Path("{id}")
    public WorkflowResource get(@PathParam("id") String id) {
        auth.realm().requireManageRealm();

        Workflow workflow = provider.getWorkflow(id);

        if (workflow == null) {
            throw new NotFoundException("Workflow with id " + id + " not found");
        }

        return new WorkflowResource(provider, workflow);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public List<WorkflowRepresentation> list(
            @Parameter(description = "A String representing the workflow name - either partial or exact") @QueryParam("search") String search,
            @Parameter(description = "Boolean which defines whether the param 'search' must match exactly or not") @QueryParam("exact") Boolean exact,
            @Parameter(description = "The position of the first result to be processed (pagination offset)") @QueryParam("first") @DefaultValue("0") Integer firstResult,
            @Parameter(description = "The maximum number of results to be returned - defaults to 10") @QueryParam("max") @DefaultValue("10") Integer maxResults
    ) {
        auth.realm().requireManageRealm();

        int first = Optional.ofNullable(firstResult).orElse(0);
        int max = Optional.ofNullable(maxResults).orElse(10);
        return provider.getWorkflows(search, exact, first, max).map(provider::toRepresentation).toList();
    }
}
