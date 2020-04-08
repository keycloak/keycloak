/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.ciba.channel;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ciba.AuthenticationChannelResult;
import org.keycloak.protocol.ciba.AuthenticationChannelStatus;
import org.keycloak.protocol.ciba.CIBAAuthReqId;
import org.keycloak.protocol.ciba.CIBAConstants;
import org.keycloak.protocol.ciba.endpoints.request.BackchannelAuthenticationRequest;
import org.keycloak.protocol.ciba.resolvers.CIBALoginUserResolver;
import org.keycloak.protocol.ciba.utils.CIBAAuthReqIdParser;
import org.keycloak.protocol.ciba.utils.AuthenticationChannelResultParser;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;

public class HttpAuthenticationChannelProvider extends HttpAuthenticationChannelProviderBase {

    private static final Logger logger = Logger.getLogger(HttpAuthenticationChannelProvider.class);

    protected final String httpAuthenticationChannelUri;

    public HttpAuthenticationChannelProvider(KeycloakSession session, String httpAuthenticationRequestUri) {
        super(session);
        this.httpAuthenticationChannelUri = httpAuthenticationRequestUri;
    }

    protected String scope;
    protected String userSessionIdWillBeCreated;
    protected String userIdToBeAuthenticated;
    protected String authResultId;
    protected int expiration;

    @Override
    protected String getScope() {
        return scope;
    }

    @Override
    protected String getUserSessionIdWillBeCreated() {
        return userSessionIdWillBeCreated;
    }

    @Override
    protected String getUserIdToBeAuthenticated() {
        return userIdToBeAuthenticated;
    }

    @Override
    protected String getAuthResultId() {
        return authResultId;
    }

    @Override
    protected int getExpiration() {
        return expiration;
    }

