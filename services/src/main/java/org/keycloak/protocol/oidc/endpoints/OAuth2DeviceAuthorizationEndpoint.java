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

package org.keycloak.protocol.oidc.endpoints;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.RandomString;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.models.OAuth2DeviceUserCodeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.OAuth2DeviceAuthorizationResponse;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;

import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class OAuth2DeviceAuthorizationEndpoint extends AuthorizationEndpointBase {

    private static final Logger logger = Logger.getLogger(OAuth2DeviceAuthorizationEndpoint.class);

    private enum Action {
        OAUTH2_DEVICE_AUTH, OAUTH2_DEVICE_VERIFY_USER_CODE
    }

    private ClientModel client;
    private AuthenticationSessionModel authenticationSession;
    private Action action;
    private AuthorizationEndpointRequest request;
    private Cors cors;

    public OAuth2DeviceAuthorizationEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response buildPost() {
        logger.trace("Processing @POST request");
        cors = Cors.add(httpRequest).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
        action = Action.OAUTH2_DEVICE_AUTH;
        event.event(EventType.OAUTH2_DEVICE_AUTH);
        return process(httpRequest.getDecodedFormParameters());
    }

    private Response process(MultivaluedMap<String, String> params) {
        checkSsl();
        checkRealm();
        checkClient(null);

        request = AuthorizationEndpointRequestParserProcessor.parseRequest(event, session, client, params);

        if (!TokenUtil.isOIDCRequest(request.getScope())) {
            ServicesLogger.LOGGER.oidcScopeMissing();
        }

        authenticationSession = createAuthenticationSession(client, request.getState());
        updateAuthenticationSession();

        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();
        switch (action) {
            case OAUTH2_DEVICE_AUTH:
                return buildDeviceAuthorizationResponse();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    public Response buildDeviceAuthorizationResponse() {
        if (!client.isOAuth2DeviceAuthorizationGrantEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Client not allowed for OAuth 2.0 Device Authorization Grant", Response.Status.BAD_REQUEST);
        }

        int expiresIn = realm.getOAuth2DeviceCodeLifespan();
        int interval = realm.getOAuth2DevicePollingInterval();

        OAuth2DeviceCodeModel deviceCode = OAuth2DeviceCodeModel.create(realm, client,
                Base64Url.encode(KeycloakModelUtils.generateSecret()), request.getScope(), request.getNonce());

        // TODO Configure user code format
        String secret = new RandomString(10, new SecureRandom(), RandomString.upper).nextString();
        OAuth2DeviceUserCodeModel userCode = new OAuth2DeviceUserCodeModel(realm,
                deviceCode.getDeviceCode(),
                secret);

        // To inform "expired_token" to the client, the lifespan of the cache provider is longer than device code
        int lifespanSeconds = expiresIn + interval + 10;

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.put(deviceCode, userCode, lifespanSeconds);

        try {
            String deviceUrl = RealmsResource.oauth2DeviceVerificationUrl(session.getContext().getUri()).build(realm.getName()).toString();

            OAuth2DeviceAuthorizationResponse response = new OAuth2DeviceAuthorizationResponse();
            response.setDeviceCode(deviceCode.getDeviceCode());
            response.setUserCode(secret);
            response.setExpiresIn(expiresIn);
            response.setInterval(interval);
            response.setVerificationUri(deviceUrl);
            response.setVerificationUriComplete(deviceUrl + "?user_code=" + response.getUserCode());

            return Response.ok(JsonSerialization.writeValueAsBytes(response)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating OAuth 2.0 Device Authorization Response.", e);
        }
    }

    private void checkClient(String clientId) {
        // https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-3.1
        // The spec says "The client authentication requirements of Section 3.2.1 of [RFC6749]
        // apply to requests on this endpoint".
        if (action == Action.OAUTH2_DEVICE_AUTH) {
            AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event);
            client = clientAuth.getClient();
            clientId = client.getClientId();
        }

        if (clientId == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.MISSING_PARAMETER, OIDCLoginProtocol.CLIENT_ID_PARAM);
        }

        event.client(clientId);

        client = realm.getClientByClientId(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.CLIENT_DISABLED);
        }

        if (!client.isOAuth2DeviceAuthorizationGrantEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.OAUTH2_DEVICE_AUTHORIZATION_GRANT_DISABLED);
        }

        if (client.isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, authenticationSession, Response.Status.FORBIDDEN, Messages.BEARER_ONLY);
        }

        String protocol = client.getProtocol();
        if (protocol == null) {
            logger.warnf("Client '%s' doesn't have protocol set. Fallback to openid-connect. Please fix client configuration", clientId);
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }
        if (!protocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, "Wrong client protocol.");
        }

        session.getContext().setClient(client);
    }

    private void updateAuthenticationSession() {
        authenticationSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authenticationSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        authenticationSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

        if (request.getNonce() != null) authenticationSession.setClientNote(OIDCLoginProtocol.NONCE_PARAM, request.getNonce());
        if (request.getMaxAge() != null) authenticationSession.setClientNote(OIDCLoginProtocol.MAX_AGE_PARAM, String.valueOf(request.getMaxAge()));
        if (request.getScope() != null) authenticationSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, request.getScope());
        if (request.getLoginHint() != null) authenticationSession.setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, request.getLoginHint());
        if (request.getPrompt() != null) authenticationSession.setClientNote(OIDCLoginProtocol.PROMPT_PARAM, request.getPrompt());
        if (request.getIdpHint() != null) authenticationSession.setClientNote(AdapterConstants.KC_IDP_HINT, request.getIdpHint());
        if (request.getClaims()!= null) authenticationSession.setClientNote(OIDCLoginProtocol.CLAIMS_PARAM, request.getClaims());
        if (request.getAcr() != null) authenticationSession.setClientNote(OIDCLoginProtocol.ACR_PARAM, request.getAcr());
        if (request.getDisplay() != null) authenticationSession.setAuthNote(OAuth2Constants.DISPLAY, request.getDisplay());

        if (request.getAdditionalReqParams() != null) {
            for (String paramName : request.getAdditionalReqParams().keySet()) {
                authenticationSession.setClientNote(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + paramName, request.getAdditionalReqParams().get(paramName));
            }
        }
    }

    public Response buildVerificationResponse(String userCode) {
        this.event.event(EventType.LOGIN);
        action = Action.OAUTH2_DEVICE_VERIFY_USER_CODE;

        checkSsl();
        checkRealm();

        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();

        client = SystemClientUtil.getSystemClient(realm);
        authenticationSession = createAuthenticationSession(client, null);
        authenticationSession.setClientNote(APP_INITIATED_FLOW, LoginActionsService.OAUTH2_DEVICE_VERIFICATION_PATH);

        if (StringUtil.isNullOrEmpty(userCode)) {
            return createVerificationPage(null);
        } else {
            return processVerification(userCode);
        }
    }

    public Response processVerification(AuthenticationSessionModel authSessionWithSystemClient, String userCode) {
        authenticationSession = authSessionWithSystemClient;
        return processVerification(userCode);
    }

    public Response processVerification(String userCode) {
        if (userCode == null) {
            event.error(Errors.INVALID_OAUTH2_USER_CODE);
            return createVerificationPage(Messages.OAUTH2_DEVICE_INVALID_USER_CODE);
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel deviceCodeModel = store.getByUserCode(realm, userCode);
        if (deviceCodeModel == null) {
            event.error(Errors.INVALID_OAUTH2_USER_CODE);
            return createVerificationPage(Messages.OAUTH2_DEVICE_INVALID_USER_CODE);
        }

        int expiresIn = deviceCodeModel.getExpiration() - Time.currentTime();
        if (expiresIn < 0) {
            event.error(Errors.EXPIRED_OAUTH2_USER_CODE);
            return createVerificationPage(Messages.OAUTH2_DEVICE_INVALID_USER_CODE);
        }

        // Update authentication session with requested clientId and scope from the device
        updateAuthenticationSession(deviceCodeModel);

        // Verification OK
        authenticationSession.setClientNote(OIDCLoginProtocol.OAUTH2_DEVICE_VERIFIED_USER_CODE, userCode);

        // Event logging for the verification
        event.client(deviceCodeModel.getClientId())
                .detail(Details.SCOPE, deviceCodeModel.getScope())
                .success();

        return redirectToBrowserAuthentication();
    }

    private AuthenticationSessionModel updateAuthenticationSession(OAuth2DeviceCodeModel deviceCode) {
        checkClient(deviceCode.getClientId());

        // Create request object using parameters which is used in device authorization request from the device
        request = AuthorizationEndpointRequestParserProcessor.parseRequest(event, session, client, deviceCode.getParams());

        // Re-create authentication session because current session doesn't have relation with the target device client
        authenticationSession = createAuthenticationSession(client, null);
        authenticationSession.setClientNote(APP_INITIATED_FLOW, LoginActionsService.OAUTH2_DEVICE_VERIFICATION_PATH);

        updateAuthenticationSession();

        AuthenticationManager.setClientScopesInSession(authenticationSession);

        return authenticationSession;
    }

    public Response createVerificationPage(String errorMessage) {
        String execution = AuthenticatedClientSessionModel.Action.USER_CODE_VERIFICATION.name();

        ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authenticationSession);
        authenticationSession.getParentSession().setTimestamp(Time.currentTime());

        accessCode.setAction(AuthenticationSessionModel.Action.USER_CODE_VERIFICATION.name());
        authenticationSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution);

        LoginFormsProvider provider = session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(authenticationSession)
                .setExecution(execution)
                .setClientSessionCode(accessCode.getOrGenerateCode());

        if (errorMessage != null) {
            provider = provider.setError(errorMessage);
        }

        return provider.createOAuth2DeviceVerifyUserCodePage();
    }

    public Response redirectToBrowserAuthentication() {
        return handleBrowserAuthenticationRequest(authenticationSession, new OIDCLoginProtocol(session, realm, session.getContext().getUri(), headers, event), false, true);
    }
}
