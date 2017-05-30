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
package org.keycloak.authorization.entitlement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.KeycloakEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.entitlement.representation.EntitlementRequest;
import org.keycloak.authorization.entitlement.representation.EntitlementResponse;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.Cors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class EntitlementService {

    protected static final Logger logger = Logger.getLogger(EntitlementService.class);
    private final AuthorizationProvider authorization;

    @Context
    private HttpRequest request;

    @Context
    private KeycloakSession session;

    public EntitlementService(AuthorizationProvider authorization) {
        this.authorization = authorization;
    }

    @Path("{resource_server_id}")
    @OPTIONS
    public Response authorizePreFlight(@PathParam("resource_server_id") String resourceServerId) {
        return Cors.add(this.request, Response.ok()).auth().preflight().build();
    }

    @Path("{resource_server_id}")
    @GET()
    @Produces("application/json")
    @Consumes("application/json")
    public Response getAll(@PathParam("resource_server_id") String resourceServerId) {
        KeycloakIdentity identity = new KeycloakIdentity(this.authorization.getKeycloakSession());

        if (resourceServerId == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Requires resource_server_id request parameter.", Status.BAD_REQUEST);
        }

        RealmModel realm = this.authorization.getKeycloakSession().getContext().getRealm();
        ClientModel client = realm.getClientByClientId(resourceServerId);

        if (client == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Identifier is not associated with any client and resource server.", Status.BAD_REQUEST);
        }

        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client.getId());

        if (resourceServer == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client does not support permissions", Status.FORBIDDEN);
        }

        return evaluate(Permissions.all(resourceServer, identity, authorization), identity, resourceServer);
    }

    @Path("{resource_server_id}")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response get(@PathParam("resource_server_id") String resourceServerId, EntitlementRequest entitlementRequest) {
        KeycloakIdentity identity = new KeycloakIdentity(this.authorization.getKeycloakSession());

        if (entitlementRequest == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid entitlement request.", Status.BAD_REQUEST);
        }

        if (resourceServerId == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid resource_server_id.", Status.BAD_REQUEST);
        }

        RealmModel realm = this.authorization.getKeycloakSession().getContext().getRealm();

        ClientModel client = realm.getClientByClientId(resourceServerId);

        if (client == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Identifier is not associated with any resource server.", Status.BAD_REQUEST);
        }

        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client.getId());

        if (resourceServer == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client does not support permissions", Status.FORBIDDEN);
        }

        return evaluate(createPermissions(entitlementRequest, resourceServer, authorization), identity, resourceServer);
    }

    private Response evaluate(List<ResourcePermission> permissions, KeycloakIdentity identity, ResourceServer resourceServer) {
        try {
            List<Result> result = authorization.evaluators().from(permissions, new KeycloakEvaluationContext(this.authorization.getKeycloakSession())).evaluate();
            List<Permission> entitlements = Permissions.permits(result, authorization, resourceServer.getId());

            if (!entitlements.isEmpty()) {
                return Cors.add(request, Response.ok().entity(new EntitlementResponse(createRequestingPartyToken(entitlements, identity.getAccessToken())))).allowedOrigins(identity.getAccessToken()).allowedMethods("GET").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
            }
        } catch (Exception cause) {
            logger.error(cause);
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, "Error while evaluating permissions.", Status.INTERNAL_SERVER_ERROR);
        }

        HashMap<Object, Object> error = new HashMap<>();

        error.put(OAuth2Constants.ERROR, "not_authorized");

        return Cors.add(request, Response.status(Status.FORBIDDEN)
                .entity(error))
                .allowedOrigins(identity.getAccessToken())
                .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private String createRequestingPartyToken(List<Permission> permissions, AccessToken accessToken) {
        RealmModel realm = this.authorization.getKeycloakSession().getContext().getRealm();
        AccessToken.Authorization authorization = new AccessToken.Authorization();

        authorization.setPermissions(permissions);
        accessToken.setAuthorization(authorization);

        return new TokenManager().encodeToken(this.authorization.getKeycloakSession(), realm, accessToken);
    }

    private List<ResourcePermission> createPermissions(EntitlementRequest entitlementRequest, ResourceServer resourceServer, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        Map<String, Set<String>> permissionsToEvaluate = new HashMap<>();

        entitlementRequest.getPermissions().forEach(requestedResource -> {
            Resource resource;

            if (requestedResource.getResourceSetId() != null) {
                resource = storeFactory.getResourceStore().findById(requestedResource.getResourceSetId(), resourceServer.getId());
            } else {
                resource = storeFactory.getResourceStore().findByName(requestedResource.getResourceSetName(), resourceServer.getId());
            }

            if (resource == null && (requestedResource.getScopes() == null || requestedResource.getScopes().isEmpty())) {
                throw new ErrorResponseException("invalid_resource", "Resource with id [" + requestedResource.getResourceSetId() + "] or name [" + requestedResource.getResourceSetName() + "] does not exist.", Status.FORBIDDEN);
            }

            Set<ScopeRepresentation> requestedScopes = requestedResource.getScopes().stream().map(ScopeRepresentation::new).collect(Collectors.toSet());
            Set<String> collect = requestedScopes.stream().map(ScopeRepresentation::getName).collect(Collectors.toSet());

            if (resource != null) {
                permissionsToEvaluate.put(resource.getId(), collect);
            } else {
                ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
                ScopeStore scopeStore = authorization.getStoreFactory().getScopeStore();
                List<Resource> resources = new ArrayList<Resource>();

                resources.addAll(resourceStore.findByScope(requestedScopes.stream().map(scopeRepresentation -> {
                    Scope scope = scopeStore.findByName(scopeRepresentation.getName(), resourceServer.getId());

                    if (scope == null) {
                        return null;
                    }

                    return scope.getId();
                }).filter(s -> s != null).collect(Collectors.toList()), resourceServer.getId()));

                for (Resource resource1 : resources) {
                    permissionsToEvaluate.put(resource1.getId(), collect);
                }

                permissionsToEvaluate.put("$KC_SCOPE_PERMISSION", collect);
            }
        });

        String rpt = entitlementRequest.getRpt();

        if (rpt != null && !"".equals(rpt)) {
            KeycloakContext context = authorization.getKeycloakSession().getContext();
            if (!Tokens.verifySignature(session, context.getRealm(), rpt)) {
                throw new ErrorResponseException("invalid_rpt", "RPT signature is invalid", Status.FORBIDDEN);
            }

            AccessToken requestingPartyToken;

            try {
                requestingPartyToken = new JWSInput(rpt).readJsonContent(AccessToken.class);
            } catch (JWSInputException e) {
                throw new ErrorResponseException("invalid_rpt", "Invalid RPT", Status.FORBIDDEN);
            }

            if (requestingPartyToken.isActive()) {
                AccessToken.Authorization authorizationData = requestingPartyToken.getAuthorization();

                if (authorizationData != null) {
                    List<Permission> permissions = authorizationData.getPermissions();

                    if (permissions != null) {
                        permissions.forEach(permission -> {
                            Resource resourcePermission = storeFactory.getResourceStore().findById(permission.getResourceSetId(), resourceServer.getId());

                            if (resourcePermission != null) {
                                Set<String> scopes = permissionsToEvaluate.get(resourcePermission.getId());

                                if (scopes == null) {
                                    scopes = new HashSet<>();
                                    permissionsToEvaluate.put(resourcePermission.getId(), scopes);
                                }

                                Set<String> scopePermission = permission.getScopes();

                                if (scopePermission != null) {
                                    scopes.addAll(scopePermission);
                                }
                            }
                        });
                    }
                }
            }
        }

        return permissionsToEvaluate.entrySet().stream()
                .flatMap((Function<Map.Entry<String, Set<String>>, Stream<ResourcePermission>>) entry -> {
                    String key = entry.getKey();

                    if ("$KC_SCOPE_PERMISSION".equals(key)) {
                        ScopeStore scopeStore = authorization.getStoreFactory().getScopeStore();
                        List<Scope> scopes = entry.getValue().stream().map(scopeName -> scopeStore.findByName(scopeName, resourceServer.getId())).filter(scope -> Objects.nonNull(scope)).collect(Collectors.toList());
                        return Arrays.asList(new ResourcePermission(null, scopes, resourceServer)).stream();
                    } else {
                        Resource entryResource = storeFactory.getResourceStore().findById(key, resourceServer.getId());
                        return Permissions.createResourcePermissions(entryResource, entry.getValue(), authorization).stream();
                    }
                }).collect(Collectors.toList());
    }
}
