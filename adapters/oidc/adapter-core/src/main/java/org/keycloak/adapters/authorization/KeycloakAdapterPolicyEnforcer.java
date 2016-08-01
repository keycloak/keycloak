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
import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.AuthorizationRequest;
import org.keycloak.authorization.client.representation.AuthorizationResponse;
import org.keycloak.authorization.client.representation.EntitlementRequest;
import org.keycloak.authorization.client.representation.EntitlementResponse;
import org.keycloak.authorization.client.representation.PermissionRequest;
import org.keycloak.authorization.client.representation.PermissionResponse;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.util.JsonSerialization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakAdapterPolicyEnforcer extends AbstractPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(KeycloakAdapterPolicyEnforcer.class);

    public KeycloakAdapterPolicyEnforcer(PolicyEnforcer policyEnforcer) {
        super(policyEnforcer);
    }

    @Override
    protected boolean isAuthorized(PathConfig pathConfig, Set<String> requiredScopes, AccessToken accessToken, OIDCHttpFacade httpFacade) {
        int retry = 2;
        AccessToken original = accessToken;

        while (retry > 0) {
            if (super.isAuthorized(pathConfig, requiredScopes, accessToken, httpFacade)) {
                return true;
            }

            accessToken = requestAuthorizationToken(pathConfig, requiredScopes, httpFacade);

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
                authorization.getPermissions().addAll(newAuthorization.getPermissions());
            }

            original.setAuthorization(authorization);

            retry--;
        }

        return false;
    }

    @Override
    protected boolean challenge(PathConfig pathConfig, Set<String> requiredScopes, OIDCHttpFacade facade) {
        String accessDeniedPath = getEnforcerConfig().getAccessDeniedPath();
        HttpFacade.Response response = facade.getResponse();

        if (accessDeniedPath != null) {
            response.setStatus(302);
            response.setHeader("Location", accessDeniedPath);
        } else {
            response.sendError(403);
        }

        return true;
    }

    private AccessToken requestAuthorizationToken(PathConfig pathConfig, Set<String> requiredScopes, OIDCHttpFacade httpFacade) {
        try {
            String accessToken = httpFacade.getSecurityContext().getTokenString();
            AuthzClient authzClient = getAuthzClient();
            KeycloakDeployment deployment = getPolicyEnforcer().getDeployment();

            if (getEnforcerConfig().getUmaProtocolConfig() != null) {
                LOGGER.debug("Obtaining authorization for  authenticated user.");
                PermissionRequest permissionRequest = new PermissionRequest();

                permissionRequest.setResourceSetId(pathConfig.getId());
                permissionRequest.setScopes(requiredScopes);

                PermissionResponse permissionResponse = authzClient.protection().permission().forResource(permissionRequest);
                AuthorizationRequest authzRequest = new AuthorizationRequest(permissionResponse.getTicket());
                AuthorizationResponse authzResponse = authzClient.authorization(accessToken).authorize(authzRequest);

                if (authzResponse != null) {
                    return RSATokenVerifier.verifyToken(authzResponse.getRpt(), deployment.getRealmKey(), deployment.getRealmInfoUrl());
                }

                return null;
            } else {
                LOGGER.debug("Obtaining entitlements for authenticated user.");
                AccessToken token = httpFacade.getSecurityContext().getToken();

                if (token.getAuthorization() == null) {
                    EntitlementResponse authzResponse = authzClient.entitlement(accessToken).getAll(authzClient.getConfiguration().getClientId());
                    return RSATokenVerifier.verifyToken(authzResponse.getRpt(), deployment.getRealmKey(), deployment.getRealmInfoUrl());
                } else {
                    EntitlementRequest request = new EntitlementRequest();
                    PermissionRequest permissionRequest = new PermissionRequest();
                    permissionRequest.setResourceSetId(pathConfig.getId());
                    permissionRequest.setResourceSetName(pathConfig.getName());
                    permissionRequest.setScopes(new HashSet<>(pathConfig.getScopes()));
                    request.addPermission(permissionRequest);
                    EntitlementResponse authzResponse = authzClient.entitlement(accessToken).get(authzClient.getConfiguration().getClientId(), request);
                    return RSATokenVerifier.verifyToken(authzResponse.getRpt(), deployment.getRealmKey(), deployment.getRealmInfoUrl());
                }
            }
        } catch (AuthorizationDeniedException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during authorization request.", e);
        }
    }
}
