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

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceUserCodeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.grants.OAuth2GrantTypeBase;
import org.keycloak.protocol.oidc.grants.device.clientpolicy.context.DeviceTokenRequestContext;
import org.keycloak.protocol.oidc.grants.device.clientpolicy.context.DeviceTokenResponseContext;
import org.keycloak.protocol.oidc.grants.device.endpoints.DeviceEndpoint;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.protocol.oidc.OIDCLoginProtocolService.tokenServiceBaseUrl;

/**
 * OAuth 2.0 Device Authorization Grant
 * https://datatracker.ietf.org/doc/html/rfc8628#section-3.4
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 * @author <a href="mailto:michito.okai.zn@hitachi.com">Michito Okai</a>
 */
public class DeviceGrantType extends OAuth2GrantTypeBase {

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
            if (!denyUserCode(session, realm, verifiedUserCode)) {
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
        if (!approveUserCode(session, realm, verifiedUserCode, userSessionId, null)) {
            // Already expired and removed in the store
            return Response.status(302).location(
                    uriBuilder.queryParam(OAuth2Constants.ERROR, OAuthErrorException.EXPIRED_TOKEN)
                            .build(realm.getName())
            ).build();
        }

        // Now, remove the verified user code
        removeDeviceByUserCode(session, realm, verifiedUserCode);

        return Response.status(302).location(
                uriBuilder.build(realm.getName())
        ).build();
    }

    public static boolean isOAuth2DeviceVerificationFlow(final AuthenticationSessionModel authSession) {
        String flow = authSession.getClientNote(DeviceGrantType.OAUTH2_DEVICE_VERIFIED_USER_CODE);
        return flow != null;
    }

    public static OAuth2DeviceCodeModel getDeviceByDeviceCode(KeycloakSession session, RealmModel realm, ClientModel client, EventBuilder event, String deviceCode) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        Map<String, String> notes = singleUseStore.get(OAuth2DeviceCodeModel.createKey(deviceCode));
        OAuth2DeviceCodeModel deviceCodeModel = notes != null ? OAuth2DeviceCodeModel.fromCache(realm, deviceCode, notes) : null;

