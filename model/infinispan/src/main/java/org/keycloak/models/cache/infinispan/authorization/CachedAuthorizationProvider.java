package org.keycloak.models.cache.infinispan.authorization;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.evaluation.PermissionTicketAwareDecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CachedAuthorizationProvider extends AuthorizationProvider {

    private PermissionCacheManager permissionCacheManager;

    public CachedAuthorizationProvider(KeycloakSession session, RealmModel realm, Map<String,
            PolicyProviderFactory> policyProviderFactories, PolicyEvaluator policyEvaluator,
            PermissionCacheManager permissionCacheManager) {
        super(session, realm, policyProviderFactories, policyEvaluator);

        this.permissionCacheManager = permissionCacheManager;
    }

    @Override
    public Collection<Permission> evaluatePermissions(Collection<ResourcePermission> toEvaluate, EvaluationContext evaluationContext, ResourceServer resourceServer, AuthorizationRequest authorizationRequest, String tokenID, PermissionTicketAwareDecisionResultCollector decision) {
        Collection<Permission> permissionsFromCache = permissionCacheManager.get(tokenID);
        List<Permission> resultingPermissions = new ArrayList<>();

        if (permissionsFromCache != null && !permissionsFromCache.isEmpty()) {
            Iterator<ResourcePermission> permissionToEvaluateIterator = toEvaluate.iterator();

            while (permissionToEvaluateIterator.hasNext()) {
                ResourcePermission checkedResourcePermission = permissionToEvaluateIterator.next();
                Optional<Permission> permissionFromCache = permissionsFromCache.stream()
                        .filter(permissionToFilter -> permissionToFilter.getResourceId().equals(checkedResourcePermission.getResource().getId())).findFirst();

                Set<String> requestedScopes = checkedResourcePermission.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());
                if (permissionFromCache.isPresent()
                        && hasValidClaims(checkedResourcePermission.getClaims(), permissionFromCache.get().getClaims())
                        && hasValidScopes(requestedScopes, permissionFromCache.get().getScopes())

                ) {
                    Permission permToAdd = permissionFromCache.get();

                    // Set scopes from request as cache can contain more scopes than requested
                    permToAdd.setScopes(requestedScopes);
                    resultingPermissions.add(permToAdd);

                    // Permission resolved from cache don't need to evaluate anymore
                    permissionToEvaluateIterator.remove();
                }
            }

        }

        if (!toEvaluate.isEmpty()) {
            resultingPermissions.addAll(super.evaluatePermissions(toEvaluate, evaluationContext, resourceServer, authorizationRequest, tokenID, decision));
        }

        permissionCacheManager.put(tokenID, resultingPermissions);

        return resultingPermissions;
    }

    /**
     * Check whether all requested scopes are present in cached Permission
     * @param requested
     * @param fromCache
     * @return
     */
    private boolean hasValidScopes(Collection<String> requested, Collection<String> fromCache) {
        if (requested == fromCache) {
            return true;
        }

        if (requested == null || fromCache == null) {
            return false;
        }

        for (String scope: requested) {
            if (!fromCache.contains(scope)) {
                return false;
            }
        }

        return true;
    }

    /**
     * check whether requested and cached claims are equal
     * @param requested
     * @param fromCache
     * @return
     */
    private boolean hasValidClaims(Map<String, Set<String>> requested, Map<String, Set<String>> fromCache) {
        if (requested == null && fromCache == null) {
            return true;
        }

        if (requested == null || fromCache == null) {
            return false;
        }

        if (requested.isEmpty() && fromCache.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Set<String>> entry : requested.entrySet()) {
            Set<String> requestedClaims = entry.getValue();
            Set<String> cacheClaims = fromCache.get(entry.getKey());
            if (requestedClaims == cacheClaims) {
                continue;
            }

            if (requestedClaims == null || cacheClaims == null) {
                return false;
            }

            if (cacheClaims.isEmpty() && requestedClaims.isEmpty()) {
                continue;
            }

            if (cacheClaims.isEmpty() || !entry.getValue().containsAll(cacheClaims)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void clear() {
        permissionCacheManager.clear();
    }
}
