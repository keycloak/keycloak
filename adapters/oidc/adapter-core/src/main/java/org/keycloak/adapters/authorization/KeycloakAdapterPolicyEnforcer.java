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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.common.util.Base64;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakAdapterPolicyEnforcer extends AbstractPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(KeycloakAdapterPolicyEnforcer.class);

    public KeycloakAdapterPolicyEnforcer(PolicyEnforcer policyEnforcer) {
        super(policyEnforcer);
    }

    @Override
    protected boolean isAuthorized(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, AccessToken accessToken, OIDCHttpFacade httpFacade, Map<String, List<String>> claims) {
        AccessToken original = accessToken;

        if (super.isAuthorized(pathConfig, methodConfig, accessToken, httpFacade, claims)) {
            return true;
        }

        accessToken = requestAuthorizationToken(pathConfig, methodConfig, httpFacade, claims);

        if (accessToken == null) {
            return false;
        }

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

        return super.isAuthorized(pathConfig, methodConfig, accessToken, httpFacade, claims);
    }

    @Override
    protected boolean challenge(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, OIDCHttpFacade httpFacade) {
        if (isBearerAuthorization(httpFacade)) {
            HttpFacade.Response response = httpFacade.getResponse();
            AuthzClient authzClient = getAuthzClient();
            String ticket = getPermissionTicket(pathConfig, methodConfig, authzClient, httpFacade);

            if (ticket != null) {
                response.setStatus(401);
                response.setHeader("WWW-Authenticate", new StringBuilder("UMA realm=\"").append(authzClient.getConfiguration().getRealm()).append("\"").append(",as_uri=\"")
                        .append(authzClient.getServerConfiguration().getIssuer()).append("\"").append(",ticket=\"").append(ticket).append("\"").toString());
            } else {
                response.setStatus(403);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sending challenge");
            }

            return true;
        }

        handleAccessDenied(httpFacade);

        return true;
    }

    @Override
    protected void handleAccessDenied(OIDCHttpFacade facade) {
        String accessDeniedPath = getEnforcerConfig().getOnDenyRedirectTo();
        HttpFacade.Response response = facade.getResponse();

        if (accessDeniedPath != null) {
            response.setStatus(302);
            response.setHeader("Location", accessDeniedPath);
        } else {
            response.sendError(403);
        }
    }

    private AccessToken requestAuthorizationToken(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, OIDCHttpFacade httpFacade, Map<String, List<String>> claims) {
        if (getEnforcerConfig().getUserManagedAccess() != null) {
            return null;
        }

        try {
            KeycloakSecurityContext securityContext = httpFacade.getSecurityContext();
            String accessTokenString = securityContext.getTokenString();
            KeycloakDeployment deployment = getPolicyEnforcer().getDeployment();
            AccessToken accessToken = securityContext.getToken();
            AuthorizationRequest authzRequest = new AuthorizationRequest();

            if (isBearerAuthorization(httpFacade) || accessToken.getAuthorization() != null) {
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

            if (isBearerAuthorization(httpFacade)) {
                authzRequest.setSubjectToken(accessTokenString);
                authzResponse = getAuthzClient().authorization().authorize(authzRequest);
            } else {
                authzResponse = getAuthzClient().authorization(accessTokenString).authorize(authzRequest);
            }

            if (authzResponse != null) {
                return AdapterTokenVerifier.verifyToken(authzResponse.getToken(), deployment);
            }
        } catch (AuthorizationDeniedException ignore) {
            LOGGER.debug("Authorization denied", ignore);
        } catch (Exception e) {
            LOGGER.debug("Authorization failed", e);
        }

        return null;
    }

    private String getPermissionTicket(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, AuthzClient authzClient, OIDCHttpFacade httpFacade) {
        if (getEnforcerConfig().getUserManagedAccess() != null) {
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

    private boolean isBearerAuthorization(OIDCHttpFacade httpFacade) {
        List<String> authHeaders = httpFacade.getRequest().getHeaders("Authorization");

        if (authHeaders != null) {
            for (String authHeader : authHeaders) {
                String[] split = authHeader.trim().split("\\s+");
                if (split == null || split.length != 2) continue;
                if (!split[0].equalsIgnoreCase("Bearer")) continue;
                return true;
            }
        }

        return getPolicyEnforcer().getDeployment().isBearerOnly();
    }
}
