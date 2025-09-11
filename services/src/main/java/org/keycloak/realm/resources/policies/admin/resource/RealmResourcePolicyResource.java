package org.keycloak.realm.resources.policies.admin.resource;

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
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.ResourceType;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;

class RealmResourcePolicyResource {

    private final ResourcePolicyManager manager;
    private final ResourcePolicy policy;

    public RealmResourcePolicyResource(ResourcePolicyManager manager, ResourcePolicy policy) {
        this.manager = manager;
        this.policy = policy;
    }

    @DELETE
    public void delete(String id) {
        manager.removePolicy(policy.getId());
    }

    @PUT
    public void update(ResourcePolicyRepresentation rep) {
        manager.updatePolicy(policy, rep.getConfig());
    }

    @GET
    @Produces(APPLICATION_JSON)
    public ResourcePolicyRepresentation toRepresentation() {
        return manager.toRepresentation(policy);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("bind/{type}/{resourceId}")
    public void bind(@PathParam("type") ResourceType type, @PathParam("resourceId") String resourceId, Long notBefore) {
        Object resource = manager.resolveResource(type, resourceId);

        if (resource == null) {
            throw new BadRequestException("Resource with id " + resourceId + " not found");
        }

        if (notBefore != null) {
            policy.setNotBefore(notBefore);
        }

        manager.bind(policy, type, resourceId);
    }
}
