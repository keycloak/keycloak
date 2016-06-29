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
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.PermissionRequest;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class BearerTokenPolicyEnforcer extends AbstractPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(BearerTokenPolicyEnforcer.class);

    public BearerTokenPolicyEnforcer(PolicyEnforcer enforcer) {
        super(enforcer);
    }

    @Override
    protected boolean challenge(PathConfig pathConfig, Set<String> requiredScopes, OIDCHttpFacade facade) {
        if (getEnforcerConfig().getUmaProtocolConfig() != null) {
            challengeUmaAuthentication(pathConfig, requiredScopes, facade);
        } else {
            challengeEntitlementAuthentication(facade);
        }
        return true;
    }

    private void challengeEntitlementAuthentication(OIDCHttpFacade facade) {
        HttpFacade.Response response = facade.getResponse();
        AuthzClient authzClient = getAuthzClient();
        String clientId = authzClient.getConfiguration().getClientId();
        String  authorizationServerUri = authzClient.getServerConfiguration().getIssuer().toString() + "/authz/entitlement";
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "KC_ETT realm=\"" + clientId + "\",as_uri=\"" + authorizationServerUri + "\"");
    }

    private void challengeUmaAuthentication(PathConfig pathConfig, Set<String> requiredScopes, OIDCHttpFacade facade) {
        HttpFacade.Response response = facade.getResponse();
        AuthzClient authzClient = getAuthzClient();
        String ticket = getPermissionTicket(pathConfig, requiredScopes, authzClient);
        String clientId = authzClient.getConfiguration().getClientId();
        String authorizationServerUri = authzClient.getServerConfiguration().getIssuer().toString() + "/authz/authorize";
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "UMA realm=\"" + clientId + "\",as_uri=\"" + authorizationServerUri + "\",ticket=\"" + ticket + "\"");
    }

    private String getPermissionTicket(PathConfig pathConfig, Set<String> requiredScopes, AuthzClient authzClient) {
        ProtectionResource protection = authzClient.protection();
        PermissionResource permission = protection.permission();
        PermissionRequest permissionRequest = new PermissionRequest();
        permissionRequest.setResourceSetId(pathConfig.getId());
        permissionRequest.setScopes(requiredScopes);
        return permission.forResource(permissionRequest).getTicket();
    }
}