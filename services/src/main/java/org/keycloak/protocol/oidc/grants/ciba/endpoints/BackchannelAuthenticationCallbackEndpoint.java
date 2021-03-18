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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationRequest;
import org.keycloak.protocol.oidc.grants.ciba.channel.HttpAuthenticationChannelProvider;
import org.keycloak.protocol.oidc.grants.ciba.resolvers.CIBALoginUserResolver;
import org.keycloak.services.ErrorResponseException;

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

        authenticateClient();

        MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();
        AuthenticationRequest request = verifyAuthenticationRequest(formParams);
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

    private AuthenticationRequest verifyAuthenticationRequest(MultivaluedMap<String, String> formParams) {
        String jwe = formParams.getFirst(HttpAuthenticationChannelProvider.AUTHENTICATION_CHANNEL_ID);
        AuthenticationRequest request;

        try {
            request = AuthenticationRequest.deserialize(session, jwe);
        } catch (Exception e) {
            event.error(Errors.INVALID_INPUT);
            // authentication channel id format is invalid or it has already been used
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Invalid authentication request",
                    Response.Status.BAD_REQUEST);
        }

        event.detail(Details.CODE_ID, request.getSessionState());
        event.session(request.getSessionState());

        if (Time.currentTime() > request.getExp().intValue()) {
            event.error(Errors.SESSION_EXPIRED);
            cancelRequest(request);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "The authentication request expired",
                    Response.Status.BAD_REQUEST);
        }

        // to bind Client Session of CD(Consumption Device) with User Session, set CD's Client Model to this class member "client".
        ClientModel issuedFor = realm.getClientByClientId(request.getIssuedFor());

        if (issuedFor == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid token recipient",
                    Response.Status.BAD_REQUEST);
        }

        if (!issuedFor.getClientId().equals(request.getIssuedFor())) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Token recipient mismatch",
                    Response.Status.BAD_REQUEST);
        }

        event.client(issuedFor);

        try {
            CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);
            String authenticatedUserId = resolver.getUserFromInfoUsedByAuthentication(
                    formParams.getFirst(HttpAuthenticationChannelProvider.AUTHENTICATION_CHANNEL_USER_INFO)).getId();

            if (!request.getSubject().equals(authenticatedUserId)) {
                event.error(DIFFERENT_USER_AUTHENTICATED);
                cancelRequest(request);
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, DIFFERENT_USER_AUTHENTICATED,
                        Response.Status.BAD_REQUEST);
            }
        } catch (ErrorResponseException ere) {
            throw ere;
        } catch (Exception cause) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Failied to resolve user",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        return request;
    }

    private void cancelRequest(AuthenticationRequest authReqId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.removeDeviceCode(realm, authReqId.getId());
        store.removeUserCode(realm, authReqId.getAuthResultId());
    }


    private void approveRequest(AuthenticationRequest authReqId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.approve(realm, authReqId.getAuthResultId(), "fake");
    }

    private void denyRequest(AuthenticationRequest authReqId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.deny(realm, authReqId.getAuthResultId());
    }
}
