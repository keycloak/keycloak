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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.CANCELLED;

public class BackchannelAuthenticationCallbackEndpoint extends AbstractCibaEndpoint {

    @Context
    private HttpRequest httpRequest;

    public BackchannelAuthenticationCallbackEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
    }

    @Path("/")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processAuthenticationChannelResult(AuthenticationChannelResponse response) {
        event.event(EventType.LOGIN);
        AccessToken bearerToken = verifyAuthenticationRequest(httpRequest.getHttpHeaders());
        Status status = response.getStatus();

        if (status == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid authentication status",
                    Response.Status.BAD_REQUEST);
        }

        switch (status) {
            case SUCCEED:
                approveRequest(bearerToken, response.getAdditionalParams());
                break;
            case CANCELLED:
            case UNAUTHORIZED:
                denyRequest(bearerToken, status);
                break;
        }

        return Response.ok(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private AccessToken verifyAuthenticationRequest(HttpHeaders headers) {
        String rawBearerToken = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(headers);

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

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel deviceCode = store.getByUserCode(realm, bearerToken.getId());

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

        event.client(issuedFor);

        return bearerToken;
    }

    private void cancelRequest(String authResultId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel userCode = store.getByUserCode(realm, authResultId);
        store.removeDeviceCode(realm, userCode.getDeviceCode());
        store.removeUserCode(realm, authResultId);
    }

    private void approveRequest(AccessToken authReqId, Map<String, String> additionalParams) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.approve(realm, authReqId.getId(), "fake", additionalParams);
    }

    private void denyRequest(AccessToken authReqId, Status status) {
        if (CANCELLED.equals(status)) {
            event.error(Errors.NOT_ALLOWED);
        } else {
            event.error(Errors.CONSENT_DENIED);
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);

        store.deny(realm, authReqId.getId());
    }
}
