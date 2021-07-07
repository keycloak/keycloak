/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants.device;

import static org.keycloak.protocol.oidc.OIDCLoginProtocolService.tokenServiceBaseUrl;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.oidc.grants.device.endpoints.DeviceEndpoint;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 * @author <a href="mailto:michito.okai.zn@hitachi.com">Michito Okai</a>
 */
public class DeviceGrantType {

    // OAuth 2.0 Device Authorization Grant
    public static final String OAUTH2_DEVICE_VERIFIED_USER_CODE = "OAUTH2_DEVICE_VERIFIED_USER_CODE";
    public static final String OAUTH2_DEVICE_USER_CODE = "device_user_code";
    public static final String OAUTH2_USER_CODE_VERIFY = "device/verify";

    public static UriBuilder oauth2DeviceVerificationUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return baseUriBuilder.path(RealmsResource.class).path("{realm}").path("device");
    }

    public static URI realmOAuth2DeviceVerificationAction(URI baseUri, String realmName) {
        return UriBuilder.fromUri(baseUri).path(RealmsResource.class).path("{realm}").path("device")
            .build(realmName);
    }

    public static UriBuilder oauth2DeviceAuthUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "auth").path(AuthorizationEndpoint.class, "authorizeDevice")
            .path(DeviceEndpoint.class, "handleDeviceRequest");
    }

    public static UriBuilder oauth2DeviceVerificationCompletedUrl(UriInfo baseUri) {
        return baseUri.getBaseUriBuilder().path(RealmsResource.class).path("{realm}").path("device").path("status");
    }

    public static Response denyOAuth2DeviceAuthorization(AuthenticationSessionModel authSession, LoginProtocol.Error error, KeycloakSession session) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        KeycloakUriInfo uri = context.getUri();
        UriBuilder uriBuilder = DeviceGrantType.oauth2DeviceVerificationCompletedUrl(uri);
        String errorType = OAuthErrorException.SERVER_ERROR;
        if (error == LoginProtocol.Error.CONSENT_DENIED) {
            String verifiedUserCode = authSession.getClientNote(DeviceGrantType.OAUTH2_DEVICE_VERIFIED_USER_CODE);
            OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
            if (!store.deny(realm, verifiedUserCode)) {
                // Already expired and removed in the store
                errorType = OAuthErrorException.EXPIRED_TOKEN;
            } else {
                errorType = OAuthErrorException.ACCESS_DENIED;
            }
        }
        return Response.status(302).location(
                uriBuilder.queryParam(OAuth2Constants.ERROR, errorType)
                        .build(realm.getName())
        ).build();
    }

    public static Response approveOAuth2DeviceAuthorization(AuthenticationSessionModel authSession, AuthenticatedClientSessionModel clientSession, KeycloakSession session) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        KeycloakUriInfo uriInfo = context.getUri();
        UriBuilder uriBuilder = DeviceGrantType.oauth2DeviceVerificationCompletedUrl(uriInfo);

        String verifiedUserCode = authSession.getClientNote(DeviceGrantType.OAUTH2_DEVICE_VERIFIED_USER_CODE);
        String userSessionId = clientSession.getUserSession().getId();
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        if (!store.approve(realm, verifiedUserCode, userSessionId, null)) {
            // Already expired and removed in the store
            return Response.status(302).location(
                    uriBuilder.queryParam(OAuth2Constants.ERROR, OAuthErrorException.EXPIRED_TOKEN)
                            .build(realm.getName())
            ).build();
        }

        // Now, remove the verified user code
        store.removeUserCode(realm, verifiedUserCode);

        return Response.status(302).location(
                uriBuilder.build(realm.getName())
        ).build();
    }

    public static boolean isOAuth2DeviceVerificationFlow(final AuthenticationSessionModel authSession) {
        String flow = authSession.getClientNote(DeviceGrantType.OAUTH2_DEVICE_VERIFIED_USER_CODE);
        return flow != null;
    }

    private MultivaluedMap<String, String> formParams;
    private ClientModel client;

    private KeycloakSession session;

    private TokenEndpoint tokenEndpoint;

    private final RealmModel realm;
    private final EventBuilder event;

    private Cors cors;

    public DeviceGrantType(MultivaluedMap<String, String> formParams, ClientModel client, KeycloakSession session,
        TokenEndpoint tokenEndpoint, RealmModel realm, EventBuilder event, Cors cors) {
        this.formParams = formParams;
        this.client = client;
        this.session = session;
        this.tokenEndpoint = tokenEndpoint;
        this.realm = realm;
        this.event = event;
        this.cors = cors;
    }

    public Response oauth2DeviceFlow() {
        if (!realm.getOAuth2DeviceConfig().isOAuth2DeviceAuthorizationGrantEnabled(client)) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                "Client not allowed OAuth 2.0 Device Authorization Grant", Response.Status.BAD_REQUEST);
        }

        String deviceCode = formParams.getFirst(OAuth2Constants.DEVICE_CODE);
        if (deviceCode == null) {
            event.error(Errors.INVALID_OAUTH2_DEVICE_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                "Missing parameter: " + OAuth2Constants.DEVICE_CODE, Response.Status.BAD_REQUEST);
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel deviceCodeModel = store.getByDeviceCode(realm, deviceCode);

        if (deviceCodeModel == null) {
            event.error(Errors.INVALID_OAUTH2_DEVICE_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Device code not valid",
                Response.Status.BAD_REQUEST);
        }

        if (deviceCodeModel.isExpired()) {
            event.error(Errors.EXPIRED_OAUTH2_DEVICE_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.EXPIRED_TOKEN, "Device code is expired",
                Response.Status.BAD_REQUEST);
        }

        if (!store.isPollingAllowed(deviceCodeModel)) {
            event.error(Errors.SLOW_DOWN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SLOW_DOWN, "Slow down", Response.Status.BAD_REQUEST);
        }

        if (deviceCodeModel.isDenied()) {
            event.error(Errors.ACCESS_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED,
                "The end user denied the authorization request", Response.Status.BAD_REQUEST);
        }

        if (deviceCodeModel.isPending()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.AUTHORIZATION_PENDING,
                "The authorization request is still pending", Response.Status.BAD_REQUEST);
        }

        // Approved

        String userSessionId = deviceCodeModel.getUserSessionId();
        event.detail(Details.CODE_ID, userSessionId);
        event.session(userSessionId);

        // Retrieve UserSession
        UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, userSessionId,
            client.getId());

        if (userSession == null) {
            userSession = session.sessions().getUserSession(realm, userSessionId);
            if (userSession == null) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.AUTHORIZATION_PENDING,
                    "The authorization request is verified but can not lookup the user session yet",
                    Response.Status.BAD_REQUEST);
            }
        }

        // Now, remove the device code
        store.removeDeviceCode(realm, deviceCode);

        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User not found",
                Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());

        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User disabled",
                Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Auth error",
                Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Session not active",
                Response.Status.BAD_REQUEST);
        }

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in device_code-to-token request, it could just theoretically happen that they are not available)
        String scopeParam = deviceCodeModel.getScope();
        Stream<ClientScopeModel> clientScopes = TokenManager.getRequestedClientScopes(scopeParam, client);
        if (!TokenManager.verifyConsentStillAvailable(session, user, client, clientScopes)) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE,
                "Client no longer has requested consent from user", Response.Status.BAD_REQUEST);
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndClientScopes(clientSession,
            clientScopes, session);

        // Set nonce as an attribute in the ClientSessionContext. Will be used for the token generation
        clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, deviceCodeModel.getNonce());

        return tokenEndpoint.createTokenResponse(user, userSession, clientSessionCtx, scopeParam, false);
    }
}