    @Override
    protected Response verifyAuthenticationChannelResult() {
        String authenticationChannelId = formParams.getFirst(HttpAuthenticationChannelProvider.AUTHENTICATION_CHANNEL_ID);
        ParseResult parseResult = parseAuthenticationChannelId(session, authenticationChannelId, event);

        if (parseResult.isIllegalAuthenticationChannelId()) {
            event.error(Errors.INVALID_INPUT);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.UNKNOWN);
            // authentication channel id format is invalid or it has already been used
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "invalid authentication channel id", Response.Status.BAD_REQUEST);
        } else if (parseResult.isExpiredAuthenticationChannelId()) {
            event.error(Errors.SESSION_EXPIRED);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.EXPIRED);
            return cors.builder(Response.ok("", MediaType.APPLICATION_JSON_TYPE)).build();
        }

        CIBAAuthReqId authenticationChannelIdJwt = parseResult.authenticationChannelIdJwt();
        authResultId = authenticationChannelIdJwt.getAuthResultId();
        scope = authenticationChannelIdJwt.getScope();
        expiration = authenticationChannelIdJwt.getExp().intValue();
        userSessionIdWillBeCreated = authenticationChannelIdJwt.getSessionState();
        userIdToBeAuthenticated = authenticationChannelIdJwt.getSubject();
        // to bind Client Session of CD(Consumption Device) with User Session, set CD's Client Model to this class member "client".
        client = realm.getClientByClientId(authenticationChannelIdJwt.getIssuedFor());

        CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);
        if (resolver == null) {
            throw new RuntimeException("CIBA Login User Resolver not setup properly.");
        }
        String userIdAuthenticated = resolver.getUserFromInfoUsedByAuthentication(formParams.getFirst(HttpAuthenticationChannelProvider.AUTHENTICATION_CHANNEL_USER_INFO)).getId();
        if (!userIdToBeAuthenticated.equals(userIdAuthenticated)) {
            event.error(Errors.DIFFERENT_USER_AUTHENTICATED);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.DIFFERENT);
            return cors.builder(Response.status(Status.BAD_REQUEST)).build();
        }

        String authResult = formParams.getFirst(HttpAuthenticationChannelProvider.AUTHENTICATION_CHANNEL_RESULT);
        if (authResult == null) {
            event.error(Errors.INVALID_INPUT);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.UNKNOWN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "authentication result not specified", Response.Status.BAD_REQUEST);
        } else if (authResult.equals(AuthenticationChannelStatus.FAILED)) {
            event.error(Errors.NOT_LOGGED_IN);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.FAILED);
        } else if (authResult.equals(AuthenticationChannelStatus.CANCELLED)) {
            event.error(Errors.NOT_ALLOWED);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.CANCELLED);
        } else if (authResult.equals(AuthenticationChannelStatus.UNAUTHORIZED)) {
            event.error(Errors.CONSENT_DENIED);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.UNAUTHORIZED);
        } else if (authResult.equals(AuthenticationChannelStatus.SUCCEEDED)) {
            return null;
        } else {
            event.error(Errors.INVALID_INPUT);
            persistAuthenticationChannelResult(AuthenticationChannelStatus.UNKNOWN);
        }
        return cors.builder(Response.ok("", MediaType.APPLICATION_JSON_TYPE)).build();
    }

    @Override
    public void requestAuthentication(ClientModel client, BackchannelAuthenticationRequest request, int expiresIn, String authResultId, String userSessionIdWillBeCreated) {
        // create JWT formatted/JWS signed/JWE encrypted Authentication Channel ID by the same manner in creating auth_req_id
        // Authentication Channel ID binds Backchannel Authentication Request with Authentication by AD(Authentication Device).
        // By including userSessionIdWillBeCreated. keycloak can create UserSession whose ID is userSessionIdWillBeCreated on Authentication Channel Callback Endpoint,
        // which can bind userSessionIdWillBeCreated (namely, Backchannel Authentication Request) with authenticated UserSession.
        // By including authResultId, keycloak can create Authentication Channel Result of Authentication by AD on Authentication Channel Callback Endpoint,
        // which can bind authResultId with Authentication Channel Result of Authentication by AD.
        // By including client_id, Authentication Channel Callback Endpoint can recognize the CD(Consumption Device) who sent Backchannel Authentication Request.

        // The following scopes should be displayed on AD(Authentication Device):
        // 1. scopes specified explicitly as query parameter in the authorization request
        // 2. scopes specified implicitly as default client scope in keycloak

        checkAuthenticationChannel();

        CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);
        if (resolver == null) {
            throw new RuntimeException("CIBA Login User Resolver not setup properly.");
        }
        String authRequestedUserHint = realm.getCIBAPolicy().getAuthRequestedUserHint();
        UserModel user;
        if (authRequestedUserHint.equals(CIBAConstants.LOGIN_HINT)) {
            user = resolver.getUserFromLoginHint(request.getLoginHint());
        } else if (authRequestedUserHint.equals(CIBAConstants.ID_TOKEN_HINT)) {
            user = resolver.getUserFromLoginHint(request.getIdTokenHint());
        } else if (authRequestedUserHint.equals(CIBAConstants.LOGIN_HINT_TOKEN)) {
            user = resolver.getUserFromLoginHint(request.getLoginHintToken());
        } else {
            throw new RuntimeException("CIBA invalid Authentication Requested User Hint.");
        }
        String infoUsedByAuthentication = resolver.getInfoUsedByAuthentication(user);

        StringBuilder scopeBuilder = new StringBuilder();
        Map<String, ClientScopeModel> defaultScopesMap = client.getClientScopes(true, true);
        defaultScopesMap.forEach((key, value)->{if (value.isDisplayOnConsentScreen()) scopeBuilder.append(value.getName()).append(" ");});
        String defaultClientScope = scopeBuilder.toString();

        CIBAAuthReqId authenticationChannelIdJwt = new CIBAAuthReqId();
        authenticationChannelIdJwt.id(KeycloakModelUtils.generateId());
        authenticationChannelIdJwt.setScope(request.getScope());
        authenticationChannelIdJwt.setSessionState(userSessionIdWillBeCreated);
        authenticationChannelIdJwt.setAuthResultId(authResultId);
        authenticationChannelIdJwt.issuedNow();
        authenticationChannelIdJwt.issuer(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authenticationChannelIdJwt.audience(authenticationChannelIdJwt.getIssuer());
        authenticationChannelIdJwt.subject(user.getId());
        authenticationChannelIdJwt.exp(Long.valueOf(Time.currentTime() + expiresIn));
        //authenticationChannelIdJwt.issuedFor(client_id of the external entity via Authentication Channel);
        authenticationChannelIdJwt.issuedFor(client.getClientId()); // TODO : set CD's client_id intentionally, not the external entity via Authentication Channel. It is not good idea so that client_id field should be added.
        String authenticationChannelId = CIBAAuthReqIdParser.persistAuthReqId(session, authenticationChannelIdJwt);

        try {
            SimpleHttp simpleHttp = SimpleHttp.doPost(httpAuthenticationChannelUri, session)
                .param(AUTHENTICATION_CHANNEL_ID, authenticationChannelId)
                .param(AUTHENTICATION_CHANNEL_USER_INFO, infoUsedByAuthentication)
                .param(AUTHENTICATION_CHANNEL_IS_CONSENT_REQUIRED, Boolean.toString(client.isConsentRequired()))
                .param(CIBAConstants.SCOPE, request.getScope())
                .param(AUTHENTICATION_CHANNEL_DEFAULT_CLIENT_SCOPE, defaultClientScope)
                .param(CIBAConstants.BINDING_MESSAGE, request.getBindingMessage());

            int status = completeAuthenticationChannelRequest(simpleHttp, user, client, request, expiresIn, authResultId).asStatus();

            if (status != Status.CREATED.getStatusCode()) {
                // To terminate CIBA flow, set Auth Result as unknown
                AuthenticationChannelResult authenticationChannelResult = new AuthenticationChannelResult(Time.currentTime() + expiresIn, AuthenticationChannelStatus.UNKNOWN);
                AuthenticationChannelResultParser.persistAuthenticationChannelResult(session, authResultId.toString(), authenticationChannelResult, Time.currentTime() + expiresIn);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Authentication Channel Access failed.", ioe);
        }
    }

    /**
     * Extension point to allow subclass to override this method in order to add datas to post to the external entity via authentication channel.
     */
    protected SimpleHttp completeAuthenticationChannelRequest(SimpleHttp simpleHttp, UserModel user, ClientModel client, BackchannelAuthenticationRequest request, int expiresIn, String authResultId) {
        return simpleHttp;
    }

    protected void checkAuthenticationChannel() {
        if (httpAuthenticationChannelUri == null) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
        if (!httpAuthenticationChannelUri.startsWith("http://") && !httpAuthenticationChannelUri.startsWith("https://")) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
    }

    public static final String AUTHENTICATION_CHANNEL_ID = "authentication_channel_id";
    public static final String AUTHENTICATION_CHANNEL_USER_INFO = "user_info";
    public static final String AUTHENTICATION_CHANNEL_RESULT = "auth_result";
    public static final String AUTHENTICATION_CHANNEL_IS_CONSENT_REQUIRED = "is_consent_required";
    public static final String AUTHENTICATION_CHANNEL_DEFAULT_CLIENT_SCOPE = "default_client_scope";

    public ParseResult parseAuthenticationChannelId(KeycloakSession session, String encodedJwt, EventBuilder event) {
        CIBAAuthReqId authenticationChannelIdJwt = null;
        try {
            authenticationChannelIdJwt = CIBAAuthReqIdParser.getAuthReqIdJwt(session, encodedJwt);
        } catch (Exception e) {
            logger.warnf("illegal format of authentication_channel_id : %s", e.getMessage());
            return (new ParseResult(null)).illegalAuthenticationChannelId();
        }
        ParseResult result = new ParseResult(authenticationChannelIdJwt);

        event.detail(Details.CODE_ID, result.authenticationChannelIdJwt.getSessionState());
        event.session(result.authenticationChannelIdJwt.getSessionState());

        // Finally doublecheck if code is not expired
        if (Time.currentTime() > result.authenticationChannelIdJwt.getExp().intValue()) {
            return result.expiredAuthenticationChannelId();
        }

        return result;
    }

    public class ParseResult {

        private final CIBAAuthReqId authenticationChannelIdJwt;

        private boolean isIllegalAuthenticationChannelId= false;
        private boolean isExpiredAuthenticationChannelId = false;

        private ParseResult(CIBAAuthReqId authenticationChannelIdJwt) {
            this.authenticationChannelIdJwt = authenticationChannelIdJwt;
        }

        public CIBAAuthReqId authenticationChannelIdJwt() {
            return authenticationChannelIdJwt;
        }

        public boolean isIllegalAuthenticationChannelId() {
            return isIllegalAuthenticationChannelId;
        }

        public boolean isExpiredAuthenticationChannelId() {
            return isExpiredAuthenticationChannelId;
        }

        private ParseResult illegalAuthenticationChannelId() {
            this.isIllegalAuthenticationChannelId = true;
            return this;
        }

        private ParseResult expiredAuthenticationChannelId() {
            this.isExpiredAuthenticationChannelId = true;
            return this;
        }
    }
}
