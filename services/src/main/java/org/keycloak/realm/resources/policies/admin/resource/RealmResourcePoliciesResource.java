package org.keycloak.realm.resources.policies.admin.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.policy.ResourceAction;
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyConditionRepresentation;
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
        ResourcePolicy policy = createPolicy(rep);
        return Response.created(session.getContext().getUri().getRequestUriBuilder().path(policy.getId()).build()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAll(List<ResourcePolicyRepresentation> reps) {
        for (ResourcePolicyRepresentation policy : reps) {
            createPolicy(policy);
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
        return manager.getPolicies().stream().map(this::toRepresentation).toList();
    }

    private ResourcePolicy createPolicy(ResourcePolicyRepresentation rep) {
        ResourcePolicyManager manager = new ResourcePolicyManager(session);
        MultivaluedHashMap<String, String> config = Optional.ofNullable(rep.getConfig()).orElse(new MultivaluedHashMap<>());

        for (ResourcePolicyConditionRepresentation condition : rep.getConditions()) {
            String conditionProviderId = condition.getProviderId();
            config.computeIfAbsent("conditions", key -> new ArrayList<>()).add(conditionProviderId);

            for (Entry<String, List<String>> configEntry : condition.getConfig().entrySet()) {
                config.put(conditionProviderId + "." + configEntry.getKey(), configEntry.getValue());
            }
        }

        ResourcePolicy policy = manager.addPolicy(rep.getProviderId(), config);
        List<ResourceAction> actions = new ArrayList<>();

        for (ResourcePolicyActionRepresentation actionRep : rep.getActions()) {
            actions.add(new ResourceAction(actionRep.getProviderId(), new MultivaluedHashMap<>(actionRep.getConfig())));
        }

        manager.updateActions(policy, actions);

        return policy;
    }

    ResourcePolicyRepresentation toRepresentation(ResourcePolicy policy) {
        ResourcePolicyRepresentation rep = new ResourcePolicyRepresentation(policy.getId(), policy.getProviderId(), policy.getConfig());

        for (ResourceAction action : manager.getActions(policy)) {
            rep.addAction(new ResourcePolicyActionRepresentation(action.getId(), action.getProviderId(), action.getConfig()));
        }

        return rep;
    }
}
