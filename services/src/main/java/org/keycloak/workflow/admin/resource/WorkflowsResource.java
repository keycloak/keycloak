package org.keycloak.workflow.admin.resource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;
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
import org.keycloak.models.ModelException;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowSetRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class WorkflowsResource {

    private final KeycloakSession session;
    private final WorkflowsManager manager;
    private final AdminPermissionEvaluator auth;

    public WorkflowsResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        if (!Profile.isFeatureEnabled(Feature.WORKFLOWS)) {
            throw new NotFoundException();
        }
        this.session = session;
        this.manager = new WorkflowsManager(session);
        this.auth = auth;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response create(WorkflowRepresentation rep) {
        auth.realm().requireManageRealm();

        try {
            Workflow workflow = manager.toModel(rep);
            return Response.created(session.getContext().getUri().getRequestUriBuilder().path(workflow.getId()).build()).build();
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @Path("set")
    @POST
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response createAll(WorkflowSetRepresentation workflows) {
        auth.realm().requireManageRealm();

        for (WorkflowRepresentation workflow : Optional.ofNullable(workflows.getWorkflows()).orElse(List.of())) {
            create(workflow).close();
        }
        return Response.created(session.getContext().getUri().getRequestUri()).build();
    }

    @Path("{id}")
    public WorkflowResource get(@PathParam("id") String id) {
        auth.realm().requireManageRealm();

        Workflow workflow = manager.getWorkflow(id);

        if (workflow == null) {
            throw new NotFoundException("Workflow with id " + id + " not found");
        }

        return new WorkflowResource(manager, workflow);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<WorkflowRepresentation> list() {
        auth.realm().requireManageRealm();

        return manager.getWorkflows().map(manager::toRepresentation);
    }
}
