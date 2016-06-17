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

package org.keycloak.authorization.authorization;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.representation.ScopeRepresentation;
import org.keycloak.authorization.authorization.representation.AuthorizationRequest;
import org.keycloak.authorization.authorization.representation.AuthorizationResponse;
import org.keycloak.authorization.common.KeycloakEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.protection.permission.PermissionTicket;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.authorization.Permission;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationTokenService {

    private final AuthorizationProvider authorization;

    @Context
    private HttpRequest httpRequest;

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
    public void authorize(AuthorizationRequest authorizationRequest, @Suspended AsyncResponse asyncResponse) {
        KeycloakEvaluationContext evaluationContext = new KeycloakEvaluationContext(this.authorization.getKeycloakSession());
        KeycloakIdentity identity = (KeycloakIdentity) evaluationContext.getIdentity();

        if (!identity.hasRole("uma_authorization")) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_SCOPE, "Requires uma_authorization scope.", Status.FORBIDDEN);
        }

        if (authorizationRequest == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid authorization request.", Status.BAD_REQUEST);
        }

        PermissionTicket ticket = verifyPermissionTicket(authorizationRequest);

        authorization.evaluators().from(createPermissions(ticket, authorizationRequest, authorization), evaluationContext).evaluate(new DecisionResultCollector() {
            @Override
            public void onComplete(List<Result> results) {
                List<Permission> entitlements = Permissions.allPermits(results);

                if (entitlements.isEmpty()) {
                    asyncResponse.resume(new ErrorResponseException("not_authorized", "Authorization denied.", Status.FORBIDDEN));
                } else {
                    AuthorizationResponse response = new AuthorizationResponse(createRequestingPartyToken(entitlements, identity.getAccessToken()));
                    asyncResponse.resume(Cors.add(httpRequest, Response.status(Status.CREATED).entity(response)).allowedOrigins(identity.getAccessToken())
                            .allowedMethods("POST")
                            .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build());
                }
            }

            @Override
            public void onError(Throwable cause) {
                asyncResponse.resume(cause);
            }
        });
    }

    private List<ResourcePermission> createPermissions(PermissionTicket ticket, AuthorizationRequest request, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        Map<String, Set<String>> permissionsToEvaluate = new HashMap<>();

        ticket.getResources().forEach(requestedResource -> {
            Resource resource;

            if (requestedResource.getId() != null) {
                resource = storeFactory.getResourceStore().findById(requestedResource.getId());
            } else {
                resource = storeFactory.getResourceStore().findByName(requestedResource.getName(), ticket.getResourceServerId());
            }

            if (resource == null) {
                throw new ErrorResponseException("invalid_resource", "Resource with id [" + requestedResource.getId() + "] or name [" + requestedResource.getName() + "] does not exist.", Status.FORBIDDEN);
            }

            Set<ScopeRepresentation> requestedScopes = requestedResource.getScopes();

            permissionsToEvaluate.put(resource.getId(), requestedScopes.stream().map(ScopeRepresentation::getName).collect(Collectors.toSet()));
        });

        String rpt = request.getRpt();

        if (rpt != null && !"".equals(rpt)) {
            if (!Tokens.verifySignature(rpt, getRealm().getPublicKey())) {
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
                            Resource resourcePermission = storeFactory.getResourceStore().findById(permission.getResourceSetId());

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
                    Resource entryResource = storeFactory.getResourceStore().findById(entry.getKey());
                    if (entry.getValue().isEmpty()) {
                        return Arrays.asList(new ResourcePermission(entryResource, Collections.emptyList(), entryResource.getResourceServer())).stream();
                    } else {
                        return entry.getValue().stream()
                                .map(scopeName -> storeFactory.getScopeStore().findByName(scopeName, entryResource.getResourceServer().getId()))
                                .filter(scope -> scope != null)
                                .map(scope -> new ResourcePermission(entryResource, Arrays.asList(scope), entryResource.getResourceServer()));
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

        return new TokenManager().encodeToken(getRealm(), accessToken);
    }

    private PermissionTicket verifyPermissionTicket(AuthorizationRequest request) {
        if (!Tokens.verifySignature(request.getTicket(), getRealm().getPublicKey())) {
            throw new ErrorResponseException("invalid_ticket", "Ticket verification failed", Status.FORBIDDEN);
        }

        try {
            PermissionTicket ticket = new JWSInput(request.getTicket()).readJsonContent(PermissionTicket.class);

            if (!ticket.isActive()) {
                throw new ErrorResponseException("invalid_ticket", "Invalid permission ticket.", Status.FORBIDDEN);
            }

            return ticket;
        } catch (JWSInputException e) {
            throw new ErrorResponseException("invalid_ticket", "Could not parse permission ticket.", Status.FORBIDDEN);
        }
    }
}
