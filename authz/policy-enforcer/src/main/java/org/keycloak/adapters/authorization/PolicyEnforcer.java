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

import static org.keycloak.adapters.authorization.util.JsonUtils.asAccessToken;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.authorization.cip.spi.ClaimInformationPointProviderFactory;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.adapters.authorization.spi.HttpResponse;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.common.util.Base64;
import org.keycloak.protocol.oidc.client.authentication.ClientCredentialsProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.MethodConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.ScopeEnforcementMode;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.util.JsonSerialization;

/**
 * <p>A Policy Enforcement Point (PEP) that requests and enforces authorization decisions from Keycloak.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(PolicyEnforcer.class);
    private static final String HTTP_METHOD_DELETE = "DELETE";

    public static Builder builder() {
        return new Builder();
    }

    private final AuthzClient authzClient;
    private final Map<String, PathConfig> paths;
    private final PathConfigMatcher pathMatcher;
    private final HttpClient httpClient;
    private final PolicyEnforcerConfig enforcerConfig;

    private final Map<String, ClaimInformationPointProviderFactory> claimInformationPointProviderFactories = new HashMap<>();

    protected PolicyEnforcer(Builder builder) {
        enforcerConfig = builder.getEnforcerConfig();
        Configuration authzClientConfig = builder.authzClientConfig;

        if (authzClientConfig.getRealm() == null) {
            authzClientConfig.setRealm(enforcerConfig.getRealm());
        }

        if (authzClientConfig.getAuthServerUrl() == null) {
            authzClientConfig.setAuthServerUrl(enforcerConfig.getAuthServerUrl());
        }

        if (authzClientConfig.getCredentials() == null || authzClientConfig.getCredentials().isEmpty()) {
            authzClientConfig.setCredentials(enforcerConfig.getCredentials());
        }

        if (authzClientConfig.getResource() == null) {
            authzClientConfig.setResource(enforcerConfig.getResource());
        }

        authzClient = AuthzClient.create(authzClientConfig);
        httpClient = authzClient.getConfiguration().getHttpClient();
        pathMatcher = new PathConfigMatcher(builder.getEnforcerConfig(), authzClient);
        paths = pathMatcher.getPathConfig();

        loadClaimInformationPointProviders(ServiceLoader.load(ClaimInformationPointProviderFactory.class, ClaimInformationPointProviderFactory.class.getClassLoader()));
        loadClaimInformationPointProviders(ServiceLoader.load(ClaimInformationPointProviderFactory.class, Thread.currentThread().getContextClassLoader()));
    }

    public AuthorizationContext enforce(HttpRequest request, HttpResponse response) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Policy enforcement is enabled. Enforcing policy decisions for path [{0}].", request.getURI());
        }

        AuthorizationContext context = authorize(request, response);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Policy enforcement result for path [{0}] is : {1}", request.getURI(), context.isGranted() ? "GRANTED" : "DENIED");
            LOGGER.debugv("Returning authorization context with permissions:");
            for (Permission permission : context.getPermissions()) {
                LOGGER.debug(permission);
            }
        }

        return context;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public AuthzClient getAuthzClient() {
        return authzClient;
    }

    public Map<String, PathConfig> getPaths() {
        return Collections.unmodifiableMap(paths);
    }

    public Map<String, ClaimInformationPointProviderFactory> getClaimInformationPointProviderFactories() {
        return claimInformationPointProviderFactories;
    }

    public PathConfigMatcher getPathMatcher() {
        return pathMatcher;
    }

    private AuthorizationContext authorize(HttpRequest request, HttpResponse response) {
        EnforcementMode enforcementMode = enforcerConfig.getEnforcementMode();
        TokenPrincipal principal = request.getPrincipal();
        boolean anonymous = principal == null || principal.getRawToken() == null;

        if (EnforcementMode.DISABLED.equals(enforcementMode)) {
            if (anonymous) {
                response.sendError(401, "Invalid bearer");
            }
            return createEmptyAuthorizationContext(true);
        }

        PathConfig pathConfig = getPathConfig(request);

        if (anonymous) {
            if (!isDefaultAccessDeniedUri(request)) {
                if (pathConfig != null) {
                    if (EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                        return createEmptyAuthorizationContext(true);
                    } else {
                        challenge(pathConfig, getRequiredScopes(pathConfig, request), request, response);
                    }
                } else {
                    handleAccessDenied(response);
                }
            }
            return createEmptyAuthorizationContext(false);
        }

        AccessToken accessToken = principal.getToken();

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

                handleAccessDenied(response);

                return createEmptyAuthorizationContext(false);
            }

            if (EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                return createAuthorizationContext(accessToken, pathConfig);
            }

            MethodConfig methodConfig = getRequiredScopes(pathConfig, request);
            Map<String, List<String>> claims = resolveClaims(pathConfig, request);

            if (isAuthorized(pathConfig, methodConfig, accessToken, request, claims)) {
                try {
                    return createAuthorizationContext(accessToken, pathConfig);
                } catch (Exception e) {
                    throw new RuntimeException("Error processing path [" + pathConfig.getPath() + "].", e);
                }
            }

            AccessToken original = accessToken;

            accessToken = requestAuthorizationToken(pathConfig, methodConfig, request, claims);

            if (accessToken != null) {
                AccessToken.Authorization authorization = original.getAuthorization();

                if (authorization == null) {
                    authorization = new AccessToken.Authorization();
                    authorization.setPermissions(new ArrayList<Permission>());
                }

                AccessToken.Authorization newAuthorization = accessToken.getAuthorization();

                if (newAuthorization != null) {
                    Collection<Permission> grantedPermissions = authorization.getPermissions();
                    Collection<Permission> newPermissions = newAuthorization.getPermissions();

                    for (Permission newPermission : newPermissions) {
                        if (!grantedPermissions.contains(newPermission)) {
                            grantedPermissions.add(newPermission);
                        }
                    }
                }

                original.setAuthorization(authorization);

                if (isAuthorized(pathConfig, methodConfig, accessToken, request, claims)) {
                    try {
                        return createAuthorizationContext(accessToken, pathConfig);
                    } catch (Exception e) {
                        throw new RuntimeException("Error processing path [" + pathConfig.getPath() + "].", e);
                    }
                }
            }

            if (methodConfig != null && ScopeEnforcementMode.DISABLED.equals(methodConfig.getScopesEnforcementMode())) {
                return createEmptyAuthorizationContext(true);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Sending challenge to the client. Path [%s]", pathConfig);
            }

            if (!challenge(pathConfig, methodConfig, request, response)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf("Challenge not sent, sending default forbidden response. Path [%s]", pathConfig);
                }
                handleAccessDenied(response);
            }
        }

        return createEmptyAuthorizationContext(false);
    }

    protected boolean isAuthorized(PathConfig actualPathConfig, MethodConfig methodConfig, AccessToken accessToken, HttpRequest request, Map<String, List<String>> claims) {
        if (isDefaultAccessDeniedUri(request)) {
            return true;
        }

        Authorization authorization = accessToken.getAuthorization();

        if (authorization == null) {
            return false;
        }

        boolean hasPermission = false;
        Collection<Permission> grantedPermissions = authorization.getPermissions();

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
                            pathMatcher.removeFromCache(getPath(request));
                        }

                        return hasValidClaims(permission, claims);
                    }
                }
            } else {
                if (hasResourceScopePermission(methodConfig, permission)) {
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

    protected Map<String, List<String>> resolveClaims(PathConfig pathConfig, HttpRequest request) {
        Map<String, List<String>> claims = new HashMap<>();

        resolveClaims(claims, enforcerConfig.getClaimInformationPointConfig(), request);
        resolveClaims(claims, pathConfig.getClaimInformationPointConfig(), request);

        return claims;
    }

    protected boolean challenge(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, HttpRequest request, HttpResponse response) {
        if (isBearerAuthorization(request)) {
            String ticket = getPermissionTicket(pathConfig, methodConfig, authzClient, request);

            if (ticket != null) {
                response.setHeader("WWW-Authenticate", new StringBuilder("UMA realm=\"").append(authzClient.getConfiguration().getRealm()).append("\"").append(",as_uri=\"")
                        .append(authzClient.getServerConfiguration().getIssuer()).append("\"").append(",ticket=\"").append(ticket).append("\"").toString());
                response.sendError(401);
            } else {
                response.sendError(403);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sending challenge");
            }

            return true;
        }

        handleAccessDenied(response);

        return true;
    }

    protected void handleAccessDenied(HttpResponse response) {
        String accessDeniedPath = enforcerConfig.getOnDenyRedirectTo();

        if (accessDeniedPath != null) {
            response.setHeader("Location", accessDeniedPath);
            response.sendError(302);
        } else {
            response.sendError(403);
        }
    }

    private boolean hasValidClaims(Permission permission, Map<String, List<String>> claims) {
        Map<String, Set<String>> grantedClaims = permission.getClaims();

        if (grantedClaims != null) {
            if (claims.isEmpty()) {
                return false;
            }

            for (Entry<String, Set<String>> entry : grantedClaims.entrySet()) {
                List<String> requestClaims = claims.get(entry.getKey());

                if (requestClaims == null || requestClaims.isEmpty() || !entry.getValue().containsAll(requestClaims)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isDefaultAccessDeniedUri(HttpRequest request) {
        String accessDeniedPath = enforcerConfig.getOnDenyRedirectTo();
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

    private String getPath(HttpRequest request) {
        return request.getRelativePath();
    }

    private MethodConfig getRequiredScopes(PathConfig pathConfig, HttpRequest request) {
        String method = request.getMethod();

        for (MethodConfig methodConfig : pathConfig.getMethods()) {
            if (methodConfig.getMethod().equals(method)) {
                return methodConfig;
            }
        }

        MethodConfig methodConfig = new MethodConfig();

        methodConfig.setMethod(request.getMethod());
        List scopes = new ArrayList<>();

        if (Boolean.TRUE.equals(enforcerConfig.getHttpMethodAsScope())) {
            scopes.add(request.getMethod());
        } else {
            scopes.addAll(pathConfig.getScopes());
        }

        methodConfig.setScopes(scopes);
        methodConfig.setScopesEnforcementMode(PolicyEnforcerConfig.ScopeEnforcementMode.ANY);

        return methodConfig;
    }

    private AuthorizationContext createAuthorizationContext(AccessToken accessToken, PathConfig pathConfig) {
        return new ClientAuthorizationContext(accessToken, pathConfig, authzClient);
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

    private PathConfig getPathConfig(HttpRequest request) {
        return isDefaultAccessDeniedUri(request) ? null : pathMatcher.matches(getPath(request));
    }

    private AccessToken requestAuthorizationToken(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, HttpRequest request, Map<String, List<String>> claims) {
        if (enforcerConfig.getUserManagedAccess() != null) {
            return null;
        }

        try {
            TokenPrincipal principal = request.getPrincipal();
            String accessTokenString = principal.getRawToken();
            AccessToken accessToken = principal.getToken();
            AuthorizationRequest authzRequest = new AuthorizationRequest();

            if (isBearerAuthorization(request) || accessToken.getAuthorization() != null) {
                authzRequest.addPermission(pathConfig.getId(), methodConfig.getScopes());
            }

            if (!claims.isEmpty()) {
                authzRequest.setClaimTokenFormat("urn:ietf:params:oauth:token-type:jwt");
                authzRequest.setClaimToken(Base64.encodeBytes(JsonSerialization.writeValueAsBytes(claims)));
            }

            if (accessToken.getAuthorization() != null) {
                authzRequest.setRpt(accessTokenString);
            }

            LOGGER.debug("Obtaining authorization for authenticated user.");
            AuthorizationResponse authzResponse;

            if (isBearerAuthorization(request)) {
                authzRequest.setSubjectToken(accessTokenString);
                authzResponse = authzClient.authorization().authorize(authzRequest);
            } else {
                authzResponse = authzClient.authorization(accessTokenString).authorize(authzRequest);
            }

            if (authzResponse != null) {
                return asAccessToken(authzResponse.getToken());
            }
        } catch (AuthorizationDeniedException ignore) {
            LOGGER.debug("Authorization denied", ignore);
        } catch (Exception e) {
            LOGGER.debug("Authorization failed", e);
        }

        return null;
    }

    private String getPermissionTicket(PathConfig pathConfig, MethodConfig methodConfig, AuthzClient authzClient, HttpRequest httpFacade) {
        if (enforcerConfig.getUserManagedAccess() != null) {
            ProtectionResource protection = authzClient.protection();
            PermissionResource permission = protection.permission();
            PermissionRequest permissionRequest = new PermissionRequest();

            permissionRequest.setResourceId(pathConfig.getId());
            permissionRequest.setScopes(new HashSet<>(methodConfig.getScopes()));

            Map<String, List<String>> claims = resolveClaims(pathConfig, httpFacade);

            if (!claims.isEmpty()) {
                permissionRequest.setClaims(claims);
            }

            return permission.create(permissionRequest).getTicket();
        }

        return null;
    }

    private boolean isBearerAuthorization(HttpRequest request) {
        List<String> authHeaders = request.getHeaders("Authorization");

        if (authHeaders != null) {
            for (String authHeader : authHeaders) {
                String[] split = authHeader.trim().split("\\s+");
                if (split == null || split.length != 2) continue;
                if (!split[0].equalsIgnoreCase("Bearer")) continue;
                return true;
            }
        }

        return authzClient.getConfiguration().isBearerOnly();
    }

    private void loadClaimInformationPointProviders(ServiceLoader<ClaimInformationPointProviderFactory> loader) {
        for (ClaimInformationPointProviderFactory factory : loader) {
            factory.init(this);

            claimInformationPointProviderFactories.put(factory.getName(), factory);
        }
    }

    private void resolveClaims(Map<String, List<String>> claims, Map<String, Map<String, Object>> claimInformationPointConfig, HttpRequest request) {
        if (claimInformationPointConfig != null) {
            for (Entry<String, Map<String, Object>> claimDef : claimInformationPointConfig.entrySet()) {
                ClaimInformationPointProviderFactory factory = claimInformationPointProviderFactories.get(claimDef.getKey());

                if (factory != null) {
                    claims.putAll(factory.create(claimDef.getValue()).resolve(request));
                }
            }
        }
    }

    public static class Builder {

        Configuration authzClientConfig = new Configuration();

        private Builder() {
        }

        public Builder authServerUrl(String authServerUrl) {
            authzClientConfig.setAuthServerUrl(authServerUrl);
            return this;
        }

        public Builder realm(String realm) {
            authzClientConfig.setRealm(realm);
            return this;
        }

        public Builder clientId(String clientId) {
            authzClientConfig.setResource(clientId);
            return this;
        }

        public Builder bearerOnly(boolean bearerOnly) {
            authzClientConfig.setBearerOnly(bearerOnly);
            return this;
        }

        public Builder credentials(Map<String, Object> credentials) {
            authzClientConfig.setCredentials(credentials);
            return this;
        }

        public Builder enforcerConfig(PolicyEnforcerConfig enforcerConfig) {
            authzClientConfig.setPolicyEnforcerConfig(enforcerConfig);
            return this;
        }

        public Builder enforcerConfig(InputStream is) {
            try {
                enforcerConfig(JsonSerialization.readValue(is, PolicyEnforcerConfig.class));
            } catch (Exception cause) {
                throw new RuntimeException("Failed to read configuration", cause);
            }
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            authzClientConfig.setHttpClient(httpClient);
            return this;
        }

        public Builder credentialProvider(ClientCredentialsProvider credentialsProvider) {
            authzClientConfig.setClientCredentialsProvider(credentialsProvider);
            return this;
        }

        public PolicyEnforcer build() {
            return new PolicyEnforcer(this);
        }

        PolicyEnforcerConfig getEnforcerConfig() {
            return authzClientConfig.getPolicyEnforcerConfig();
        }
    }
}
