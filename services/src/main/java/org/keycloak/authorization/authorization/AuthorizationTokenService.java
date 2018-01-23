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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.authorization.representation.AuthorizationRequest;
import org.keycloak.authorization.authorization.representation.AuthorizationRequestMetadata;
import org.keycloak.authorization.authorization.representation.AuthorizationResponse;
import org.keycloak.authorization.common.KeycloakEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.PermissionTicketAwareDecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.Cors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationTokenService {

    private static final Logger logger = Logger.getLogger(AuthorizationTokenService.class);
    private static Map<String, BiFunction<AuthorizationRequest, AuthorizationProvider, KeycloakEvaluationContext>> SUPPORTED_CLAIM_TOKEN_FORMATS;

    static {
        SUPPORTED_CLAIM_TOKEN_FORMATS = new HashMap<>();
        SUPPORTED_CLAIM_TOKEN_FORMATS.put("urn:ietf:params:oauth:token-type:jwt", (authorizationRequest, authorization) -> new KeycloakEvaluationContext(authorizationRequest.getClaimToken(), authorization.getKeycloakSession()));
        SUPPORTED_CLAIM_TOKEN_FORMATS.put("http://openid.net/specs/openid-connect-core-1_0.html#IDToken", (authorizationRequest, authorization) -> {
            try {
                KeycloakSession keycloakSession = authorization.getKeycloakSession();
                IDToken idToken = new TokenManager().verifyIDTokenSignature(keycloakSession, authorization.getRealm(), authorizationRequest.getClaimToken());
                return new KeycloakEvaluationContext(new KeycloakIdentity(keycloakSession, idToken), keycloakSession);
            } catch (OAuthErrorException cause) {
                throw new RuntimeException("Failed to verify ID token", cause);
            }
        });
    }

    private final HttpRequest httpRequest;
    private final AuthorizationProvider authorization;
    private final Cors cors;

    public AuthorizationTokenService(AuthorizationProvider authorization, HttpRequest httpRequest, Cors cors) {
        this.httpRequest = httpRequest;
        this.authorization = authorization;
        this.cors = cors;
    }

    public Response authorize(AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid authorization request.", Status.BAD_REQUEST);
        }

        try {
            PermissionTicketToken ticket = createPermissionTicket(authorizationRequest);
            ResourceServer resourceServer = getResourceServer(ticket);
            KeycloakEvaluationContext evaluationContext = createEvaluationContext(authorizationRequest);
            KeycloakIdentity identity = (KeycloakIdentity) evaluationContext.getIdentity();
            List<Result> results;

            if ((ticket.getResources() == null || ticket.getResources().isEmpty()) && authorizationRequest.getRpt() == null) {
                results = authorization.evaluators().from(Permissions.all(resourceServer, identity, authorization), evaluationContext).evaluate();
            } else {
                List<ResourcePermission> permissions = createPermissions(ticket, authorizationRequest, resourceServer, authorization);
                PermissionTicketAwareDecisionResultCollector decision = new PermissionTicketAwareDecisionResultCollector(ticket, evaluationContext.getIdentity(), resourceServer, authorization);

                authorization.evaluators().from(permissions, evaluationContext).evaluate(decision);

                results = decision.getResults();
            }

            List<Permission> entitlements = Permissions.permits(results, authorizationRequest.getMetadata(), authorization, resourceServer);

            if (entitlements.isEmpty()) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "not_authorized", Status.FORBIDDEN);
            }

            AuthorizationResponse response = new AuthorizationResponse();

            response.setToken(createRequestingPartyToken(entitlements, ticket, identity.getAccessToken(), resourceServer));
            response.setTokenType("bearer");

            if (authorizationRequest.getRpt() != null) {
                response.setUpgraded(true);
            }

            return Cors.add(httpRequest, Response.status(Status.CREATED).type(MediaType.APPLICATION_JSON_TYPE).entity(response))
                    .allowedOrigins(identity.getAccessToken())
                    .allowedMethods(HttpMethod.POST)
                    .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
        } catch (ErrorResponseException | CorsErrorResponseException cause) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error while evaluating permissions", cause);
            }
            throw cause;
        } catch (Exception cause) {
            logger.error("Error while evaluating permissions", cause);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SERVER_ERROR, "Error while evaluating permissions", Status.INTERNAL_SERVER_ERROR);
        }
    }

    private PermissionTicketToken createPermissionTicket(AuthorizationRequest authorizationRequest) {
        if (authorizationRequest.getTicket() != null) {
            return verifyPermissionTicket(authorizationRequest);
        }

        if (authorizationRequest.getPermissions() != null) {
            PermissionTicketToken permissions = authorizationRequest.getPermissions();

            permissions.audience(authorizationRequest.getAudience());

            return permissions;
        }

        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "You must provide either a ticket or permissions", Status.BAD_REQUEST);
    }

    private ResourceServer getResourceServer(PermissionTicketToken ticket) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();
        String[] audience = ticket.getAudience();

        if (audience == null || audience.length == 0) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "You must provide the audience", Status.BAD_REQUEST);
        }

        String resourceServerId = audience[0];
        ResourceServer resourceServer = resourceServerStore.findById(resourceServerId);

        if (resourceServer == null) {
            ClientModel clientModel = getRealm().getClientByClientId(resourceServerId);
            if (clientModel == null) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Unknown resource server id.", Status.BAD_REQUEST);
            }
            resourceServer = resourceServerStore.findById(clientModel.getId());
            if (resourceServer == null) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client does not support permissions", Status.BAD_REQUEST);
            }
        }
        return resourceServer;
    }

    private KeycloakEvaluationContext createEvaluationContext(AuthorizationRequest authorizationRequest) {
        String claimTokenFormat = authorizationRequest.getClaimTokenFormat();

        if (claimTokenFormat == null) {
            claimTokenFormat = "urn:ietf:params:oauth:token-type:jwt";
        }

        BiFunction<AuthorizationRequest, AuthorizationProvider, KeycloakEvaluationContext> evaluationContextProvider = SUPPORTED_CLAIM_TOKEN_FORMATS.get(claimTokenFormat);

        if (evaluationContextProvider == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Claim token format [" + claimTokenFormat + "] not supported", Status.BAD_REQUEST);
        }

        return evaluationContextProvider.apply(authorizationRequest, authorization);
    }

    private List<ResourcePermission> createPermissions(PermissionTicketToken ticket, AuthorizationRequest request, ResourceServer resourceServer, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        Map<String, Set<String>> permissionsToEvaluate = new LinkedHashMap<>();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        AuthorizationRequestMetadata metadata = request.getMetadata();
        Integer limit = metadata != null && metadata.getLimit() > 0 ? metadata.getLimit() : null;

        for (PermissionTicketToken.ResourcePermission requestedResource : ticket.getResources()) {
            if (limit != null && limit <= 0) {
                break;
            }

            Set<String> requestedScopes = requestedResource.getScopes();

            if (requestedResource.getScopes() == null) {
                requestedScopes = new HashSet<>();
            }

            Resource existingResource = null;

            if (requestedResource.getResourceId() != null) {
                existingResource = resourceStore.findById(requestedResource.getResourceId(), resourceServer.getId());

                if (existingResource == null) {
                    existingResource = resourceStore.findByName(requestedResource.getResourceId(), resourceServer.getId());
                }
            }

            if (existingResource == null && (requestedScopes == null || requestedScopes.isEmpty())) {
                throw new CorsErrorResponseException(cors, "invalid_resource", "Resource with id [" + requestedResource.getResourceId() + "] does not exist.", Status.FORBIDDEN);
            }

            String clientAdditionalScopes = request.getScope();

            if (clientAdditionalScopes != null) {
                requestedScopes.addAll(Arrays.asList(clientAdditionalScopes.split(" ")));
            }

            if (existingResource != null) {
                Set<String> scopes = permissionsToEvaluate.get(existingResource.getId());

                if (scopes == null) {
                    scopes = new HashSet<>();
                    permissionsToEvaluate.put(existingResource.getId(), scopes);
                    if (limit != null) {
                        limit--;
                    }
                }

                scopes.addAll(requestedScopes);
            } else {
                List<Resource> resources = resourceStore.findByScope(new ArrayList<>(requestedScopes), ticket.getAudience()[0]);

                for (Resource resource : resources) {
                    permissionsToEvaluate.put(resource.getId(), requestedScopes);
                    if (limit != null) {
                        limit--;
                    }
                }

                permissionsToEvaluate.put("$KC_SCOPE_PERMISSION", requestedScopes);
            }
        }

        String rpt = request.getRpt();

        if (rpt != null) {
            if (!Tokens.verifySignature(getKeycloakSession(), getRealm(), rpt)) {
                throw new CorsErrorResponseException(cors, "invalid_rpt", "RPT signature is invalid", Status.FORBIDDEN);
            }

            AccessToken requestingPartyToken;

            try {
                requestingPartyToken = new JWSInput(rpt).readJsonContent(AccessToken.class);
            } catch (JWSInputException e) {
                throw new CorsErrorResponseException(cors, "invalid_rpt", "Invalid RPT", Status.FORBIDDEN);
            }

            if (requestingPartyToken.isActive()) {
                AccessToken.Authorization authorizationData = requestingPartyToken.getAuthorization();

                if (authorizationData != null) {
                    List<Permission> permissions = authorizationData.getPermissions();

                    if (permissions != null) {
                        for (Permission permission : permissions) {
                            if (limit != null && limit <= 0) {
                                break;
                            }

                            Resource resourcePermission = resourceStore.findById(permission.getResourceSetId(), ticket.getAudience()[0]);

                            if (resourcePermission != null) {
                                Set<String> scopes = permissionsToEvaluate.get(resourcePermission.getId());

                                if (scopes == null) {
                                    scopes = new HashSet<>();
                                    permissionsToEvaluate.put(resourcePermission.getId(), scopes);
                                    if (limit != null) {
                                        limit--;
                                    }
                                }

                                Set<String> scopePermission = permission.getScopes();

                                if (scopePermission != null) {
                                    scopes.addAll(scopePermission);
                                }
                            }
                        }
                    }
                }
            }
        }

        ScopeStore scopeStore = storeFactory.getScopeStore();

        return permissionsToEvaluate.entrySet().stream()
                .flatMap((Function<Entry<String, Set<String>>, Stream<ResourcePermission>>) entry -> {
                    String key = entry.getKey();
                    if ("$KC_SCOPE_PERMISSION".equals(key)) {
                        List<Scope> scopes = entry.getValue().stream().map(scopeName -> scopeStore.findByName(scopeName, resourceServer.getId())).filter(scope -> Objects.nonNull(scope)).collect(Collectors.toList());
                        return Arrays.asList(new ResourcePermission(null, scopes, resourceServer)).stream();
                    } else {
                        Resource entryResource = resourceStore.findById(key, resourceServer.getId());
                        return Permissions.createResourcePermissions(entryResource, entry.getValue(), authorization).stream();
                    }
                }).collect(Collectors.toList());
    }

    private KeycloakSession getKeycloakSession() {
        return this.authorization.getKeycloakSession();
    }

    private RealmModel getRealm() {
        return getKeycloakSession().getContext().getRealm();
    }

    private String createRequestingPartyToken(List<Permission> permissions, PermissionTicketToken ticket, AccessToken accessToken, ResourceServer resourceServer) {
        AccessToken.Authorization authorization = new AccessToken.Authorization();

        authorization.setPermissions(permissions);
        accessToken.setAuthorization(authorization);

        ClientModel clientModel = this.authorization.getRealm().getClientById(resourceServer.getId());

        if (!accessToken.hasAudience(clientModel.getClientId())) {
            accessToken.audience(clientModel.getClientId());
        }

        return new TokenManager().encodeToken(getKeycloakSession(), getRealm(), accessToken);
    }

    private PermissionTicketToken verifyPermissionTicket(AuthorizationRequest request) {
        String ticketString = request.getTicket();

        if (ticketString == null || !Tokens.verifySignature(getKeycloakSession(), getRealm(), ticketString)) {
            throw new CorsErrorResponseException(cors, "invalid_ticket", "Ticket verification failed", Status.FORBIDDEN);
        }

        try {
            PermissionTicketToken ticket = new JWSInput(ticketString).readJsonContent(PermissionTicketToken.class);

            if (!ticket.isActive()) {
                throw new CorsErrorResponseException(cors, "invalid_ticket", "Invalid permission ticket.", Status.FORBIDDEN);
            }

            return ticket;
        } catch (JWSInputException e) {
            throw new CorsErrorResponseException(cors, "invalid_ticket", "Could not parse permission ticket.", Status.FORBIDDEN);
        }
    }
}
