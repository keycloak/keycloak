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

import static org.keycloak.events.Errors.DIFFERENT_USER_AUTHENTICATED;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.protocol.oidc.grants.ciba.channel.HttpAuthenticationChannelProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;

public class BackchannelAuthenticationCallbackEndpoint extends AbstractCibaEndpoint {

    public static final String SUCCEEDED = "succeeded";
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String CANCELLED = "cancelled";

    @Context
    private HttpRequest httpRequest;

    public BackchannelAuthenticationCallbackEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
    }

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processAuthenticationChannelResult() {
        event.event(EventType.LOGIN);
        MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();
        AccessToken request = verifyAuthenticationRequest(httpRequest.getHttpHeaders());
        String status = formParams.getFirst(HttpAuthenticationChannelProvider.AUTHENTICATION_STATUS);

        if (SUCCEEDED.equals(status)) {
            approveRequest(request);
            return Response.ok("", MediaType.APPLICATION_JSON_TYPE)
                    .header(HttpHeaderNames.CACHE_CONTROL, "no-store")
                    .header(HttpHeaderNames.PRAGMA, "no-cache")
                    .build();
        }

        if (status == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "authentication result not specified",
                    Response.Status.BAD_REQUEST);
        } else if (status.equals(CANCELLED)) {
            event.error(Errors.NOT_ALLOWED);
            denyRequest(request);
        } else if (status.equals(UNAUTHORIZED)) {
            event.error(Errors.CONSENT_DENIED);
            denyRequest(request);
        }

        throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid authentication status",
                Response.Status.BAD_REQUEST);
    }

    private AccessToken verifyAuthenticationRequest(HttpHeaders headers) {
        String jwe = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(headers);

        if (jwe == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.UNAUTHORIZED);
        }

        AccessToken bearerToken;

        try {
            bearerToken = TokenVerifier.createWithoutSignature(session.tokens().decode(jwe, AccessToken.class))
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
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.FORBIDDEN);
        }

        event.detail(Details.CODE_ID, bearerToken.getSessionState());
        event.session(bearerToken.getSessionState());

        ClientModel issuedFor = realm.getClientByClientId(bearerToken.getIssuedFor());

        if (issuedFor == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid token recipient",
                    Response.Status.BAD_REQUEST);
        }

        if (!deviceCode.getClientId().equals(bearerToken.getIssuedFor())) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Token recipient mismatch",
                    Response.Status.BAD_REQUEST);
        }

        event.client(issuedFor);

        return bearerToken;
    }

    private void cancelRequest(String authReqId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel userCode = store.getByUserCode(realm, authReqId);
        store.removeDeviceCode(realm, userCode.getDeviceCode());
        store.removeUserCode(realm, authReqId);
    }


    private void approveRequest(AccessToken authReqId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.approve(realm, authReqId.getId(), "fake");
    }

    private void denyRequest(AccessToken authReqId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.deny(realm, authReqId.getId());
    }
}
