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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointChecker;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.par.ParResponse;
import org.keycloak.protocol.oidc.par.clientpolicy.context.PushedAuthorizationRequestContext;
import org.keycloak.protocol.oidc.par.endpoints.request.ParEndpointRequestParserProcessor;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.utils.ProfileHelper;

import static org.keycloak.protocol.oidc.OIDCLoginProtocol.REQUEST_URI_PARAM;

/**
 * Pushed Authorization Request endpoint
 */
public class ParEndpoint extends AbstractParEndpoint {

    public static final String PAR_CREATED_TIME = "par.created.time";
    public static final String PAR_DPOP_PROOF_JKT = "par.dpop.proof.jkt";
    private static final String REQUEST_URI_PREFIX = "urn:ietf:params:oauth:request_uri:";
    public static final int REQUEST_URI_PREFIX_LENGTH = REQUEST_URI_PREFIX.length();

    private final HttpRequest httpRequest;

    private AuthorizationEndpointRequest authorizationRequest;

    public static UriBuilder parUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = OIDCLoginProtocolService.tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "resolveExtension").resolveTemplate("extension", ParRootEndpoint.PROVIDER_ID, false).path(ParRootEndpoint.class, "request");
    }

    public ParEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
    this.httpRequest = session.getContext().getHttpRequest();
    }

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response request() {

        ProfileHelper.requireFeature(Profile.Feature.PAR);

        cors = Cors.builder().auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        event.event(EventType.PUSHED_AUTHORIZATION_REQUEST);

        checkSsl();
        checkRealm();
        authorizeClient();

        MultivaluedMap<String, String> decodedFormParameters = httpRequest.getDecodedFormParameters();

        if (decodedFormParameters.containsKey(REQUEST_URI_PARAM)) {
            throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, "It is not allowed to include request_uri to PAR.", Response.Status.BAD_REQUEST);
        }

        // https://datatracker.ietf.org/doc/html/rfc9449#section-10.1
        DPoPUtil.handleDPoPHeader(session, event, cors, null);

        try {
            authorizationRequest = ParEndpointRequestParserProcessor.parseRequest(event, session, client, decodedFormParameters);
        } catch (Exception e) {
            if (!decodedFormParameters.containsKey(OIDCLoginProtocol.REQUEST_PARAM)) {
                throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getMessage(), Response.Status.BAD_REQUEST);
            }
            throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST_OBJECT, e.getMessage(), Response.Status.BAD_REQUEST);
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
            checker.checkParDPoPParams();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            ex.throwAsCorsErrorResponseException(cors);
        }

        try {
            session.clientPolicy().triggerOnEvent(new PushedAuthorizationRequestContext(authorizationRequest, decodedFormParameters));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw throwErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        Map<String, String> params = new HashMap<>();

        String key = UUID.randomUUID().toString();
        String requestUri = REQUEST_URI_PREFIX + key;

        int expiresIn = realm.getParPolicy().getRequestUriLifespan();

        flattenDecodedFormParametersToParamsMap(decodedFormParameters, params);

        params.put(PAR_CREATED_TIME, String.valueOf(System.currentTimeMillis()));
        // If DPoP Proof exists, its public key needs to be matched with the one with Token Request afterward
        DPoP dpop = session.getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE, DPoP.class);
        if (dpop != null) {
            params.put(PAR_DPOP_PROOF_JKT, dpop.getThumbprint());
        }

        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        singleUseStore.put(key, expiresIn, params);

        ParResponse parResponse = new ParResponse(requestUri, expiresIn);

        session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
        return cors.add(Response.status(Response.Status.CREATED)
                .entity(parResponse)
                .type(MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Flattens the given decodedFormParameters MultivaluedMap to a plain Map.
     * Rationale: The SingleUseObjectProvider used as store for PARs only accepts Map so that MultivaluedMap needs to be converted to Map.
     * @param decodedFormParameters form parameters sent in request body
     * @param params target parameter Map
     */
    public static void flattenDecodedFormParametersToParamsMap(
            MultivaluedMap<String, String> decodedFormParameters,
            Map<String, String> params) {

        for (var parameterEntry : decodedFormParameters.entrySet()) {
            String parameterName = parameterEntry.getKey();
            List<String> parameterValues = parameterEntry.getValue();

            if (parameterValues.isEmpty()) {
                // We emit the empty parameter as a marker, but only if it does not exist yet. This prevents "accidental" value overrides.
                params.putIfAbsent(parameterName, null);
            } else {
                // We flatten the MultivaluedMap values list by emitting the first value only.
                // We override potential empty parameters that were added to the params map before.
                params.put(parameterName, parameterValues.get(0));
            }

        }
    }

}
