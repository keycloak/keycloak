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

import org.jboss.logging.Logger;
import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.adapters.spi.HttpFacade.Response;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(AbstractPolicyEnforcer.class);
    private final PolicyEnforcerConfig enforcerConfig;
    private final PolicyEnforcer policyEnforcer;

    private List<PathConfig> paths;
    private AuthzClient authzClient;
    private PathMatcher pathMatcher;

    public AbstractPolicyEnforcer(PolicyEnforcer policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
        this.enforcerConfig = policyEnforcer.getEnforcerConfig();
        this.authzClient = policyEnforcer.getClient();
        this.pathMatcher = new PathMatcher();
        this.paths = policyEnforcer.getPaths();
    }

    public AuthorizationContext authorize(OIDCHttpFacade httpFacade) {
        EnforcementMode enforcementMode = this.enforcerConfig.getEnforcementMode();

        if (EnforcementMode.DISABLED.equals(enforcementMode)) {
            return createEmptyAuthorizationContext(true);
        }

        AccessToken accessToken = httpFacade.getSecurityContext().getToken();
        Request request = httpFacade.getRequest();
        Response response = httpFacade.getResponse();
        String pathInfo = URI.create(request.getURI()).getPath().substring(1);
        String path = pathInfo.substring(pathInfo.indexOf('/'), pathInfo.length());
        PathConfig pathConfig = this.pathMatcher.matches(path, this.paths);

        LOGGER.debugf("Checking permissions for path [%s] with config [%s].", request.getURI(), pathConfig);

        if (pathConfig == null) {
            if (EnforcementMode.PERMISSIVE.equals(enforcementMode)) {
                return createAuthorizationContext(accessToken);
            }

            LOGGER.debugf("Could not find a configuration for path [%s]", path);
            response.sendError(403, "Could not find a configuration for path [" + path + "].");

            return createEmptyAuthorizationContext(false);
        }

        PathConfig actualPathConfig = resolvePathConfig(pathConfig, request);
        Set<String> requiredScopes = getRequiredScopes(actualPathConfig, request);

        if (isAuthorized(actualPathConfig, requiredScopes, accessToken, httpFacade)) {
            try {
                return createAuthorizationContext(accessToken);
            } catch (Exception e) {
                throw new RuntimeException("Error processing path [" + actualPathConfig.getPath() + "].", e);
            }
        }

        if (!challenge(actualPathConfig, requiredScopes, httpFacade)) {
            LOGGER.debugf("Sending challenge to the client. Path [%s]", pathConfig);
            response.sendError(403, "Authorization failed.");
        }

        return createEmptyAuthorizationContext(false);
    }

    protected abstract boolean challenge(PathConfig pathConfig, Set<String> requiredScopes, OIDCHttpFacade facade);

    protected boolean isAuthorized(PathConfig actualPathConfig, Set<String> requiredScopes, AccessToken accessToken, OIDCHttpFacade httpFacade) {
        Request request = httpFacade.getRequest();
        PolicyEnforcerConfig enforcerConfig = getEnforcerConfig();
        String accessDeniedPath = enforcerConfig.getAccessDeniedPath();

        if (accessDeniedPath != null) {
            if (request.getURI().contains(accessDeniedPath)) {
                return true;
            }
        }

        AccessToken.Authorization authorization = accessToken.getAuthorization();

        if (authorization == null) {
            return false;
        }

        List<Permission> permissions = authorization.getPermissions();

        for (Permission permission : permissions) {
            if (permission.getResourceSetId() != null) {
                if (isResourcePermission(actualPathConfig, permission)) {
                    if (actualPathConfig.isInstance() && !matchResourcePermission(actualPathConfig, permission)) {
                        continue;

                    }
                    if (hasResourceScopePermission(requiredScopes, permission, actualPathConfig)) {
                        LOGGER.debugf("Authorization GRANTED for path [%s]. Permissions [%s].", actualPathConfig, permissions);
                        if (request.getMethod().equalsIgnoreCase("DELETE") && actualPathConfig.isInstance()) {
                            this.paths.remove(actualPathConfig);
                        }
                        return true;
                    }
                }
            } else {
                if (hasResourceScopePermission(requiredScopes, permission, actualPathConfig)) {
                    return true;
                }
            }
        }

        LOGGER.debugf("Authorization FAILED for path [%s]. No enough permissions [%s].", actualPathConfig, permissions);

        return false;
    }

    private boolean hasResourceScopePermission(Set<String> requiredScopes, Permission permission, PathConfig actualPathConfig) {
        Set<String> allowedScopes = permission.getScopes();
        return (allowedScopes.containsAll(requiredScopes) || allowedScopes.isEmpty());
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
        return new AuthorizationContext() {
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

    private PathConfig resolvePathConfig(PathConfig originalConfig, Request request) {
        if (originalConfig.hasPattern()) {
            String pathInfo = URI.create(request.getURI()).getPath().substring(1);
            String path = pathInfo.substring(pathInfo.indexOf('/'), pathInfo.length());
            ProtectedResource resource = this.authzClient.protection().resource();
            Set<String> search = resource.findByFilter("uri=" + path);

            if (!search.isEmpty()) {
                // resource does exist on the server, cache it
                ResourceRepresentation targetResource = resource.findById(search.iterator().next()).getResourceDescription();
                PathConfig config = new PathConfig();

                config.setId(targetResource.getId());
                config.setName(targetResource.getName());
                config.setType(targetResource.getType());
                config.setPath(targetResource.getUri());
                config.setScopes(originalConfig.getScopes());
                config.setMethods(originalConfig.getMethods());
                config.setParentConfig(originalConfig);

                this.paths.add(config);

                return config;
            }
        }

        return originalConfig;
    }

    private Set<String> getRequiredScopes(PathConfig pathConfig, Request request) {
        Set<String> requiredScopes = new HashSet<>();

        requiredScopes.addAll(pathConfig.getScopes());

        String method = request.getMethod();

        for (PolicyEnforcerConfig.MethodConfig methodConfig : pathConfig.getMethods()) {
            if (methodConfig.getMethod().equals(method)) {
                requiredScopes.addAll(methodConfig.getScopes());
            }
        }

        return requiredScopes;
    }

    private AuthorizationContext createAuthorizationContext(AccessToken accessToken) {
        return new AuthorizationContext(accessToken, this.paths);
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
        return permission.getResourceSetId().equals(actualPathConfig.getId());
    }
}
