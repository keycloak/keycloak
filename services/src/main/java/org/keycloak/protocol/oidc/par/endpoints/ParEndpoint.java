/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.par.endpoints;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PushedAuthzRequestStoreProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointChecker;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.par.ParResponse;
import org.keycloak.protocol.oidc.par.clientpolicy.context.PushedAuthorizationRequestContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.resources.Cors;
import org.keycloak.utils.ProfileHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static org.keycloak.protocol.oidc.OIDCLoginProtocol.REQUEST_URI_PARAM;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pushed Authorization Request endpoint
 */
public class ParEndpoint extends AbstractParEndpoint {

    public static final String PAR_CREATED_TIME = "par.created.time";
    private static final String REQUEST_URI_PREFIX = "urn:ietf:params:oauth:request_uri:";
    public static final int REQUEST_URI_PREFIX_LENGTH = REQUEST_URI_PREFIX.length();

    @Context
    private HttpRequest httpRequest;

    private AuthorizationEndpointRequest authorizationRequest;

    public static UriBuilder parUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = OIDCLoginProtocolService.tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "resolveExtension").resolveTemplate("extension", ParRootEndpoint.PROVIDER_ID, false).path(ParRootEndpoint.class, "request");
    }

    public ParEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
    }

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response request() {

        ProfileHelper.requireFeature(Profile.Feature.PAR);

        cors = Cors.add(httpRequest).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        event.event(EventType.PUSHED_AUTHORIZATION_REQUEST);

        checkSsl();
        checkRealm();
        authorizeClient();

        if (httpRequest.getDecodedFormParameters().containsKey(REQUEST_URI_PARAM)) {
            throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, "It is not allowed to include request_uri to PAR.", Response.Status.BAD_REQUEST);
        }

        try {
            authorizationRequest = AuthorizationEndpointRequestParserProcessor.parseRequest(event, session, client, httpRequest.getDecodedFormParameters());
        } catch (Exception e) {
            throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getMessage(), Response.Status.BAD_REQUEST);
        }

        AuthorizationEndpointChecker checker = new AuthorizationEndpointChecker()
                .event(event)
                .client(client)
                .realm(realm)
                .request(authorizationRequest)
                .session(session);

        try {
            checker.checkRedirectUri();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter: redirect_uri", Response.Status.BAD_REQUEST);
        }

        try {
            checker.checkResponseType();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            if (ex.getError().equals(OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE)) {
                throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Unsupported response type", Response.Status.BAD_REQUEST);
            } else {
                ex.throwAsCorsErrorResponseException(cors);
            }
        }

        try {
            checker.checkValidScope();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            // PAR throws this as "invalid_request" error
            throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, ex.getErrorDescription(), Response.Status.BAD_REQUEST);
        }

        try {
            checker.checkInvalidRequestMessage();
            checker.checkOIDCRequest();
            checker.checkOIDCParams();
            checker.checkPKCEParams();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            ex.throwAsCorsErrorResponseException(cors);
        }

        try {
            session.clientPolicy().triggerOnEvent(new PushedAuthorizationRequestContext(authorizationRequest, httpRequest.getDecodedFormParameters()));
        } catch (ClientPolicyException cpe) {
            throw throwErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        Map<String, String> params = new HashMap<>();

        UUID key = UUID.randomUUID();
        String requestUri = REQUEST_URI_PREFIX + key.toString();

        int expiresIn = realm.getParPolicy().getRequestUriLifespan();

        httpRequest.getDecodedFormParameters().forEach((k, v) -> {
                // PAR store only accepts Map so that MultivaluedMap needs to be converted to Map.
                String singleValue = String.valueOf(v).replace("[", "").replace("]", "");
                params.put(k, singleValue);
            });
        params.put(PAR_CREATED_TIME, String.valueOf(System.currentTimeMillis()));

        PushedAuthzRequestStoreProvider parStore = session.getProvider(PushedAuthzRequestStoreProvider.class);
        parStore.put(key, expiresIn, params);

        ParResponse parResponse = new ParResponse(requestUri, expiresIn);

        session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
        return cors.builder(Response.status(Response.Status.CREATED)
                       .entity(parResponse)
                       .type(MediaType.APPLICATION_JSON_TYPE))
                       .build();
    }

}