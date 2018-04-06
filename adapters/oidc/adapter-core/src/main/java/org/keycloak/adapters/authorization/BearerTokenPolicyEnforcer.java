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

import java.util.HashSet;

import org.jboss.logging.Logger;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.authorization.PermissionRequest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class BearerTokenPolicyEnforcer extends AbstractPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(BearerTokenPolicyEnforcer.class);

    public BearerTokenPolicyEnforcer(PolicyEnforcer enforcer) {
        super(enforcer);
    }

    @Override
    protected boolean challenge(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, OIDCHttpFacade facade) {
        HttpFacade.Response response = facade.getResponse();
        AuthzClient authzClient = getAuthzClient();
        String ticket = getPermissionTicket(pathConfig, methodConfig, authzClient);

        if (ticket == null) {
            response.setStatus(403);
            return true;
        }

        String realm = authzClient.getConfiguration().getRealm();
        String authorizationServerUri = authzClient.getServerConfiguration().getIssuer().toString();
        response.setStatus(401);
        StringBuilder wwwAuthenticate = new StringBuilder("UMA realm=\"").append(realm).append("\"").append(",as_uri=\"").append(authorizationServerUri).append("\"");

        if (ticket != null) {
            wwwAuthenticate.append(",ticket=\"").append(ticket).append("\"");
        }

        response.setHeader("WWW-Authenticate", wwwAuthenticate.toString());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending UMA challenge");
        }
        return true;
    }

    private String getPermissionTicket(PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, AuthzClient authzClient) {
        if (getEnforcerConfig().getUserManagedAccess() != null) {
            ProtectionResource protection = authzClient.protection();
            PermissionResource permission = protection.permission();
            PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.setResourceId(pathConfig.getId());
            permissionRequest.setScopes(new HashSet<>(methodConfig.getScopes()));
            return permission.create(permissionRequest).getTicket();
        }
        return null;
    }
}