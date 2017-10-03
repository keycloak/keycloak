/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.protection;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.ResourceSetService;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.protection.permission.PermissionService;
import org.keycloak.authorization.protection.permission.PermissionsService;
import org.keycloak.authorization.protection.resource.ResourceService;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ProtectionService {

    private final AuthorizationProvider authorization;
    @Context
    protected ClientConnection clientConnection;

    public ProtectionService(AuthorizationProvider authorization) {
        this.authorization = authorization;
    }

    @Path("/resource_set")
    public Object resource() {
        KeycloakIdentity identity = createIdentity();
        ResourceServer resourceServer = getResourceServer(identity);
        RealmModel realm = authorization.getRealm();
        ClientModel client = realm.getClientById(identity.getId());
        KeycloakSession keycloakSession = authorization.getKeycloakSession();
        UserModel serviceAccount = keycloakSession.users().getServiceAccount(client);
        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, new AdminAuth(realm, identity.getAccessToken(), serviceAccount, client), keycloakSession, clientConnection);
        ResourceSetService resourceManager = new ResourceSetService(resourceServer, this.authorization, null, adminEvent.realm(realm).authClient(client).authUser(serviceAccount));

        ResteasyProviderFactory.getInstance().injectProperties(resourceManager);

        ResourceService resource = new ResourceService(resourceServer, identity, resourceManager, this.authorization);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("/permission")
    public Object permission() {
        KeycloakIdentity identity = createIdentity();

        PermissionService resource = new PermissionService(identity, getResourceServer(identity), this.authorization);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("/permissions")
    public Object permissions() {
        KeycloakIdentity identity = createIdentity();

        PermissionsService resource = new PermissionsService(identity, getResourceServer(identity), this.authorization);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    private KeycloakIdentity createIdentity() {
        KeycloakIdentity identity = new KeycloakIdentity(this.authorization.getKeycloakSession());
        ResourceServer resourceServer = getResourceServer(identity);
        KeycloakSession keycloakSession = authorization.getKeycloakSession();
        RealmModel realm = keycloakSession.getContext().getRealm();
        ClientModel client = realm.getClientById(resourceServer.getId());

        if (!identity.hasClientRole(client.getClientId(), "uma_protection")) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_SCOPE, "Requires uma_protection scope.", Status.FORBIDDEN);
        }

        return identity;
    }

    private ResourceServer getResourceServer(Identity identity) {
        RealmModel realm = this.authorization.getKeycloakSession().getContext().getRealm();
        ClientModel clientApplication = realm.getClientById(identity.getId());

        if (clientApplication == null) {
            throw new ErrorResponseException("invalid_clientId", "Client application with id [" + identity.getId() + "] does not exist in realm [" + realm.getName() + "]", Status.BAD_REQUEST);
        }

        ResourceServer resourceServer = this.authorization.getStoreFactory().getResourceServerStore().findById(identity.getId());

        if (resourceServer == null) {
            throw new ErrorResponseException("invalid_clientId", "Client application [" + clientApplication.getClientId() + "] is not registered as resource server.", Status.FORBIDDEN);
        }

        return resourceServer;
    }
}
