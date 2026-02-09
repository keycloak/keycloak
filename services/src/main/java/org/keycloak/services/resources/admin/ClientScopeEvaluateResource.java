/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.resources.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.TriFunction;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.protocol.ProtocolMapperUtils.isEnabled;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientScopeEvaluateResource {

    protected static final Logger logger = Logger.getLogger(ClientScopeEvaluateResource.class);

    private final RealmModel realm;
    private final ClientModel client;
    private final AdminPermissionEvaluator auth;

    private final UriInfo uriInfo;
    private final KeycloakSession session;
    private final ClientConnection clientConnection;

    public ClientScopeEvaluateResource(KeycloakSession session, UriInfo uriInfo, RealmModel realm, AdminPermissionEvaluator auth,
                                 ClientModel client, ClientConnection clientConnection) {
        this.uriInfo = uriInfo;
        this.realm = realm;
        this.client = client;
        this.auth = auth;
        this.session = session;
        this.clientConnection = clientConnection;
    }

    /**
     *
     * @param scopeParam
     * @param roleContainerId either realm name OR client UUID
     * @return
     */
    @Path("scope-mappings/{roleContainerId}")
    public ClientScopeEvaluateScopeMappingsResource scopeMappings(@QueryParam("scope") String scopeParam, @Parameter(description = "either realm name OR client UUID") @PathParam("roleContainerId") String roleContainerId) {
        auth.clients().requireView(client);

        if (roleContainerId == null) {
            throw new NotFoundException("No roleContainerId provided");
        }

        RoleContainerModel roleContainer = roleContainerId.equals(realm.getName()) ? realm : realm.getClientById(roleContainerId);
        if (roleContainer == null) {
            throw new NotFoundException("Role Container not found");
        }

        return new ClientScopeEvaluateScopeMappingsResource(session, roleContainer, auth, client, scopeParam);
    }

    /**
     * Return list of all protocol mappers, which will be used when generating tokens issued for particular client. This means
     * protocol mappers assigned to this client directly and protocol mappers assigned to all client scopes of this client.
     *
     * @return
     */
    @GET
    @Path("protocol-mappers")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation(summary = "Return list of all protocol mappers, which will be used when generating tokens issued for particular client.",
            description = "This means protocol mappers assigned to this client directly and protocol mappers assigned to all client scopes of this client.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = ProtocolMapperEvaluationRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<ProtocolMapperEvaluationRepresentation> getGrantedProtocolMappers(@QueryParam("scope") String scopeParam) {
        auth.clients().requireView(client);

        return TokenManager.getRequestedClientScopes(session, scopeParam, client, null)
                .flatMap(mapperContainer -> mapperContainer.getProtocolMappersStream()
                    .filter(current -> isEnabled(session, current) && Objects.equals(current.getProtocol(), client.getProtocol()))
                    .map(current -> toProtocolMapperEvaluationRepresentation(current, mapperContainer)));
    }

    private ProtocolMapperEvaluationRepresentation toProtocolMapperEvaluationRepresentation(ProtocolMapperModel mapper,
                                                                                            ClientScopeModel mapperContainer) {
        ProtocolMapperEvaluationRepresentation rep = new ProtocolMapperEvaluationRepresentation();
        rep.setMapperId(mapper.getId());
        rep.setMapperName(mapper.getName());
        rep.setProtocolMapper(mapper.getProtocolMapper());

        if (mapperContainer.getId().equals(client.getId())) {
            // Must be this client
            rep.setContainerId(client.getId());
            rep.setContainerName("");
            rep.setContainerType("client");
        } else {
            ClientScopeModel clientScope = mapperContainer;
            rep.setContainerId(clientScope.getId());
            rep.setContainerName(clientScope.getName());
            rep.setContainerType("client-scope");
        }
        return rep;
    }

    /**
     * Create JSON with payload of example user info
     *
     * @return
     */
    @GET
    @Path("generate-example-userinfo")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation(summary = "Create JSON with payload of example user info")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = Map.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Map<String, Object> generateExampleUserinfo(@QueryParam("scope") String scopeParam, @QueryParam("userId") String userId) {
        auth.clients().requireView(client);

        UserModel user = getUserModel(userId);

        logger.debugf("generateExampleUserinfo invoked. User: %s", user.getUsername());

        return sessionAware(user, scopeParam, "", (userSession, clientSessionCtx, audienceClients) -> {
            AccessToken userInfo = new AccessToken();
            TokenManager tokenManager = new TokenManager();

            userInfo = tokenManager.transformUserInfoAccessToken(session, userInfo, userSession, clientSessionCtx);
            return tokenManager.generateUserInfoClaims(userInfo, user);
        });
    }

    /**
     * Create JSON with payload of example id token
     *
     * @return
     */
    @GET
    @Path("generate-example-id-token")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation(summary = "Create JSON with payload of example id token")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = IDToken.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public IDToken generateExampleIdToken(@QueryParam("scope") String scopeParam, @QueryParam("userId") String userId, @QueryParam("audience") String audience) {
        auth.clients().requireView(client);

        UserModel user = getUserModel(userId);

        logger.debugf("generateExampleIdToken invoked. User: %s, Scope param: %s, Target Audience: %s", user.getUsername(), scopeParam);

        return sessionAware(user, scopeParam, audience, (userSession, clientSessionCtx, audienceClients) ->
        {
            TokenManager tokenManager = new TokenManager();
            TokenManager.AccessTokenResponseBuilder response = tokenManager.responseBuilder(realm, client, null, session, userSession, clientSessionCtx)
                    .generateAccessToken().generateIDToken();
            IDToken idToken = response.getIdToken();

            // Retrieve accessToken just to check the audience
            AccessToken accessToken = response.getAccessToken();
            validateAudience(accessToken, audienceClients);

            return idToken;
        });
    }

    /**
     * Create JSON with payload of example access token
     *
     * @return
     */
    @GET
    @Path("generate-example-access-token")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation(summary = "Create JSON with payload of example access token")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = AccessToken.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public AccessToken generateExampleAccessToken(@QueryParam("scope") String scopeParam, @QueryParam("userId") String userId, @QueryParam("audience") String audience) {
        auth.clients().requireView(client);

        UserModel user = getUserModel(userId);

        logger.debugf("generateExampleAccessToken invoked. User: %s, Scope param: %s, Target Audience: %s", user.getUsername(), scopeParam, audience);

        return sessionAware(user, scopeParam, audience, (userSession, clientSessionCtx, audienceClients) ->
        {
            TokenManager tokenManager = new TokenManager();
            AccessToken accessToken =  tokenManager.responseBuilder(realm, client, null, session, userSession, clientSessionCtx)
                    .generateAccessToken().getAccessToken();
            validateAudience(accessToken, audienceClients);
            return accessToken;
        });
    }

    private<R> R sessionAware(UserModel user, String scopeParam, String audienceParam, TriFunction<UserSessionModel, ClientSessionContext, ClientModel[], R> function) {
        AuthenticationSessionModel authSession = null;
        UserSessionModel userSession = null;
        AuthenticationSessionManager authSessionManager = new AuthenticationSessionManager(session);

        try {
            RootAuthenticationSessionModel rootAuthSession = authSessionManager.createAuthenticationSession(realm, false);
            authSession = rootAuthSession.createAuthenticationSession(client);

            authSession.setAuthenticatedUser(user);
            authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
            authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scopeParam);

            userSession = new UserSessionManager(session).createUserSession(authSession.getParentSession().getId(), realm, user, user.getUsername(),
                    clientConnection.getRemoteHost(), "example-auth", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

            AuthenticationManager.setClientScopesInSession(session, authSession);
            ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);

            ClientModel[] audienceClients = getClients(audienceParam);
            if (audienceClients.length > 0) {
                clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, audienceClients);
            }

            return function.apply(userSession, clientSessionCtx, audienceClients);

        } finally {
            if (authSession != null) {
                authSessionManager.removeAuthenticationSession(realm, authSession, false);
            }
            if (userSession != null) {
                session.sessions().removeUserSession(realm, userSession);
            }
        }
    }

    private ClientModel[] getClients(String clientsStr) {
        List<ClientModel> clients = new ArrayList<>();
        if(clientsStr != null && !clientsStr.isEmpty()) {
            for (String clientId : clientsStr.split("\\s+")) {
                ClientModel client = realm.getClientByClientId(clientId);
                if (client != null) {
                    clients.add(client);
                }
            }
        }
        return clients.toArray(ClientModel[]::new);
    }
    
    private void validateAudience(AccessToken accessToken, ClientModel[] requestedAudience) {
        List<String> requestedAudienceClientIds = Stream.of(requestedAudience)
                .map(ClientModel::getClientId)
                .collect(Collectors.toList());
        Set<String> missingAudience = TokenUtils.checkRequestedAudiences(accessToken, requestedAudienceClientIds);
        if (!missingAudience.isEmpty()) {
            String missingAudienceStr = CollectionUtil.join(missingAudience);
            throw new NotFoundException("Requested audience not available: " + missingAudienceStr);
        }
    }

    private UserModel getUserModel(String userId) {
        if (userId == null) {
            throw new NotFoundException("No userId provided");
        }

        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            throw new NotFoundException("No user found");
        }
        return user;
    }

    public static class ProtocolMapperEvaluationRepresentation {

        @JsonProperty("mapperId")
        private String mapperId;

        @JsonProperty("mapperName")
        private String mapperName;

        @JsonProperty("containerId")
        private String containerId;

        @JsonProperty("containerName")
        private String containerName;

        @JsonProperty("containerType")
        private String containerType;

        @JsonProperty("protocolMapper")
        private String protocolMapper;

        public String getMapperId() {
            return mapperId;
        }

        public void setMapperId(String mapperId) {
            this.mapperId = mapperId;
        }

        public String getMapperName() {
            return mapperName;
        }

        public void setMapperName(String mapperName) {
            this.mapperName = mapperName;
        }

        public String getContainerId() {
            return containerId;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        public String getContainerType() {
            return containerType;
        }

        public void setContainerType(String containerType) {
            this.containerType = containerType;
        }

        public String getProtocolMapper() {
            return protocolMapper;
        }

        public void setProtocolMapper(String protocolMapper) {
            this.protocolMapper = protocolMapper;
        }
    }
}
