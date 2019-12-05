/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.protection.permission;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AbstractPermissionService {

    private final AuthorizationProvider authorization;
    private final KeycloakIdentity identity;
    private final ResourceServer resourceServer;

    public AbstractPermissionService(KeycloakIdentity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
    }

    public Response create(List<PermissionRequest> request) {
        if (request == null || request.isEmpty()) {
            throw new ErrorResponseException("invalid_permission_request", "Invalid permission request.", Response.Status.BAD_REQUEST);
        }

        return Response.status(Response.Status.CREATED).entity(new PermissionResponse(createPermissionTicket(request))).build();
    }

    private List<Permission> verifyRequestedResource(List<PermissionRequest> request) {
        ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
        List<Permission> requestedResources = new ArrayList<>();

        for (PermissionRequest permissionRequest : request) {
            String resourceSetId = permissionRequest.getResourceId();
            List<Resource> resources = new ArrayList<>();

            if (resourceSetId == null) {
                if (permissionRequest.getScopes() == null || permissionRequest.getScopes().isEmpty()) {
                    throw new ErrorResponseException("invalid_resource_id", "Resource id or name not provided.", Response.Status.BAD_REQUEST);
                }
            } else {
                Resource resource = resourceStore.findById(resourceSetId, resourceServer.getId());

                if (resource != null) {
                    resources.add(resource);
                } else {
                    Resource userResource = resourceStore.findByName(resourceSetId, identity.getId(), this.resourceServer.getId());

                    if (userResource != null) {
                        resources.add(userResource);
                    }

                    if (!identity.isResourceServer()) {
                        Resource serverResource = resourceStore.findByName(resourceSetId, this.resourceServer.getId());

                        if (serverResource != null) {
                            resources.add(serverResource);
                        }
                    }
                }

                if (resources.isEmpty()) {
                    throw new ErrorResponseException("invalid_resource_id", "Resource set with id [" + resourceSetId + "] does not exists in this server.", Response.Status.BAD_REQUEST);
                }
            }

            if (resources.isEmpty()) {
                requestedResources.add(new Permission(null, verifyRequestedScopes(permissionRequest, null)));
            } else {
                for (Resource resource : resources) {
                    requestedResources.add(new Permission(resource.getId(), verifyRequestedScopes(permissionRequest, resource)));
                }
            }
        }

        return requestedResources;
    }

    private Set<String> verifyRequestedScopes(PermissionRequest request, Resource resource) {
        Set<String> requestScopes = request.getScopes();

        if (requestScopes == null) {
            return Collections.emptySet();
        }

        ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();

        return requestScopes.stream().map(scopeName -> {
            Scope scope = null;

            if (resource != null) {
                scope = resource.getScopes().stream().filter(scope1 -> scope1.getName().equals(scopeName)).findFirst().orElse(null);

                if (scope == null && resource.getType() != null) {
                    scope = resourceStore.findByType(resource.getType(), resourceServer.getId()).stream()
                            .filter(baseResource -> baseResource.getOwner().equals(resource.getResourceServer().getId()))
                            .flatMap(resource1 -> resource1.getScopes().stream())
                            .filter(baseScope -> baseScope.getName().equals(scopeName)).findFirst().orElse(null);
                }
            } else {
                scope = authorization.getStoreFactory().getScopeStore().findByName(scopeName, resourceServer.getId());
            }

            if (scope == null) {
                throw new ErrorResponseException("invalid_scope", "Scope [" + scopeName + "] is invalid", Response.Status.BAD_REQUEST);
            }

            return scope.getName();
        }).collect(Collectors.toSet());
    }

    private String createPermissionTicket(List<PermissionRequest> request) {
        List<Permission> permissions = verifyRequestedResource(request);

        String audience = Urls.realmIssuer(this.authorization.getKeycloakSession().getContext().getUri().getBaseUri(), this.authorization.getRealm().getName());
        PermissionTicketToken token = new PermissionTicketToken(permissions, audience, this.identity.getAccessToken());
        Map<String, List<String>> claims = new HashMap<>();

        for (PermissionRequest permissionRequest : request) {
            Map<String, List<String>> requestClaims = permissionRequest.getClaims();

            if (requestClaims != null) {
                claims.putAll(requestClaims);
            }
        }

        if (!claims.isEmpty()) {
            token.setClaims(claims);
        }

        return this.authorization.getKeycloakSession().tokens().encode(token);
    }
}