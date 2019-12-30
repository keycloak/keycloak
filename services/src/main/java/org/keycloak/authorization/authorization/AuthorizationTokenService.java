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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.evaluation.PermissionTicketAwareDecisionResultCollector;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.common.util.Base64Url;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.TokenManager.AccessTokenResponseBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationRequest.Metadata;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.services.util.DefaultClientSessionContext;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationTokenService {

    public static final String CLAIM_TOKEN_FORMAT_ID_TOKEN = "http://openid.net/specs/openid-connect-core-1_0.html#IDToken";

    private static final Logger logger = Logger.getLogger(AuthorizationTokenService.class);
    private static final String RESPONSE_MODE_DECISION = "decision";
    private static final String RESPONSE_MODE_PERMISSIONS = "permissions";
    private static final String RESPONSE_MODE_DECISION_RESULT = "result";
    private static Map<String, BiFunction<KeycloakAuthorizationRequest, AuthorizationProvider, EvaluationContext>> SUPPORTED_CLAIM_TOKEN_FORMATS;

    static {
        SUPPORTED_CLAIM_TOKEN_FORMATS = new HashMap<>();
        SUPPORTED_CLAIM_TOKEN_FORMATS.put("urn:ietf:params:oauth:token-type:jwt", (request, authorization) -> {
            String claimToken = request.getClaimToken();

            if (claimToken != null) {
                Map claims;
                
                try {
                    claims = JsonSerialization.readValue(Base64Url.decode(request.getClaimToken()), Map.class);
                    request.setClaims(claims);
                } catch (Exception cause) {
                    throw new CorsErrorResponseException(request.getCors(), "invalid_request", "Invalid claims",
                            Status.BAD_REQUEST);
                }

                KeycloakIdentity identity;
                
                try {
                    identity = new KeycloakIdentity(authorization.getKeycloakSession(),
                            Tokens.getAccessToken(request.getSubjectToken(), authorization.getKeycloakSession()));
                } catch (Exception cause) {
                    throw new CorsErrorResponseException(request.getCors(), "unauthorized_client", "Invalid identity", Status.BAD_REQUEST);
                }

                return new DefaultEvaluationContext(identity, claims, authorization.getKeycloakSession());
            }

            throw new CorsErrorResponseException(request.getCors(), "invalid_request", "Claim token can not be null", Status.BAD_REQUEST);
        });
        SUPPORTED_CLAIM_TOKEN_FORMATS.put(CLAIM_TOKEN_FORMAT_ID_TOKEN, (request, authorization) -> {
            KeycloakSession keycloakSession = authorization.getKeycloakSession();
            String subjectToken = request.getSubjectToken();

            if (subjectToken == null) {
                throw new CorsErrorResponseException(request.getCors(), "invalid_request", "Subject token can not be null and must be a valid ID or Access Token",
                        Status.BAD_REQUEST);
            }

            IDToken idToken;

            try {
                idToken = new TokenManager().verifyIDTokenSignature(keycloakSession, subjectToken);
            } catch (Exception cause) {
                throw new CorsErrorResponseException(request.getCors(), "unauthorized_client", "Invalid signature", Status.BAD_REQUEST);
            }

            KeycloakIdentity identity;
            
            try {
                identity = new KeycloakIdentity(keycloakSession, idToken);
            } catch (Exception cause) {
                throw new CorsErrorResponseException(request.getCors(), "unauthorized_client", "Invalid identity", Status.BAD_REQUEST);
            }

            return new DefaultEvaluationContext(identity, request.getClaims(), keycloakSession);
        });
    }

    private static final AuthorizationTokenService INSTANCE = new AuthorizationTokenService();

    public static AuthorizationTokenService instance() {
        return INSTANCE;
    }

    public Response authorize(KeycloakAuthorizationRequest request) {
        if (request == null) {
            throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.INVALID_GRANT, "Invalid authorization request.", Status.BAD_REQUEST);
        }

        // it is not secure to allow public clients to push arbitrary claims because message can be tampered
        if (isPublicClientRequestingEntitlementWithClaims(request)) {
            throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.INVALID_GRANT, "Public clients are not allowed to send claims", Status.FORBIDDEN);
        }

        try {
            PermissionTicketToken ticket = getPermissionTicket(request);

            request.setClaims(ticket.getClaims());

            ResourceServer resourceServer = getResourceServer(ticket, request);
            EvaluationContext evaluationContext = createEvaluationContext(request);
            KeycloakIdentity identity = KeycloakIdentity.class.cast(evaluationContext.getIdentity());
            Collection<Permission> permissions;

            if (request.getTicket() != null) {
                permissions = evaluateUserManagedPermissions(request, ticket, resourceServer, evaluationContext, identity);
            } else if (ticket.getPermissions().isEmpty() && request.getRpt() == null) {
                permissions = evaluateAllPermissions(request, resourceServer, evaluationContext, identity);
            } else {
                permissions = evaluatePermissions(request, ticket, resourceServer, evaluationContext, identity);
            }

            if (isGranted(ticket, request, permissions)) {
                AuthorizationProvider authorization = request.getAuthorization();
                ClientModel targetClient = authorization.getRealm().getClientById(resourceServer.getId());
                Metadata metadata = request.getMetadata();
                String responseMode = metadata != null ? metadata.getResponseMode() : null;

                if (responseMode != null) {
                    if (RESPONSE_MODE_DECISION.equals(metadata.getResponseMode())) {
                        Map<String, Object> responseClaims = new HashMap<>();

                        responseClaims.put(RESPONSE_MODE_DECISION_RESULT, true);

                        return createSuccessfulResponse(responseClaims, request);
                    } else if (RESPONSE_MODE_PERMISSIONS.equals(metadata.getResponseMode())) {
                        return createSuccessfulResponse(permissions, request);
                    } else {
                        throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.INVALID_REQUEST, "Invalid response_mode", Status.BAD_REQUEST);
                    }
                } else {
                    return createSuccessfulResponse(createAuthorizationResponse(identity, permissions, request, targetClient), request);
                }
            }

            if (request.isSubmitRequest()) {
                throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.ACCESS_DENIED, "request_submitted", Status.FORBIDDEN);
            } else {
                throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.ACCESS_DENIED, "not_authorized", Status.FORBIDDEN);
            }
        } catch (ErrorResponseException | CorsErrorResponseException cause) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error while evaluating permissions", cause);
            }
            throw cause;
        } catch (Exception cause) {
            logger.error("Unexpected error while evaluating permissions", cause);
            throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.SERVER_ERROR, "Unexpected error while evaluating permissions", Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Response createSuccessfulResponse(Object response, KeycloakAuthorizationRequest request) {
        return Cors.add(request.getHttpRequest(), Response.status(Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(response))
                .allowedOrigins(request.getKeycloakSession(), request.getKeycloakSession().getContext().getClient())
                .allowedMethods(HttpMethod.POST)
                .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private boolean isPublicClientRequestingEntitlementWithClaims(KeycloakAuthorizationRequest request) {
        return request.getClaimToken() != null && request.getKeycloakSession().getContext().getClient().isPublicClient() && request.getTicket() == null;
    }

    private Collection<Permission> evaluatePermissions(KeycloakAuthorizationRequest request, PermissionTicketToken ticket, ResourceServer resourceServer, EvaluationContext evaluationContext, KeycloakIdentity identity) {
        AuthorizationProvider authorization = request.getAuthorization();
        return authorization.evaluators()
                .from(createPermissions(ticket, request, resourceServer, identity, authorization), evaluationContext)
                .evaluate(resourceServer, request);
    }

    private Collection<Permission> evaluateUserManagedPermissions(KeycloakAuthorizationRequest request, PermissionTicketToken ticket, ResourceServer resourceServer, EvaluationContext evaluationContext, KeycloakIdentity identity) {
        AuthorizationProvider authorization = request.getAuthorization();
        return authorization.evaluators()
                .from(createPermissions(ticket, request, resourceServer, identity, authorization), evaluationContext)
                .evaluate(new PermissionTicketAwareDecisionResultCollector(request, ticket, identity, resourceServer, authorization)).results();
    }

    private Collection<Permission> evaluateAllPermissions(KeycloakAuthorizationRequest request, ResourceServer resourceServer, EvaluationContext evaluationContext, KeycloakIdentity identity) {
        AuthorizationProvider authorization = request.getAuthorization();
        return authorization.evaluators()
                .from(Permissions.all(resourceServer, identity, authorization, request), evaluationContext)
                .evaluate(resourceServer, request);
    }

    private AuthorizationResponse createAuthorizationResponse(KeycloakIdentity identity, Collection<Permission> entitlements, KeycloakAuthorizationRequest request, ClientModel targetClient) {
        KeycloakSession keycloakSession = request.getKeycloakSession();
        AccessToken accessToken = identity.getAccessToken();
        RealmModel realm = request.getRealm();
        UserSessionProvider sessions = keycloakSession.sessions();
        UserSessionModel userSessionModel = sessions.getUserSession(realm, accessToken.getSessionState());

        if (userSessionModel == null) {
            userSessionModel = sessions.getOfflineUserSession(realm, accessToken.getSessionState());
        }

        ClientModel client = realm.getClientByClientId(accessToken.getIssuedFor());
        AuthenticatedClientSessionModel clientSession = userSessionModel.getAuthenticatedClientSessionByClient(targetClient.getId());
        ClientSessionContext clientSessionCtx;

        if (clientSession == null) {
            RootAuthenticationSessionModel rootAuthSession = keycloakSession.authenticationSessions().getRootAuthenticationSession(realm, userSessionModel.getId());

            if (rootAuthSession == null) {
                if (userSessionModel.getUser().getServiceAccountClientLink() == null) {
                    rootAuthSession = keycloakSession.authenticationSessions().createRootAuthenticationSession(userSessionModel.getId(), realm);
                } else {
                    // if the user session is associated with a service account
                    rootAuthSession = new AuthenticationSessionManager(keycloakSession).createAuthenticationSession(realm, false);
                }
            }

            AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(targetClient);

            authSession.setAuthenticatedUser(userSessionModel.getUser());
            authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(keycloakSession.getContext().getUri().getBaseUri(), realm.getName()));

            AuthenticationManager.setClientScopesInSession(authSession);
            clientSessionCtx = TokenManager.attachAuthenticationSession(keycloakSession, userSessionModel, authSession);
        } else {
            clientSessionCtx = DefaultClientSessionContext.fromClientSessionScopeParameter(clientSession, keycloakSession);
        }

        TokenManager tokenManager = request.getTokenManager();
        EventBuilder event = request.getEvent();
        AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, keycloakSession, userSessionModel, clientSessionCtx)
                .generateAccessToken()
                .generateRefreshToken();
        AccessToken rpt = responseBuilder.getAccessToken();
        Authorization authorization = new Authorization();

        authorization.setPermissions(entitlements);

        rpt.setAuthorization(authorization);

        RefreshToken refreshToken = responseBuilder.getRefreshToken();

        refreshToken.issuedFor(client.getClientId());
        refreshToken.setAuthorization(authorization);

        if (!rpt.hasAudience(targetClient.getClientId())) {
            rpt.audience(targetClient.getClientId());
        }

        return new AuthorizationResponse(responseBuilder.build(), isUpgraded(request, authorization));
    }

    private boolean isUpgraded(AuthorizationRequest request, Authorization authorization) {
        AccessToken previousRpt = request.getRpt();

        if (previousRpt == null) {
            return false;
        }

        Authorization previousAuthorization = previousRpt.getAuthorization();

        if (previousAuthorization != null) {
            Collection<Permission> previousPermissions = previousAuthorization.getPermissions();

            if (previousPermissions != null) {
                for (Permission previousPermission : previousPermissions) {
                    if (!authorization.getPermissions().contains(previousPermission)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private PermissionTicketToken getPermissionTicket(KeycloakAuthorizationRequest request) {
        // if there is a ticket is because it is a UMA flow and the ticket was sent by the client after obtaining it from the target resource server
        if (request.getTicket() != null) {
            return verifyPermissionTicket(request);
        }

        // if there is no ticket, we use the permissions the client is asking for.
        // This is a Keycloak extension to UMA flow where clients are capable of obtaining a RPT without a ticket
        PermissionTicketToken permissions = request.getPermissions();

        // an issuedFor must be set by the client when doing this method of obtaining RPT, that is how we know the target resource server
        permissions.issuedFor(request.getAudience());

        return permissions;
    }

    private ResourceServer getResourceServer(PermissionTicketToken ticket, KeycloakAuthorizationRequest request) {
        AuthorizationProvider authorization = request.getAuthorization();
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();
        String issuedFor = ticket.getIssuedFor();

        if (issuedFor == null) {
            throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.INVALID_REQUEST, "You must provide the issuedFor", Status.BAD_REQUEST);
        }

        ClientModel clientModel = request.getRealm().getClientByClientId(issuedFor);

        if (clientModel == null) {
            throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.INVALID_REQUEST, "Unknown resource server id.", Status.BAD_REQUEST);
        }

        ResourceServer resourceServer = resourceServerStore.findById(clientModel.getId());

        if (resourceServer == null) {
            throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.INVALID_REQUEST, "Client does not support permissions", Status.BAD_REQUEST);
        }

        return resourceServer;
    }

    private EvaluationContext createEvaluationContext(KeycloakAuthorizationRequest request) {
        String claimTokenFormat = request.getClaimTokenFormat();

        if (claimTokenFormat == null) {
            claimTokenFormat = CLAIM_TOKEN_FORMAT_ID_TOKEN;
        }

        BiFunction<KeycloakAuthorizationRequest, AuthorizationProvider, EvaluationContext> evaluationContextProvider = SUPPORTED_CLAIM_TOKEN_FORMATS.get(claimTokenFormat);

        if (evaluationContextProvider == null) {
            throw new CorsErrorResponseException(request.getCors(), OAuthErrorException.INVALID_REQUEST, "Claim token format [" + claimTokenFormat + "] not supported", Status.BAD_REQUEST);
        }

        return evaluationContextProvider.apply(request, request.getAuthorization());
    }

    private Collection<ResourcePermission> createPermissions(PermissionTicketToken ticket, KeycloakAuthorizationRequest request, ResourceServer resourceServer, KeycloakIdentity identity, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        Map<String, ResourcePermission> permissionsToEvaluate = new LinkedHashMap<>();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        ScopeStore scopeStore = storeFactory.getScopeStore();
        Metadata metadata = request.getMetadata();
        final AtomicInteger limit = metadata != null && metadata.getLimit() != null ? new AtomicInteger(metadata.getLimit()) : null;

        for (Permission permission : ticket.getPermissions()) {
            if (limit != null && limit.get() <= 0) {
                break;
            }

            Set<String> requestedScopes = permission.getScopes();

            if (permission.getScopes() == null) {
                requestedScopes = new HashSet<>();
            }

            List<Resource> requestedResources = new ArrayList<>();
            String resourceId = permission.getResourceId();

            if (resourceId != null) {
                Resource resource = null;

                if (resourceId.indexOf('-') != -1) {
                    resource = resourceStore.findById(resourceId, resourceServer.getId());
                }

                if (resource != null) {
                    requestedResources.add(resource);
                } else if (resourceId.startsWith("resource-type:")) {
                    // only resource types, no resource instances. resource types are owned by the resource server
                    String resourceType = resourceId.substring("resource-type:".length());
                    resourceStore.findByType(resourceType, resourceServer.getId(), resourceServer.getId(), requestedResources::add);
                } else if (resourceId.startsWith("resource-type-any:")) {
                    // any resource with a given type
                    String resourceType = resourceId.substring("resource-type-any:".length());
                    resourceStore.findByType(resourceType, null, resourceServer.getId(), requestedResources::add);
                } else if (resourceId.startsWith("resource-type-instance:")) {
                    // only resource instances with a given type
                    String resourceType = resourceId.substring("resource-type-instance:".length());
                    resourceStore.findByTypeInstance(resourceType, resourceServer.getId(), requestedResources::add);
                } else if (resourceId.startsWith("resource-type-owner:")) {
                    // only resources where the current identity is the owner
                    String resourceType = resourceId.substring("resource-type-owner:".length());
                    resourceStore.findByType(resourceType, identity.getId(), resourceServer.getId(), requestedResources::add);
                } else {
                    String resourceName = resourceId;
                    Resource ownerResource = resourceStore.findByName(resourceName, identity.getId(), resourceServer.getId());

                    if (ownerResource != null) {
                        permission.setResourceId(ownerResource.getId());
                        requestedResources.add(ownerResource);
                    }

                    if (!identity.isResourceServer() || !identity.getId().equals(resourceServer.getId())) {
                        List<PermissionTicket> tickets = storeFactory.getPermissionTicketStore().findGranted(resourceName, identity.getId(), resourceServer.getId());
                        for (PermissionTicket permissionTicket : tickets) {
                            requestedResources.add(permissionTicket.getResource());
                        }

                        Resource serverResource = resourceStore.findByName(resourceName, resourceServer.getId());

                        if (serverResource != null) {
                            permission.setResourceId(serverResource.getId());
                            requestedResources.add(serverResource);
                        }
                    }
                }
            }

            String clientAdditionalScopes = request.getScope();

            if (clientAdditionalScopes != null) {
                requestedScopes.addAll(Arrays.asList(clientAdditionalScopes.split(" ")));
            }

            Set<Scope> requestedScopesModel = requestedScopes.stream().map(s -> scopeStore.findByName(s, resourceServer.getId())).filter(Objects::nonNull).collect(Collectors.toSet());

            if (resourceId != null && requestedResources.isEmpty()) {
                throw new CorsErrorResponseException(request.getCors(), "invalid_resource", "Resource with id [" + resourceId + "] does not exist.", Status.BAD_REQUEST);
            }

            if (!requestedScopes.isEmpty() && requestedScopesModel.isEmpty()) {
                throw new CorsErrorResponseException(request.getCors(), "invalid_scope", "One of the given scopes " + permission.getScopes() + " is invalid", Status.BAD_REQUEST);
            }

            if (!requestedResources.isEmpty()) {
                for (Resource resource : requestedResources) {
                    if (limit != null && limit.get() <= 0) {
                        break;
                    }
                    ResourcePermission perm = permissionsToEvaluate.get(resource.getId());

                    if (perm == null) {
                        perm = Permissions.createResourcePermissions(resource, requestedScopesModel, authorization, request);
                        permissionsToEvaluate.put(resource.getId(), perm);
                        if (limit != null) {
                            limit.decrementAndGet();
                        }
                    } else {
                        for (Scope scope : requestedScopesModel) {
                            perm.addScope(scope);
                        }
                    }
                }
            } else {
                AtomicBoolean processed = new AtomicBoolean();

                resourceStore.findByScope(requestedScopesModel.stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId(), resource -> {
                    if (limit != null && limit.get() <= 0) {
                        return;
                    }

                    ResourcePermission perm = permissionsToEvaluate.get(resource.getId());

                    if (perm == null) {
                        perm = Permissions.createResourcePermissions(resource, requestedScopesModel, authorization, request);
                        permissionsToEvaluate.put(resource.getId(), perm);
                        if (limit != null) {
                            limit.decrementAndGet();
                        }
                    } else {
                        for (Scope scope : requestedScopesModel) {
                            perm.addScope(scope);
                        }
                    }

                    processed.compareAndSet(false, true);
                });

                if (!processed.get()) {
                    for (Scope scope : requestedScopesModel) {
                        if (limit != null && limit.getAndDecrement() <= 0) {
                            break;
                        }
                        permissionsToEvaluate.computeIfAbsent(scope.getId(), s -> new ResourcePermission(null, new ArrayList<>(Arrays.asList(scope)), resourceServer, request.getClaims()));
                    }
                }
            }
        }

        AccessToken rpt = request.getRpt();

        if (rpt != null && rpt.isActive()) {
            AccessToken.Authorization authorizationData = rpt.getAuthorization();

            if (authorizationData != null) {
                Collection<Permission> permissions = authorizationData.getPermissions();

                if (permissions != null) {
                    for (Permission grantedPermission : permissions) {
                        if (limit != null && limit.get() <= 0) {
                            break;
                        }

                        Resource resource = resourceStore.findById(grantedPermission.getResourceId(), ticket.getIssuedFor());

                        if (resource != null) {
                            ResourcePermission permission = permissionsToEvaluate.get(resource.getId());

                            if (permission == null) {
                                permission = new ResourcePermission(resource, new ArrayList<>(), resourceServer, grantedPermission.getClaims());
                                permissionsToEvaluate.put(resource.getId(), permission);
                                if (limit != null) {
                                    limit.decrementAndGet();
                                }
                            } else {
                                if (grantedPermission.getClaims() != null) {
                                    for (Entry<String, Set<String>> entry : grantedPermission.getClaims().entrySet()) {
                                        Set<String> claims = permission.getClaims().get(entry.getKey());

                                        if (claims != null) {
                                            claims.addAll(entry.getValue());
                                        }
                                    }
                                }
                            }

                            for (String scopeName : grantedPermission.getScopes()) {
                                Scope scope = scopeStore.findByName(scopeName, resourceServer.getId());

                                if (scope != null) {
                                    if (!permission.getScopes().contains(scope)) {
                                        permission.getScopes().add(scope);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return permissionsToEvaluate.values();
    }

    private PermissionTicketToken verifyPermissionTicket(KeycloakAuthorizationRequest request) {
        String ticketString = request.getTicket();

        PermissionTicketToken ticket = request.getKeycloakSession().tokens().decode(ticketString, PermissionTicketToken.class);
        if (ticket == null) {
            throw new CorsErrorResponseException(request.getCors(), "invalid_ticket", "Ticket verification failed", Status.FORBIDDEN);
        }

        if (!ticket.isActive()) {
            throw new CorsErrorResponseException(request.getCors(), "invalid_ticket", "Invalid permission ticket.", Status.FORBIDDEN);
        }

        return ticket;
    }

    private boolean isGranted(PermissionTicketToken ticket, AuthorizationRequest request, Collection<Permission> permissions) {
        List<Permission> requestedPermissions = ticket.getPermissions();

        // denies in case a rpt was provided along with the authorization request but any requested permission was not granted
        if (request.getRpt() != null && !requestedPermissions.isEmpty() && requestedPermissions.stream().anyMatch(permission -> !permissions.contains(permission))) {
            return false;
        }

        return !permissions.isEmpty();
    }

    public static class KeycloakAuthorizationRequest extends AuthorizationRequest {

        private final AuthorizationProvider authorization;
        private final TokenManager tokenManager;
        private final EventBuilder event;
        private final HttpRequest httpRequest;
        private final Cors cors;

        public KeycloakAuthorizationRequest(AuthorizationProvider authorization, TokenManager tokenManager, EventBuilder event, HttpRequest request, Cors cors) {
            this.authorization = authorization;
            this.tokenManager = tokenManager;
            this.event = event;
            httpRequest = request;
            this.cors = cors;
        }

        TokenManager getTokenManager() {
            return tokenManager;
        }

        EventBuilder getEvent() {
            return event;
        }

        HttpRequest getHttpRequest() {
            return httpRequest;
        }

        AuthorizationProvider getAuthorization() {
            return authorization;
        }

        Cors getCors() {
            return cors;
        }

        KeycloakSession getKeycloakSession() {
            return getAuthorization().getKeycloakSession();
        }

        RealmModel getRealm() {
            return getKeycloakSession().getContext().getRealm();
        }
    }
}
