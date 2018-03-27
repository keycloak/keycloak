/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.adapters.authorization;

import org.keycloak.AuthorizationContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.MethodConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPolicyEnforcer {

    private static Logger LOG = LoggerFactory.getLogger(AbstractPolicyEnforcer.class);
    private final PolicyEnforcerConfig enforcerConfig;
    private final PolicyEnforcer policyEnforcer;

    private Map<String, PathConfig> paths;
    private AuthzClient authzClient;
    private PathMatcher pathMatcher;

    public AbstractPolicyEnforcer(PolicyEnforcer policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
        this.enforcerConfig = policyEnforcer.getEnforcerConfig();
        this.authzClient = policyEnforcer.getClient();
        this.pathMatcher = policyEnforcer.getPathMatcher();
        this.paths = policyEnforcer.getPaths();
    }

    public AuthorizationContext authorize(OIDCHttpFacade httpFacade) {
        EnforcementMode enforcementMode = this.enforcerConfig.getEnforcementMode();

        if (EnforcementMode.DISABLED.equals(enforcementMode)) {
            return createEmptyAuthorizationContext(true);
        }

        Request request = httpFacade.getRequest();
        String path = getPath(request);
        PathConfig pathConfig = this.pathMatcher.matches(path, this.paths);
        KeycloakSecurityContext securityContext = httpFacade.getSecurityContext();

        if (securityContext == null) {
            if (pathConfig != null) {
                challenge(pathConfig, getRequiredScopes(pathConfig, request), httpFacade);
            }
            return createEmptyAuthorizationContext(false);
        }

        AccessToken accessToken = securityContext.getToken();

        if (accessToken != null) {
            LOG.debug("Checking permissions for path [{}] with config [{}].", request.getURI(), pathConfig);

            if (pathConfig == null) {
                if (EnforcementMode.PERMISSIVE.equals(enforcementMode)) {
                    return createAuthorizationContext(accessToken, null);
                }

                LOG.debug("Could not find a configuration for path [{}]", path);

                if (isDefaultAccessDeniedUri(request, enforcerConfig)) {
                    return createAuthorizationContext(accessToken, null);
                }

                handleAccessDenied(httpFacade);

                return createEmptyAuthorizationContext(false);
            }

            if (EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                return createEmptyAuthorizationContext(true);
            }

            MethodConfig methodConfig = getRequiredScopes(pathConfig, request);

            if (isAuthorized(pathConfig, methodConfig, accessToken, httpFacade)) {
                try {
                    return createAuthorizationContext(accessToken, pathConfig);
                } catch (Exception e) {
                    throw new RuntimeException("Error processing path [" + pathConfig.getPath() + "].", e);
                }
            }

            LOG.debug("Sending challenge to the client. Path [{}]", pathConfig);

            if (!challenge(pathConfig, methodConfig, httpFacade)) {
                LOG.debug("Challenge not sent, sending default forbidden response. Path [{}]", pathConfig);
                handleAccessDenied(httpFacade);
            }
        }

        return createEmptyAuthorizationContext(false);
    }

    protected abstract boolean challenge(PathConfig pathConfig, MethodConfig methodConfig, OIDCHttpFacade facade);

    protected boolean isAuthorized(PathConfig actualPathConfig, MethodConfig methodConfig, AccessToken accessToken, OIDCHttpFacade httpFacade) {
        Request request = httpFacade.getRequest();
        PolicyEnforcerConfig enforcerConfig = getEnforcerConfig();

        if (isDefaultAccessDeniedUri(request, enforcerConfig)) {
            return true;
        }

        AccessToken.Authorization authorization = accessToken.getAuthorization();

        if (authorization == null) {
            return false;
        }

        List<Permission> permissions = authorization.getPermissions();
        boolean hasPermission = false;

        for (Permission permission : permissions) {
            if (permission.getResourceId() != null) {
                if (isResourcePermission(actualPathConfig, permission)) {
                    hasPermission = true;

                    if (actualPathConfig.isInstance() && !matchResourcePermission(actualPathConfig, permission)) {
                        continue;
                    }

                    if (hasResourceScopePermission(methodConfig, permission)) {
                        LOG.debug("Authorization GRANTED for path [{}]. Permissions [{}].", actualPathConfig, permissions);
                        if (request.getMethod().equalsIgnoreCase("DELETE") && actualPathConfig.isInstance()) {
                            this.paths.remove(actualPathConfig);
                        }
                        return true;
                    }
                }
            } else {
                if (hasResourceScopePermission(methodConfig, permission)) {
                    hasPermission = true;
                    return true;
                }
            }
        }

        if (!hasPermission && EnforcementMode.PERMISSIVE.equals(actualPathConfig.getEnforcementMode())) {
            return true;
        }

        LOG.debug("Authorization FAILED for path [{}]. Not enough permissions [{}].", actualPathConfig, permissions);

        return false;
    }

    protected void handleAccessDenied(OIDCHttpFacade httpFacade) {
        httpFacade.getResponse().sendError(403);
    }

    private boolean isDefaultAccessDeniedUri(Request request, PolicyEnforcerConfig enforcerConfig) {
        String accessDeniedPath = enforcerConfig.getOnDenyRedirectTo();

        if (accessDeniedPath != null) {
            if (request.getURI().contains(accessDeniedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasResourceScopePermission(MethodConfig methodConfig, Permission permission) {
        List<String> requiredScopes = methodConfig.getScopes();
        Set<String> allowedScopes = permission.getScopes();

        if (allowedScopes.isEmpty()) {
            return true;
        }

        PolicyEnforcerConfig.ScopeEnforcementMode enforcementMode = methodConfig.getScopesEnforcementMode();

        if (PolicyEnforcerConfig.ScopeEnforcementMode.ALL.equals(enforcementMode)) {
            return allowedScopes.containsAll(requiredScopes);
        }

        if (PolicyEnforcerConfig.ScopeEnforcementMode.ANY.equals(enforcementMode)) {
            for (String requiredScope : requiredScopes) {
                if (allowedScopes.contains(requiredScope)) {
                    return true;
                }
            }
        }

        return requiredScopes.isEmpty();
    }

    protected AuthzClient getAuthzClient() {
        return this.authzClient;
    }

    protected PolicyEnforcerConfig getEnforcerConfig() {
        return enforcerConfig;
    }

    protected PolicyEnforcer getPolicyEnforcer() {
        return policyEnforcer;
    }

    private AuthorizationContext createEmptyAuthorizationContext(final boolean granted) {
        return new ClientAuthorizationContext(authzClient) {
            @Override
            public boolean hasPermission(String resourceName, String scopeName) {
                return granted;
            }

            @Override
            public boolean hasResourcePermission(String resourceName) {
                return granted;
            }

            @Override
            public boolean hasScopePermission(String scopeName) {
                return granted;
            }

            @Override
            public List<Permission> getPermissions() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public boolean isGranted() {
                return granted;
            }
        };
    }

    private String getPath(Request request) {
        return request.getRelativePath();
    }

    private MethodConfig getRequiredScopes(PathConfig pathConfig, Request request) {
        String method = request.getMethod();

        for (MethodConfig methodConfig : pathConfig.getMethods()) {
            if (methodConfig.getMethod().equals(method)) {
                return methodConfig;
            }
        }

        MethodConfig methodConfig = new MethodConfig();

        methodConfig.setMethod(request.getMethod());
        methodConfig.setScopes(pathConfig.getScopes());
        methodConfig.setScopesEnforcementMode(PolicyEnforcerConfig.ScopeEnforcementMode.ANY);

        return methodConfig;
    }

    private AuthorizationContext createAuthorizationContext(AccessToken accessToken, PathConfig pathConfig) {
        return new ClientAuthorizationContext(accessToken, pathConfig, this.paths, authzClient);
    }

    private boolean isResourcePermission(PathConfig actualPathConfig, Permission permission) {
        // first we try a match using resource id
        boolean resourceMatch = matchResourcePermission(actualPathConfig, permission);

        // as a fallback, check if the current path is an instance and if so, check if parent's id matches the permission
        if (!resourceMatch && actualPathConfig.isInstance()) {
            resourceMatch = matchResourcePermission(actualPathConfig.getParentConfig(), permission);
        }

        return resourceMatch;
    }

    private boolean matchResourcePermission(PathConfig actualPathConfig, Permission permission) {
        return permission.getResourceId().equals(actualPathConfig.getId());
    }
}
