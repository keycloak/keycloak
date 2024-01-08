/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.policy.evaluation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Stream;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Permission;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DecisionPermissionCollector extends AbstractDecisionCollector {

    protected final AuthorizationProvider authorizationProvider;
    protected final ResourceServer resourceServer;
    protected final AuthorizationRequest request;
    protected final Set<Permission> permissions = new LinkedHashSet<>();

    public DecisionPermissionCollector(AuthorizationProvider authorizationProvider, ResourceServer resourceServer, AuthorizationRequest request) {
        this.authorizationProvider = authorizationProvider;
        this.resourceServer = resourceServer;
        this.request = request;
    }

    /**
     * Processes the final permission decisions for all policies associated with a single resource permission
     * @param results a map of {@link Policy} to {@link Result} containing all needed contextual and hierarchical information for making a decision
     */
    @Override
    public void onComplete(ResourcePermission permission, Map<Policy, Result> results) {
        Collection<Scope> requestedScopes = permission.getScopes();
        Scope fakeScope = null;

        // we need to allow requests to scopes that don't actually exist on the resource so we need to do some processing...
        Collection<Scope> resourceScopes = new LinkedHashSet<>();
        if(permission.getResource() != null) {
           resourceScopes.addAll(permission.getResource().getScopes());
            // in some cases we end up with no temp scopes to work with. We still need to be able to track overrides on these policies
            // create a fake scope relevant to this resource that we'll remove at the end
            fakeScope = new FakeScope(permission.getResource().getId());
        }
        // if we don't have a resource we can still try to grant the scopes requested outright
        resourceScopes.addAll(requestedScopes);

        // without having some modifications to how policies are queried we need filter out a bunch of stuff that shouldn't be here.
        // for each result entry:
        Stream<Result> typePermissions = results.values().stream()
            // only pass policies that have a type on them and no scopes or resources (we'll pick those up in the other streams)
            .filter(result -> result.getPolicy() != null)
            .filter(result -> !StringUtil.isNullOrEmpty(result.getPolicy().getType()))
            .filter(result -> CollectionUtil.isEmpty(result.getPolicy().getResources()))
            .filter(result -> CollectionUtil.isEmpty(result.getPolicy().getScopes()));
        Stream<Result> resourceScopePermissions = results.values().stream()
            // only pass policies that match the currently requested resource and either have no scopes or at least one scope matches a requested scope
            .filter(result -> result.getPolicy() != null)
            .filter(result -> permission.getResource() != null)
            .filter(result -> result.getPolicy().getResources().contains(permission.getResource()))
            .filter(result -> CollectionUtil.isEmpty(result.getPolicy().getScopes()) || result.getPolicy().getScopes().stream().anyMatch(permission.getScopes()::contains));
        Stream<Result> scopeOnlyPermissions = results.values().stream()
            // only pass policies that match at least one of the requested scopes and have no resources
            .filter(result -> result.getPolicy() != null)
            .filter(result -> CollectionUtil.isNotEmpty(permission.getScopes()))
            .filter(result -> CollectionUtil.isEmpty(result.getPolicy().getResources()))
            .filter(result -> result.getPolicy().getScopes().stream().anyMatch(permission.getScopes()::contains));
        Stream<Result> grantResults = results.values().stream()
            // make sure we don't filter out any outright grants in the process of pruning our results
            .filter(result -> result.getPolicy() == null);
        // remove all duplicate entries from both the original result set (because of overlapping queries) and the multiple combined streams
        List<Result> prunedResults = Stream.of(typePermissions, resourceScopePermissions, scopeOnlyPermissions, grantResults).flatMap(resultStream -> resultStream).distinct().collect(Collectors.toList());

        // if we filter out all of the fluff policies and there's nothing here then we can just return now. When policies are filtered at the DB query level
        // we won't end up in this section of code unless a policy would pass these filters
        if(prunedResults.isEmpty()) {
            return;
        }

        // once queries are fixed, we can simply sort the result set. For now we have to sort the pruned results
        List<Result> sortedResults = prunedResults.stream().sorted(Comparator.comparing(Result::getPriority)).collect(Collectors.toList());

        Map<Scope, Integer> grantedScopePriority = new HashMap<>();
        Map<Scope, Integer> deniedScopePriority = new HashMap<>();
        boolean anyDeny = false;

        for(Result result : sortedResults) {
            Set<Scope> tempScopes = new HashSet<>(resourceScopes);
            if(CollectionUtil.isNotEmpty(requestedScopes)) {
                tempScopes.retainAll(requestedScopes);
            }
            if(result.getPolicy() != null && CollectionUtil.isNotEmpty(result.getPolicy().getScopes())) {
                tempScopes.retainAll(result.getPolicy().getScopes());
            }
            if(tempScopes.isEmpty()) {
                if(fakeScope == null) {
                    anyDeny = true;
                    continue; // we only arrive here if we're not requesting access to a resource AND there's no scopes being requested -- there's nothing to process
                }
                tempScopes.add(fakeScope);
            }

            // working out equal priority is done after the individual results and determining evaluation mode
            if(Effect.PERMIT.equals(result.getEffect())) {
                // only remove denied scopes if the priority is higher.
                for(Scope scope : tempScopes) {
                    deniedScopePriority.computeIfPresent(scope, (key, value) -> {
                       if(result.getPriority() > value) {
                           return null;
                       }
                       return value;
                    });
                }
                tempScopes.forEach(scope -> grantedScopePriority.put(scope, result.getPriority()));
            } else {
                // only take away from granted scopes if the priority is higher
                for(Scope scope : tempScopes) {
                    grantedScopePriority.computeIfPresent(scope, (key, value) -> {
                        if(result.getPriority() > value) {
                            return null;
                        }
                        return value;
                    });
                }
                tempScopes.forEach(scope -> deniedScopePriority.put(scope, result.getPriority()));
                anyDeny = true;
            }
        }

        if (DecisionStrategy.AFFIRMATIVE.equals(resourceServer.getDecisionStrategy())) {
            // remove any scope that was granted from the list of denied scopes if the decision strategy is affirmative
            grantedScopePriority.keySet().forEach(deniedScopePriority::remove);
        }

        // because of the above logic to modify these lists according to priority, what's left should be anything that wasn't overridden
        // in affirmative mode this list will contain only scopes that haven't been granted as well
        deniedScopePriority.keySet().forEach(grantedScopePriority::remove);

        if (anyDeny && grantedScopePriority.isEmpty()) {
            return;
        }

        // remove any fake scopes that helped us figure out overrides on resources with no scopes
        grantedScopePriority.remove(fakeScope);
        grantPermission(authorizationProvider, permissions, permission, grantedScopePriority.keySet(), resourceServer, request);
    }

    /**
     * Checks if the given {@code policy} is eligible to grant access to a resource. Resources are only granted if policy is 
     * not a scope-permission or, if so, the resource is a user-owned resource so that permissions can be overridden when 
     * inheriting policies from a typed/parent resource.
     * 
     * @param resource the resource
     * @param policy the policy that grants access to the resources
     * @return {@code true} if the resource should be granted 
     */
    private boolean isGrantingAccessToResource(Resource resource, Policy policy) {
        boolean scopePermission = isScopePermission(policy);
        
        if (!scopePermission) {
            return true;
        }
        
        return resource != null && !resource.getOwner().equals(resourceServer.getClientId());
    }

    public Collection<Permission> results() {
        return permissions;
    }

    @Override
    public void onError(Throwable cause) {
        throw new RuntimeException("Failed to evaluate permissions", cause);
    }

    protected void grantPermission(AuthorizationProvider authorizationProvider, Set<Permission> permissions, ResourcePermission permission, Collection<Scope> grantedScopes, ResourceServer resourceServer, AuthorizationRequest request) {
        Set<String> scopeNames = grantedScopes.stream().map(Scope::getName).collect(Collectors.toSet());
        Resource resource = permission.getResource();

        if (resource != null) {
            permissions.add(createPermission(resource, scopeNames, permission.getClaims(), request));
        } else if (!grantedScopes.isEmpty()) {
            ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();

            resourceStore.findByScopes(resourceServer, new HashSet<>(grantedScopes), resource1 -> permissions.add(createPermission(resource, scopeNames, permission.getClaims(), request)));

            permissions.add(createPermission(null, scopeNames, permission.getClaims(), request));
        }
    }

    private Permission createPermission(Resource resource, Set<String> scopes, Map<String, Set<String>> claims, AuthorizationRequest request) {
        AuthorizationRequest.Metadata metadata = null;

        if (request != null) {
            metadata = request.getMetadata();
        }

        Permission permission;

        if (resource != null) {
            String resourceName = metadata == null || metadata.getIncludeResourceName() ? resource.getName() : null;
            permission = new Permission(resource.getId(), resourceName, scopes, claims);
        } else {
            permission = new Permission(null, null, scopes, claims);
        }

        onGrant(permission);

        return permission;
    }

    protected void onGrant(Permission permission) {

    }

    private static boolean isResourcePermission(Policy policy) {
        return "resource".equals(policy.getType());
    }

    private static boolean isScopePermission(Policy policy) {
        return "scope".equals(policy.getType());
    }

    private static class FakeScope implements Scope {

        String id;
        public FakeScope(String id) {
            this.id = "fake_" + id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return "Fake Resource Scope: " + id;
        }

        @Override
        public void setName(String name) {

        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public void setDisplayName(String name) {

        }

        @Override
        public String getIconUri() {
            return null;
        }

        @Override
        public void setIconUri(String iconUri) {

        }

        @Override
        public ResourceServer getResourceServer() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof Scope)) return false;

            Scope that = (Scope) o;
            return that.getId().equals(getId());
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }
    }
}
