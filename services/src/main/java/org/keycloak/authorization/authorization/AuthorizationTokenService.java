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
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.TokenManager.AccessTokenResponseBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationRequest.Metadata;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
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
        SUPPORTED_CLAIM_TOKEN_FORMATS.put("urn:ietf:params:oauth:token-type:jwt", (authorizationRequest, authorization) -> {
            String claimToken = authorizationRequest.getClaimToken();

            if (claimToken == null) {
                claimToken = authorizationRequest.getAccessToken();
            }

            return new KeycloakEvaluationContext(new KeycloakIdentity(authorization.getKeycloakSession(), Tokens.getAccessToken(claimToken, authorization.getKeycloakSession())), authorization.getKeycloakSession());
        });
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

    private final TokenManager tokenManager;
    private final EventBuilder event;
    private final HttpRequest httpRequest;
    private final AuthorizationProvider authorization;
    private final Cors cors;

    public AuthorizationTokenService(AuthorizationProvider authorization, TokenManager tokenManager, EventBuilder event, HttpRequest httpRequest, Cors cors) {
        this.tokenManager = tokenManager;
        this.event = event;
        this.httpRequest = httpRequest;
        this.authorization = authorization;
        this.cors = cors;
    }

    public Response authorize(AuthorizationRequest request) {
        if (request == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid authorization request.", Status.BAD_REQUEST);
        }

        try {
            PermissionTicketToken ticket = getPermissionTicket(request);
            ResourceServer resourceServer = getResourceServer(ticket);
            KeycloakEvaluationContext evaluationContext = createEvaluationContext(request);
            KeycloakIdentity identity = KeycloakIdentity.class.cast(evaluationContext.getIdentity());
            List<Result> evaluation;

            if (ticket.getResources().isEmpty() && request.getRpt() == null) {
                evaluation = evaluateAllPermissions(resourceServer, evaluationContext, identity);
            } else if(!request.getPermissions().getResources().isEmpty()) {
                evaluation = evaluatePermissions(request, ticket, resourceServer, evaluationContext, identity);
            } else {
                evaluation = evaluateUserManagedPermissions(request, ticket, resourceServer, evaluationContext, identity);
            }

            List<Permission> permissions = Permissions.permits(evaluation, request.getMetadata(), authorization, resourceServer);

            if (permissions.isEmpty()) {
                if (request.isSubmitRequest()) {
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "request_submitted", Status.FORBIDDEN);
                } else {
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "not_authorized", Status.FORBIDDEN);
                }
            }

            ClientModel targetClient = this.authorization.getRealm().getClientById(resourceServer.getId());
            AuthorizationResponse response = new AuthorizationResponse(createRequestingPartyToken(identity, permissions, targetClient), request.getRpt() != null);

            return Cors.add(httpRequest, Response.status(Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(response))
                    .allowedOrigins(getKeycloakSession().getContext().getUri(), targetClient)
                    .allowedMethods(HttpMethod.POST)
                    .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
        } catch (ErrorResponseException | CorsErrorResponseException cause) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error while evaluating permissions", cause);
            }
            throw cause;
        } catch (Exception cause) {
            logger.error("Unexpected error while evaluating permissions", cause);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SERVER_ERROR, "Unexpected error while evaluating permissions", Status.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Result> evaluatePermissions(AuthorizationRequest authorizationRequest, PermissionTicketToken ticket, ResourceServer resourceServer, KeycloakEvaluationContext evaluationContext, KeycloakIdentity identity) {
        return authorization.evaluators()
                .from(createPermissions(ticket, authorizationRequest, resourceServer, identity, authorization), evaluationContext)
                .evaluate();
    }

    private List<Result> evaluateUserManagedPermissions(AuthorizationRequest request, PermissionTicketToken ticket, ResourceServer resourceServer, KeycloakEvaluationContext evaluationContext, KeycloakIdentity identity) {
        return authorization.evaluators()
                .from(createPermissions(ticket, request, resourceServer, identity, authorization), evaluationContext)
                .evaluate(new PermissionTicketAwareDecisionResultCollector(request, ticket, identity, resourceServer, authorization)).results();
    }

    private List<Result> evaluateAllPermissions(ResourceServer resourceServer, KeycloakEvaluationContext evaluationContext, KeycloakIdentity identity) {
        return authorization.evaluators()
                .from(Permissions.all(resourceServer, identity, authorization), evaluationContext)
                .evaluate();
    }

    private AccessTokenResponse createRequestingPartyToken(KeycloakIdentity identity, List<Permission> entitlements, ClientModel targetClient) {
        KeycloakSession keycloakSession = getKeycloakSession();
        AccessToken accessToken = identity.getAccessToken();
        UserSessionModel userSessionModel = keycloakSession.sessions().getUserSession(getRealm(), accessToken.getSessionState());
        ClientModel client = getRealm().getClientByClientId(accessToken.getIssuedFor());
        AuthenticatedClientSessionModel clientSession = userSessionModel.getAuthenticatedClientSessionByClient(client.getId());
        AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(getRealm(), clientSession.getClient(), event, keycloakSession, userSessionModel, clientSession)
                .generateAccessToken()
                .generateRefreshToken();

        AccessToken rpt = responseBuilder.getAccessToken();

        rpt.issuedFor(client.getClientId());

        Authorization authorization = new Authorization();

        authorization.setPermissions(entitlements);

        rpt.setAuthorization(authorization);

        RefreshToken refreshToken = responseBuilder.getRefreshToken();

        refreshToken.issuedFor(client.getClientId());
        refreshToken.setAuthorization(authorization);

        if (!rpt.hasAudience(targetClient.getClientId())) {
            rpt.audience(targetClient.getClientId());
        }

        return responseBuilder.build();
    }

    private PermissionTicketToken getPermissionTicket(AuthorizationRequest request) {
        // if there is a ticket is because it is a UMA flow and the ticket was sent by the client after obtaining it from the target resource server
        if (request.getTicket() != null) {
            return verifyPermissionTicket(request);
        }

        // if there is no ticket, we use the permissions the client is asking for.
        // This is a Keycloak extension to UMA flow where clients are capable of obtaining a RPT without a ticket
        PermissionTicketToken permissions = request.getPermissions();

        // an audience must be set by the client when doing this method of obtaining RPT, that is how we know the target resource server
        permissions.audience(request.getAudience());

        return permissions;
    }

    private ResourceServer getResourceServer(PermissionTicketToken ticket) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();
        String[] audience = ticket.getAudience();

        if (audience == null || audience.length == 0) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "You must provide the audience", Status.BAD_REQUEST);
        }

        ClientModel clientModel = getRealm().getClientByClientId(audience[0]);

        if (clientModel == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Unknown resource server id.", Status.BAD_REQUEST);
        }

        ResourceServer resourceServer = resourceServerStore.findById(clientModel.getId());

        if (resourceServer == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client does not support permissions", Status.BAD_REQUEST);
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

    private List<ResourcePermission> createPermissions(PermissionTicketToken ticket, AuthorizationRequest request, ResourceServer resourceServer, KeycloakIdentity identity, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        Map<String, Set<String>> permissionsToEvaluate = new LinkedHashMap<>();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Metadata metadata = request.getMetadata();
        Integer limit = metadata != null ? metadata.getLimit() : null;

        for (PermissionTicketToken.ResourcePermission requestedResource : ticket.getResources()) {
            if (limit != null && limit <= 0) {
                break;
            }

            Set<String> requestedScopes = requestedResource.getScopes();

            if (requestedResource.getScopes() == null) {
                requestedScopes = new HashSet<>();
            }

            List<Resource> existingResources = new ArrayList<>();

            if (requestedResource.getResourceId() != null) {
                Resource resource = resourceStore.findById(requestedResource.getResourceId(), resourceServer.getId());

                if (resource != null) {
                    existingResources.add(resource);
                } else {
                    Resource ownerResource = resourceStore.findByName(requestedResource.getResourceId(), identity.getId(), resourceServer.getId());

                    if (ownerResource != null) {
                        existingResources.add(ownerResource);
                    }

                    if (!identity.isResourceServer()) {
                        Resource serverResource = resourceStore.findByName(requestedResource.getResourceId(), resourceServer.getId());

                        if (serverResource != null) {
                            existingResources.add(serverResource);
                        }
                    }
                }
            }

            if (existingResources.isEmpty() && (requestedScopes == null || requestedScopes.isEmpty())) {
                throw new CorsErrorResponseException(cors, "invalid_resource", "Resource with id [" + requestedResource.getResourceId() + "] does not exist.", Status.FORBIDDEN);
            }

            String clientAdditionalScopes = request.getScope();

            if (clientAdditionalScopes != null) {
                requestedScopes.addAll(Arrays.asList(clientAdditionalScopes.split(" ")));
            }

            if (!existingResources.isEmpty()) {
                for (Resource resource : existingResources) {
                    Set<String> scopes = permissionsToEvaluate.get(resource.getId());

                    if (scopes == null) {
                        scopes = new HashSet<>();
                        permissionsToEvaluate.put(resource.getId(), scopes);
                        if (limit != null) {
                            limit--;
                        }
                    }

                    scopes.addAll(requestedScopes);
                }
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

                            Resource resourcePermission = resourceStore.findById(permission.getResourceId(), ticket.getAudience()[0]);

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

    private KeycloakSession getKeycloakSession() {
        return this.authorization.getKeycloakSession();
    }

    private RealmModel getRealm() {
        return getKeycloakSession().getContext().getRealm();
    }
}
