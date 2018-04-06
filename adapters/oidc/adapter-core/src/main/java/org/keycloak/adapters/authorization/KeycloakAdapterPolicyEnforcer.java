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
import java.util.HashSet;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.rotation.AdapterRSATokenVerifier;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakAdapterPolicyEnforcer extends AbstractPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(KeycloakAdapterPolicyEnforcer.class);

    public KeycloakAdapterPolicyEnforcer(PolicyEnforcer policyEnforcer) {
        super(policyEnforcer);
    }

    @Override
    protected boolean isAuthorized(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, AccessToken accessToken, OIDCHttpFacade httpFacade) {
        AccessToken original = accessToken;

        if (super.isAuthorized(pathConfig, methodConfig, accessToken, httpFacade)) {
            return true;
        }

        accessToken = requestAuthorizationToken(pathConfig, methodConfig, httpFacade);

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

        return super.isAuthorized(pathConfig, methodConfig, accessToken, httpFacade);
    }

    @Override
    protected boolean challenge(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, OIDCHttpFacade facade) {
        handleAccessDenied(facade);
        return true;
    }

    @Override
    protected void handleAccessDenied(OIDCHttpFacade facade) {
        KeycloakSecurityContext securityContext = facade.getSecurityContext();

        if (securityContext == null) {
            return;
        }

        String accessDeniedPath = getEnforcerConfig().getOnDenyRedirectTo();
        HttpFacade.Response response = facade.getResponse();

        if (accessDeniedPath != null) {
            response.setStatus(302);
            response.setHeader("Location", accessDeniedPath);
        } else {
            response.sendError(403);
        }
    }

    private AccessToken requestAuthorizationToken(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, OIDCHttpFacade httpFacade) {
        try {
            KeycloakSecurityContext securityContext = httpFacade.getSecurityContext();
            String accessTokenString = securityContext.getTokenString();
            AuthzClient authzClient = getAuthzClient();
            KeycloakDeployment deployment = getPolicyEnforcer().getDeployment();
            PermissionRequest permissionRequest = new PermissionRequest();

            permissionRequest.setResourceId(pathConfig.getId());
            permissionRequest.setScopes(new HashSet<>(methodConfig.getScopes()));

            AccessToken accessToken = securityContext.getToken();
            AuthorizationRequest authzRequest;

            if (getEnforcerConfig().getUserManagedAccess() != null) {
                PermissionResponse permissionResponse = authzClient.protection().permission().create(permissionRequest);
                authzRequest = new AuthorizationRequest();
                authzRequest.setTicket(permissionResponse.getTicket());
            } else {
                authzRequest = new AuthorizationRequest();
                if (accessToken.getAuthorization() != null) {
                    authzRequest.addPermission(pathConfig.getId(), methodConfig.getScopes());
                }
            }

            if (accessToken.getAuthorization() != null) {
                authzRequest.setRpt(accessTokenString);
            }

            LOGGER.debug("Obtaining authorization for authenticated user.");
            AuthorizationResponse authzResponse = authzClient.authorization(accessTokenString).authorize(authzRequest);

            if (authzResponse != null) {
                return AdapterRSATokenVerifier.verifyToken(authzResponse.getToken(), deployment);
            }

            return null;
        } catch (AuthorizationDeniedException e) {
            LOGGER.debug("Authorization denied", e);
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during authorization request.", e);
        }
    }
}
