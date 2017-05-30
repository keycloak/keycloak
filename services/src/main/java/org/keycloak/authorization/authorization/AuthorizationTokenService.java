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
package org.keycloak.authorization.authorization;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.authorization.representation.AuthorizationRequest;
import org.keycloak.authorization.authorization.representation.AuthorizationResponse;
import org.keycloak.authorization.common.KeycloakEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.entitlement.representation.EntitlementResponse;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.protection.permission.PermissionTicket;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationTokenService {
    protected static final Logger logger = Logger.getLogger(AuthorizationTokenService.class);

    private final AuthorizationProvider authorization;

    @Context
    private HttpRequest httpRequest;

    @Context
    private KeycloakSession session;

    public AuthorizationTokenService(AuthorizationProvider authorization) {
        this.authorization = authorization;
    }

    @OPTIONS
    public Response authorizepPreFlight() {
        return Cors.add(this.httpRequest, Response.ok()).auth().preflight().build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response authorize(AuthorizationRequest authorizationRequest) {
        KeycloakEvaluationContext evaluationContext = new KeycloakEvaluationContext(this.authorization.getKeycloakSession());
        KeycloakIdentity identity = (KeycloakIdentity) evaluationContext.getIdentity();

        if (!identity.hasRealmRole("uma_authorization")) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_SCOPE, "Requires uma_authorization scope.", Status.FORBIDDEN);
        }

        if (authorizationRequest == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid authorization request.", Status.BAD_REQUEST);
        }

        try {
            PermissionTicket ticket = verifyPermissionTicket(authorizationRequest);
            List<Result> result = authorization.evaluators().from(createPermissions(ticket, authorizationRequest, authorization), evaluationContext).evaluate();
            List<Permission> entitlements = Permissions.permits(result, authorization, ticket.getResourceServerId());

            if (!entitlements.isEmpty()) {
                AuthorizationResponse response = new AuthorizationResponse(createRequestingPartyToken(entitlements, identity.getAccessToken()));
                return Cors.add(httpRequest, Response.status(Status.CREATED).entity(response)).allowedOrigins(identity.getAccessToken())
                        .allowedMethods("POST")
                        .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
            }
        } catch (Exception cause) {
            logger.error(cause);
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, "Error while evaluating permissions.", Status.INTERNAL_SERVER_ERROR);
        }

        HashMap<Object, Object> error = new HashMap<>();

        error.put(OAuth2Constants.ERROR, "not_authorized");

        return Cors.add(httpRequest, Response.status(Status.FORBIDDEN)
                .entity(error))
                .allowedOrigins(identity.getAccessToken())
                .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private List<ResourcePermission> createPermissions(PermissionTicket ticket, AuthorizationRequest request, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = authorization.getStoreFactory().getResourceServerStore().findById(ticket.getResourceServerId());

        if (resourceServer == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client does not support permissions", Status.FORBIDDEN);
        }

        Map<String, Set<String>> permissionsToEvaluate = new HashMap<>();

        ticket.getResources().forEach(requestedResource -> {
            Resource resource;

            if (requestedResource.getId() != null) {
                resource = storeFactory.getResourceStore().findById(requestedResource.getId(), ticket.getResourceServerId());
            } else {
                resource = storeFactory.getResourceStore().findByName(requestedResource.getName(), ticket.getResourceServerId());
            }

            if (resource == null && (requestedResource.getScopes() == null || requestedResource.getScopes().isEmpty())) {
                throw new ErrorResponseException("invalid_resource", "Resource with id [" + requestedResource.getId() + "] or name [" + requestedResource.getName() + "] does not exist.", Status.FORBIDDEN);
            }

            Set<ScopeRepresentation> requestedScopes = requestedResource.getScopes();
            Set<String> collect = requestedScopes.stream().map(ScopeRepresentation::getName).collect(Collectors.toSet());

            if (resource != null) {
                permissionsToEvaluate.put(resource.getId(), collect);
            } else {
                ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
                ScopeStore scopeStore = authorization.getStoreFactory().getScopeStore();
                List<Resource> resources = new ArrayList<Resource>();

                resources.addAll(resourceStore.findByScope(requestedScopes.stream().map(scopeRepresentation -> {
                    Scope scope = scopeStore.findByName(scopeRepresentation.getName(), ticket.getResourceServerId());

                    if (scope == null) {
                        return null;
                    }

                    return scope.getId();
                }).filter(s -> s != null).collect(Collectors.toList()), ticket.getResourceServerId()));

                for (Resource resource1 : resources) {
                    permissionsToEvaluate.put(resource1.getId(), collect);
                }

                permissionsToEvaluate.put("$KC_SCOPE_PERMISSION", collect);
            }
        });

        String rpt = request.getRpt();

        if (rpt != null && !"".equals(rpt)) {
            if (!Tokens.verifySignature(session, getRealm(), rpt)) {
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
                            Resource resourcePermission = storeFactory.getResourceStore().findById(permission.getResourceSetId(), ticket.getResourceServerId());

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
                .flatMap((Function<Entry<String, Set<String>>, Stream<ResourcePermission>>) entry -> {
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

    private RealmModel getRealm() {
        return this.authorization.getKeycloakSession().getContext().getRealm();
    }

    private String createRequestingPartyToken(List<Permission> permissions, AccessToken accessToken) {
        AccessToken.Authorization authorization = new AccessToken.Authorization();

        authorization.setPermissions(permissions);
        accessToken.setAuthorization(authorization);

        return new TokenManager().encodeToken(session, getRealm(), accessToken);
    }

    private PermissionTicket verifyPermissionTicket(AuthorizationRequest request) {
        String ticketString = request.getTicket();

        if (ticketString == null || !Tokens.verifySignature(session, getRealm(), ticketString)) {
            throw new ErrorResponseException("invalid_ticket", "Ticket verification failed", Status.FORBIDDEN);
        }

        try {
            PermissionTicket ticket = new JWSInput(ticketString).readJsonContent(PermissionTicket.class);

            if (!ticket.isActive()) {
                throw new ErrorResponseException("invalid_ticket", "Invalid permission ticket.", Status.FORBIDDEN);
            }

            return ticket;
        } catch (JWSInputException e) {
            throw new ErrorResponseException("invalid_ticket", "Could not parse permission ticket.", Status.FORBIDDEN);
        }
    }
}
