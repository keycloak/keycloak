package org.keycloak.realm.resources.policies.admin.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import org.keycloak.models.policy.ResourceAction;
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
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
        return toRepresentation(policy);
    }

    ResourcePolicyRepresentation toRepresentation(ResourcePolicy policy) {
        ResourcePolicyRepresentation rep = new ResourcePolicyRepresentation(policy.getId(), policy.getProviderId(), policy.getConfig());

        for (ResourceAction action : manager.getActions(policy)) {
            rep.addAction(new ResourcePolicyActionRepresentation(action.getId(), action.getProviderId(), action.getConfig()));
        }

        return rep;
    }
}