        if (deviceCodeModel != null) {
            if (!client.getClientId().equals(deviceCodeModel.getClientId())) {
                String errorMessage = "unauthorized client";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.INVALID_OAUTH2_DEVICE_CODE);
                throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, errorMessage,
                        Response.Status.BAD_REQUEST);
            }
        }

        return deviceCodeModel;
    }

    public static boolean isDeviceCodeDeniedForDeviceVerificationFlow(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession) {
        if (DeviceGrantType.isOAuth2DeviceVerificationFlow(authSession)) {
            String verifiedUserCode = authSession.getClientNote(DeviceGrantType.OAUTH2_DEVICE_VERIFIED_USER_CODE);
            OAuth2DeviceCodeModel deviceCodeModel = DeviceEndpoint.getDeviceByUserCode(session, realm, verifiedUserCode);
            if (deviceCodeModel != null) {
                return deviceCodeModel.isDenied();
            }
        }
        return false;
    }

    public static void removeDeviceByDeviceCode(KeycloakSession session, String deviceCode) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        singleUseStore.remove(OAuth2DeviceCodeModel.createKey(deviceCode));
    }

    public static void removeDeviceByUserCode(KeycloakSession session, RealmModel realm, String userCode) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        singleUseStore.remove(OAuth2DeviceUserCodeModel.createKey(realm, userCode));
    }

    public static boolean isPollingAllowed(KeycloakSession session, OAuth2DeviceCodeModel deviceCodeModel) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        return singleUseStore.putIfAbsent(deviceCodeModel.serializePollingKey(), deviceCodeModel.getPollingInterval());
    }

    public static boolean approveUserCode(KeycloakSession session, RealmModel realm, String userCode, String userSessionId, Map<String, String> additionalParams) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        OAuth2DeviceCodeModel deviceCodeModel = DeviceEndpoint.getDeviceByUserCode(session, realm, userCode);

        if (deviceCodeModel != null) {
            OAuth2DeviceCodeModel approvedDeviceCode = deviceCodeModel.approve(userSessionId, additionalParams);
            return singleUseStore.replace(approvedDeviceCode.serializeKey(), approvedDeviceCode.toMap());
        }

        return false;
    }

    public static boolean denyUserCode(KeycloakSession session, RealmModel realm, String userCode) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        OAuth2DeviceCodeModel deviceCodeModel = DeviceEndpoint.getDeviceByUserCode(session, realm, userCode);

        if (deviceCodeModel != null) {
            OAuth2DeviceCodeModel deniedDeviceCode = deviceCodeModel.deny();
            return singleUseStore.replace(deniedDeviceCode.serializeKey(), deniedDeviceCode.toMap());
        }

        return false;
    }

    @Override
    public Response process(Context context) {
        setContext(context);

        if (!realm.getOAuth2DeviceConfig().isOAuth2DeviceAuthorizationGrantEnabled(client)) {
            String errorMessage = "Client not allowed OAuth 2.0 Device Authorization Grant";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        String deviceCode = formParams.getFirst(OAuth2Constants.DEVICE_CODE);
        if (deviceCode == null) {
            String errorMessage = "Missing parameter: " + OAuth2Constants.DEVICE_CODE;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_OAUTH2_DEVICE_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        OAuth2DeviceCodeModel deviceCodeModel = getDeviceByDeviceCode(session, realm, client, event, deviceCode);

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

        if (!isPollingAllowed(session, deviceCodeModel)) {
            event.error(Errors.SLOW_DOWN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SLOW_DOWN, "Slow down", Response.Status.BAD_REQUEST);
        }

        if (deviceCodeModel.isDenied()) {
            String errorMessage = "The end user denied the authorization request";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.ACCESS_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        if (deviceCodeModel.isPending()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.AUTHORIZATION_PENDING,
                "The authorization request is still pending", Response.Status.BAD_REQUEST);
        }

        // https://tools.ietf.org/html/rfc7636#section-4.6
        String codeVerifier = formParams.getFirst(OAuth2Constants.CODE_VERIFIER);
        String codeChallenge = deviceCodeModel.getCodeChallenge();
        String codeChallengeMethod = deviceCodeModel.getCodeChallengeMethod();

        if (codeChallengeMethod != null && !codeChallengeMethod.isEmpty()) {
            PkceUtils.checkParamsForPkceEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, null, null, event, cors);
        } else {
            // PKCE Activation is OFF, execute the codes implemented in KEYCLOAK-2604
            PkceUtils.checkParamsForPkceNotEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, null, null, event, cors);
        }

        // Approved

        String userSessionId = deviceCodeModel.getUserSessionId();
        event.detail(Details.CODE_ID, userSessionId);
        event.session(userSessionId);

        // Retrieve UserSession
        var userSessionProvider = session.sessions();
        UserSessionModel userSession = userSessionProvider.getUserSessionIfClientExists(realm, userSessionId, false, client.getId());

        if (userSession == null) {
            userSession = userSessionProvider.getUserSession(realm, userSessionId);
            if (userSession == null) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.AUTHORIZATION_PENDING,
                    "The authorization request is verified but can not lookup the user session yet",
                    Response.Status.BAD_REQUEST);
            }
        }

        // Now, remove the device code
        removeDeviceByDeviceCode(session, deviceCode);

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
            String errorMessage = "Session not active";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage,
                Response.Status.BAD_REQUEST);
        }

        try {
            session.clientPolicy().triggerOnEvent(new DeviceTokenRequestContext(deviceCodeModel, formParams));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, cpe.getErrorDetail(),
                Response.Status.BAD_REQUEST);
        }

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in device_code-to-token request, it could just theoretically happen that they are not available)
        String scopeParam = deviceCodeModel.getScope();
        if (!TokenManager.verifyConsentStillAvailable(session, user, client, TokenManager.getRequestedClientScopes(session, scopeParam, client, user))) {
            String errorMessage = "Client no longer has requested consent from user";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE,
                    errorMessage, Response.Status.BAD_REQUEST);
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession,
                scopeParam, session);

        // Set nonce as an attribute in the ClientSessionContext. Will be used for the token generation
        clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, deviceCodeModel.getNonce());

        return createTokenResponse(user, userSession, clientSessionCtx, scopeParam, false, s -> {return new DeviceTokenResponseContext(deviceCodeModel, formParams, clientSession, s);});
    }

    @Override
    public EventType getEventType() {
        return EventType.OAUTH2_DEVICE_CODE_TO_TOKEN;
    }

}
