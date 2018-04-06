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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.AuthorizationContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.MethodConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.ScopeEnforcementMode;
import org.keycloak.representations.idm.authorization.Permission;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(AbstractPolicyEnforcer.class);
    private static final String HTTP_METHOD_DELETE = "DELETE";

    private final PolicyEnforcer policyEnforcer;

    protected AbstractPolicyEnforcer(PolicyEnforcer policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
    }

    public AuthorizationContext authorize(OIDCHttpFacade httpFacade) {
        EnforcementMode enforcementMode = getEnforcerConfig().getEnforcementMode();

        if (EnforcementMode.DISABLED.equals(enforcementMode)) {
            return createEmptyAuthorizationContext(true);
        }

        Request request = httpFacade.getRequest();
        PathConfig pathConfig = getPathConfig(request);
        KeycloakSecurityContext securityContext = httpFacade.getSecurityContext();

        if (securityContext == null) {
            if (pathConfig != null) {
                challenge(pathConfig, getRequiredScopes(pathConfig, request), httpFacade);
            }
            return createEmptyAuthorizationContext(false);
        }

        AccessToken accessToken = securityContext.getToken();

        if (accessToken != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Checking permissions for path [%s] with config [%s].", request.getURI(), pathConfig);
            }

            if (pathConfig == null) {
                if (EnforcementMode.PERMISSIVE.equals(enforcementMode)) {
                    return createAuthorizationContext(accessToken, null);
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf("Could not find a configuration for path [%s]", getPath(request));
                }

                if (isDefaultAccessDeniedUri(request)) {
                    return createAuthorizationContext(accessToken, null);
                }

                handleAccessDenied(httpFacade);

                return createEmptyAuthorizationContext(false);
            }

            if (EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                return createAuthorizationContext(accessToken, pathConfig);
            }

            MethodConfig methodConfig = getRequiredScopes(pathConfig, request);

            if (isAuthorized(pathConfig, methodConfig, accessToken, httpFacade)) {
                try {
                    return createAuthorizationContext(accessToken, pathConfig);
                } catch (Exception e) {
                    throw new RuntimeException("Error processing path [" + pathConfig.getPath() + "].", e);
                }
            }

            if (methodConfig != null && ScopeEnforcementMode.DISABLED.equals(methodConfig.getScopesEnforcementMode())) {
                return createEmptyAuthorizationContext(true);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Sending challenge to the client. Path [%s]", pathConfig);
            }

            if (!challenge(pathConfig, methodConfig, httpFacade)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf("Challenge not sent, sending default forbidden response. Path [%s]", pathConfig);
                }
                handleAccessDenied(httpFacade);
            }
        }

        return createEmptyAuthorizationContext(false);
    }

    protected abstract boolean challenge(PathConfig pathConfig, MethodConfig methodConfig, OIDCHttpFacade facade);

    protected boolean isAuthorized(PathConfig actualPathConfig, MethodConfig methodConfig, AccessToken accessToken, OIDCHttpFacade httpFacade) {
        Request request = httpFacade.getRequest();

        if (isDefaultAccessDeniedUri(request)) {
            return true;
        }

        Authorization authorization = accessToken.getAuthorization();

        if (authorization == null) {
            return false;
        }

        boolean hasPermission = false;
        List<Permission> grantedPermissions = authorization.getPermissions();

        for (Permission permission : grantedPermissions) {
            if (permission.getResourceId() != null) {
                if (isResourcePermission(actualPathConfig, permission)) {
                    hasPermission = true;

                    if (actualPathConfig.isInstance() && !matchResourcePermission(actualPathConfig, permission)) {
                        continue;
                    }

                    if (hasResourceScopePermission(methodConfig, permission)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debugf("Authorization GRANTED for path [%s]. Permissions [%s].", actualPathConfig, grantedPermissions);
                        }
                        if (HTTP_METHOD_DELETE.equalsIgnoreCase(request.getMethod()) && actualPathConfig.isInstance()) {
                            policyEnforcer.getPathMatcher().removeFromCache(getPath(request));
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Authorization FAILED for path [%s]. Not enough permissions [%s].", actualPathConfig, grantedPermissions);
        }

        return false;
    }

    protected void handleAccessDenied(OIDCHttpFacade httpFacade) {
        httpFacade.getResponse().sendError(403);
    }

    protected AuthzClient getAuthzClient() {
        return policyEnforcer.getClient();
    }

    protected PolicyEnforcerConfig getEnforcerConfig() {
        return policyEnforcer.getEnforcerConfig();
    }

    protected PolicyEnforcer getPolicyEnforcer() {
        return policyEnforcer;
    }

    private boolean isDefaultAccessDeniedUri(Request request) {
        String accessDeniedPath = getEnforcerConfig().getOnDenyRedirectTo();
        return accessDeniedPath != null && request.getURI().contains(accessDeniedPath);
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

    private AuthorizationContext createEmptyAuthorizationContext(final boolean granted) {
        return new ClientAuthorizationContext(getAuthzClient()) {
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
        return new ClientAuthorizationContext(accessToken, pathConfig, getAuthzClient());
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

    private PathConfig getPathConfig(Request request) {
        return isDefaultAccessDeniedUri(request) ? null : policyEnforcer.getPathMatcher().matches(getPath(request));
    }
}
