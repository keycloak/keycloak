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
package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status;
import org.keycloak.protocol.oidc.grants.device.DeviceGrantType;
import org.keycloak.protocol.oidc.grants.device.endpoints.DeviceEndpoint;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.CANCELLED;

public class BackchannelAuthenticationCallbackEndpoint extends AbstractCibaEndpoint {

    private static final Logger logger = Logger.getLogger(BackchannelAuthenticationCallbackEndpoint.class);

    private final HttpRequest httpRequest;

    public BackchannelAuthenticationCallbackEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
        this.httpRequest = session.getContext().getHttpRequest();
    }

    @Path("/")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processAuthenticationChannelResult(AuthenticationChannelResponse response) {
        event.event(EventType.LOGIN);
        BackchannelAuthCallbackContext ctx = verifyAuthenticationRequest(getRawBearerToken(httpRequest.getHttpHeaders(), response));
        AccessToken bearerToken = ctx.bearerToken;
        OAuth2DeviceCodeModel deviceModel = ctx.deviceModel;

        Status status = response.getStatus();

        if (status == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid authentication status",
                    Response.Status.BAD_REQUEST);
        }

        status = preApprove(response);
        
        switch (status) {
            case SUCCEED:
                approveRequest(bearerToken.getId(), response.getAdditionalParams());
                break;
            case CANCELLED:
            case UNAUTHORIZED:
                denyRequest(bearerToken.getId(), status);
                break;
        }

        // Call the notification endpoint
        ClientModel client = session.getContext().getClient();
        CibaConfig cibaConfig = realm.getCibaPolicy();
        if (cibaConfig.getBackchannelTokenDeliveryMode(client).equals(CibaConfig.CIBA_PING_MODE)) {
            sendClientNotificationRequest(client, cibaConfig, deviceModel);
        }

        return Response.ok(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected BackchannelAuthCallbackContext verifyAuthenticationRequest(String rawBearerToken) {

        if (rawBearerToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.UNAUTHORIZED);
        }

        AccessToken bearerToken;

        try {
            bearerToken = TokenVerifier.createWithoutSignature(session.tokens().decode(rawBearerToken, AccessToken.class))
                    .withDefaultChecks()
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))
                    .checkActive(true)
                    .audience(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))
                    .verify().getToken();
        } catch (Exception e) {
            event.error(Errors.INVALID_TOKEN);
            // authentication channel id format is invalid or it has already been used
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.FORBIDDEN);
        }

        OAuth2DeviceCodeModel deviceCode = DeviceEndpoint.getDeviceByUserCode(session, realm, bearerToken.getId());

        if (deviceCode == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.FORBIDDEN);
        }

        if (!deviceCode.isPending()) {
            cancelRequest(bearerToken.getId());
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.FORBIDDEN);
        }

        ClientModel issuedFor = realm.getClientByClientId(bearerToken.getIssuedFor());

        if (issuedFor == null || !issuedFor.isEnabled()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid token recipient",
                    Response.Status.BAD_REQUEST);
        }

        if (!deviceCode.getClientId().equals(issuedFor.getClientId())) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Token recipient mismatch",
                    Response.Status.BAD_REQUEST);
        }

        session.getContext().setClient(issuedFor);
        event.client(issuedFor);

        return new BackchannelAuthCallbackContext(bearerToken, deviceCode);
    }

    /**
     * Handels the cancellation of an authentication request.
     * 
     * @param authResultId The id to identify the request.
     */
    protected void cancelRequest(String authResultId) {
        OAuth2DeviceCodeModel userCode = DeviceEndpoint.getDeviceByUserCode(session, realm, authResultId);
        DeviceGrantType.removeDeviceByDeviceCode(session, userCode.getDeviceCode());
        DeviceGrantType.removeDeviceByUserCode(session, realm, authResultId);
    }

    /**
     * Is called before the request approving, allows additional validation of other factors.
     * 
     * @param response The {@link AuthenticationChannelResponse} to work with.
     *                 
     * @return The {@link Status} of the response, after pre-approving.
     */
    protected Status preApprove(AuthenticationChannelResponse response) {
        return response.getStatus();
    }

    /**
     * Approves the request respectively the code.
     * 
     * @param authReqId The id to identify the request.
     * @param additionalParams Additional parameters.
     */
    protected void approveRequest(String authReqId, Map<String, String> additionalParams) {
        DeviceGrantType.approveUserCode(session, realm, authReqId, "fake", additionalParams);
    }

    protected void denyRequest(String authReqId, Status status) {
        if (CANCELLED.equals(status)) {
            event.error(Errors.NOT_ALLOWED);
        } else {
            event.error(Errors.CONSENT_DENIED);
        }

        DeviceGrantType.denyUserCode(session, realm, authReqId);
    }

    /**
     * Extracts the raw bearer token from the request.
     * 
     * @param httpHeaders The request headers.
     * @param response The {@link AuthenticationChannelResponse}
     *                 
     * @return The raw bearer token.
     */
    protected String getRawBearerToken(HttpHeaders httpHeaders, AuthenticationChannelResponse response) {
        AppAuthManager.AuthHeader authHeader = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(httpHeaders);
        return authHeader == null ? null : authHeader.getToken();
    }

    protected void sendClientNotificationRequest(ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel) {
        String clientNotificationEndpoint = cibaConfig.getBackchannelClientNotificationEndpoint(client);
        if (clientNotificationEndpoint == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client notification endpoint not set for the client with the ping mode",
                    Response.Status.BAD_REQUEST);
        }

        logger.debugf("Sending request to client notification endpoint '%s' for the client '%s'", clientNotificationEndpoint, client.getClientId());

        ClientNotificationEndpointRequest clientNotificationRequest = new ClientNotificationEndpointRequest();
        clientNotificationRequest.setAuthReqId(deviceModel.getAuthReqId());

        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doPost(clientNotificationEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .json(clientNotificationRequest)
                .auth(deviceModel.getClientNotificationToken());

        try {
            int notificationResponseStatus = simpleHttp.asStatus();

            logger.tracef("Received status '%d' from request to client notification endpoint '%s' for the client '%s'", notificationResponseStatus, clientNotificationEndpoint, client.getClientId());
            if (notificationResponseStatus != 200 && notificationResponseStatus != 204) {
                logger.warnf("Invalid status returned from client notification endpoint '%s' of client '%s'", clientNotificationEndpoint, client.getClientId());
                event.error(Errors.INVALID_REQUEST);
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Failed to send request to client notification endpoint",
                        Response.Status.BAD_REQUEST);
            }
        } catch (IOException ioe) {
            logger.errorf(ioe, "Failed to send request to client notification endpoint '%s' of client '%s'", clientNotificationEndpoint, client.getClientId());
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Failed to send request to client notification endpoint",
                    Response.Status.BAD_REQUEST);
        }
    }

    protected static class BackchannelAuthCallbackContext {

        private final AccessToken bearerToken;
        private final OAuth2DeviceCodeModel deviceModel;

        private BackchannelAuthCallbackContext(AccessToken bearerToken, OAuth2DeviceCodeModel deviceModel) {
            this.bearerToken = bearerToken;
            this.deviceModel = deviceModel;
        }

    }
}
