package org.keycloak.authorization.policy.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.store.PolicyStore;

/**
 * this class is intended to take a provided resource server, policy store, and permission context
 * and use them to coordinate policy queries that need to be performed in order to evaluate authz
 */
public class PolicyQueryBuilder {
    ResourcePermission permission;
    ResourceServer resourceServer;
    PolicyStore policyStore;
    Collection<Scope> scopes;
    Resource resource;
    Boolean isFullSearch;

    Consumer<Policy> resourceConsumer = null;
    Consumer<Policy> scopeConsumer = null;
    Consumer<Policy> resourceScopeConsumer = null;
    Consumer<Policy> typeConsumer = null;

    private PolicyQueryBuilder(ResourceServer resourceServer, PolicyStore policyStore, ResourcePermission permission, boolean isFullSearch) {
        this.permission = permission;
        this.scopes = permission.getScopes();
        this.resource = permission.getResource();
        this.resourceServer = resourceServer;
        this.policyStore = policyStore;
        this.isFullSearch = isFullSearch;
    }

    public static PolicyQueryBuilder init(ResourceServer resourceServer, PolicyStore policyStore, ResourcePermission permission) {
        return new PolicyQueryBuilder(resourceServer, policyStore, permission, true);
    }

    /**
     * This method kicks off the evaluation that is configured at the end of the chain.
     * Because the provided policy consumers will also house the results in some way
     * (for instance, a decision cache, set, collection, or other output...)
     * we are only concerned with figuring out what we have configured and then executing
     *
     * @return if and only if no policy consumer was provided for that query returns a Set of resultant policies mapped by the query name
     */
    public Map<String, List<Policy>> query() {
        Map<String, List<Policy>> results = new HashMap<>();

        if(Boolean.TRUE.equals(isFullSearch)) {
            // get scope policies that are explicitly not linked to any resources
            if(scopes != null) {
                if(scopeConsumer != null) {
                    policyStore.findByScopes(resourceServer, null, new LinkedList<>(scopes), scopeConsumer);
                } else {
                    results.put("scope", policyStore.findByScopes(resourceServer, null, new LinkedList<>(scopes)));
                }
            }

            if(resource != null) {
                // get type policies that are explicitly not linked to any resources
                if(resource.getType() != null) {
                    if(typeConsumer != null) {
                        policyStore.findByResourceType(resourceServer, false, resource.getType(), typeConsumer);
                    } else {
                        ArrayList<Policy> typePolicies = new ArrayList<>();
                        policyStore.findByResourceType(resourceServer, false, resource.getType(), typePolicies::add);
                        results.put("type", typePolicies);
                    }
                }

                // get resource policies that are explicitly not linked to any scopes but are to the resource
                if(resourceConsumer != null) {
                    policyStore.findByResource(resourceServer, false, resource, resourceConsumer);
                } else {
                    ArrayList<Policy> resourcePolicies = new ArrayList<>();
                    policyStore.findByResource(resourceServer, false, resource, resourcePolicies::add);
                    results.put("resource", resourcePolicies);
                }
            }
        }

        // find only policies that are explicitly linked to the provided resource AND scopes
        if(scopes != null && resource != null) {
            if(resourceScopeConsumer != null) {
                policyStore.findByScopes(resourceServer, resource, new LinkedList<>(scopes), resourceScopeConsumer);
            } else {
                results.put("resource_scope", policyStore.findByScopes(resourceServer, resource, new LinkedList<>(scopes)));
            }
        }

        return results;
    }

    public PolicyQueryBuilder scopePolicyConsumer(Consumer<Policy> scopeConsumer) {
        this.scopeConsumer = scopeConsumer;
        return this;
    }


    public PolicyQueryBuilder typePolicyConsumer(Consumer<Policy> typeConsumer) {
        this.typeConsumer = typeConsumer;
        return this;
    }


    public PolicyQueryBuilder resourcePolicyConsumer(Consumer<Policy> resourceConsumer) {
        this.resourceConsumer = resourceConsumer;
        return this;
    }


    public PolicyQueryBuilder resourceScopePolicyConsumer(Consumer<Policy> resourceScopeConsumer) {
        this.resourceScopeConsumer = resourceScopeConsumer;
        return this;
    }

    public PolicyQueryBuilder allConsumers(Consumer<Policy> policyConsumer) {
        return this.resourcePolicyConsumer(policyConsumer).scopePolicyConsumer(policyConsumer).resourceScopePolicyConsumer(policyConsumer).typePolicyConsumer(policyConsumer);
    }
}
