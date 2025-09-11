package org.keycloak.realm.resources.policies.admin.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;

class RealmResourcePoliciesResource {

    private final KeycloakSession session;
    private final ResourcePolicyManager manager;

    public RealmResourcePoliciesResource(KeycloakSession session) {
        this.session = session;
        manager = new ResourcePolicyManager(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(ResourcePolicyRepresentation rep) {
        ResourcePolicy policy = manager.toModel(rep);
        return Response.created(session.getContext().getUri().getRequestUriBuilder().path(policy.getId()).build()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAll(List<ResourcePolicyRepresentation> reps) {
        for (ResourcePolicyRepresentation policy : reps) {
            manager.toModel(policy);
        }
        return Response.created(session.getContext().getUri().getRequestUri()).build();
    }

    @Path("{id}")
    public RealmResourcePolicyResource get(@PathParam("id") String id) {
        ResourcePolicy policy = manager.getPolicy(id);

        if (policy == null) {
            throw new NotFoundException("Resource policy with id " + id + " not found");
        }

        return new RealmResourcePolicyResource(manager, policy);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ResourcePolicyRepresentation> list() {
        return manager.getPolicies().stream().map(manager::toRepresentation).toList();
    }
}
