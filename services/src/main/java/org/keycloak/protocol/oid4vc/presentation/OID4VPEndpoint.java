/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.presentation;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider.AuthenticationCallback;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DirectPostRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DirectPostResponse;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

public class OID4VPEndpoint {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OID4VPIdentityProvider provider;
    private final AuthenticationCallback callback;

    public OID4VPEndpoint(KeycloakSession session, RealmModel realm, OID4VPIdentityProvider provider, AuthenticationCallback callback) {
        this.session = session;
        this.realm = realm;
        this.provider = provider;
        this.callback = callback;
    }

    @GET
    @Path("/request-object/{request_handle}")
    public Response getRequestObject(@PathParam("request_handle") String requestHandle) {
        OID4VPIdentityProvider.OID4VPRequestHandleReference handleReference = readStored(
                OID4VPIdentityProvider.storageKey(OID4VPIdentityProvider.REQUEST_HANDLE_PREFIX, requestHandle),
                OID4VPIdentityProvider.OID4VPRequestHandleReference.class);
        if (handleReference == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        AuthenticationSessionModel authSession = resolveAuthenticationSession(handleReference);
        if (authSession == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        session.getContext().setAuthenticationSession(authSession);

        String responseUri = provider.getVerifierEndpoint(realm);

        AuthorizationRequest authorizationRequest = provider.createAuthorizationRequest(realm, responseUri, handleReference.getState(), responseUri);
        String requestObject = new JWSBuilder()
                .type(OID4VPConstants.REQUEST_OBJECT_TYPE)
                .jsonContent(authorizationRequest)
                .sign(getSignatureSigner());
        return Response.ok(requestObject).type(OID4VPConstants.MEDIA_TYPE_AUTHORIZATION_REQUEST_JWT).build();
    }

    @POST
    public Response directPost(@BeanParam DirectPostRequest directPostRequest) {
        if (!directPostRequest.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        OID4VPIdentityProvider.OID4VPRequestHandleReference stateReference = removeStored(
                OID4VPIdentityProvider.storageKey(OID4VPIdentityProvider.STATE_REFERENCE_PREFIX, directPostRequest.getState()),
                OID4VPIdentityProvider.OID4VPRequestHandleReference.class);
        AuthenticationSessionModel authSession = resolveAuthenticationSession(stateReference);
        if (authSession == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        session.getContext().setAuthenticationSession(authSession);

        // TODO: Validate and extract claims from the presented VP token instead of accepting any well-formed direct_post payload.
        int lifespan = provider.getConfig().getRequestObjectLifespan();
        String responseCode = UUID.randomUUID().toString();
        session.singleUseObjects().put(
                OID4VPIdentityProvider.storageKey(OID4VPIdentityProvider.RESPONSE_CODE_PREFIX, responseCode),
                lifespan,
                Map.of(OID4VPIdentityProvider.ENTRY_JSON, JsonSerialization.valueAsString(
                        new OID4VPDirectPostResult()
                                .setState(directPostRequest.getState())
                                .setAuthorizationResponse(directPostRequest))));

        URI redirectUri = session.getContext().getUri().getBaseUriBuilder()
                .path("realms")
                .path(realm.getName())
                .path("broker")
                .path(provider.getConfig().getAlias())
                .path("endpoint")
                .path("continue")
                .queryParam(OID4VPConstants.STATE, directPostRequest.getState())
                .queryParam(OID4VPConstants.RESPONSE_CODE, responseCode)
                .build();

        return Response.ok(new DirectPostResponse().setRedirectUri(redirectUri.toString()))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path("/continue")
    public Response continueAuthentication(@QueryParam(OID4VPConstants.STATE) String state,
                                           @QueryParam(OID4VPConstants.RESPONSE_CODE) String responseCode) {
        if (state == null || responseCode == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        AuthenticationSessionModel authSession = callback.getAndVerifyAuthenticationSession(state);
        session.getContext().setAuthenticationSession(authSession);

        OID4VPDirectPostResult result = removeStored(
                OID4VPIdentityProvider.storageKey(OID4VPIdentityProvider.RESPONSE_CODE_PREFIX, responseCode),
                OID4VPDirectPostResult.class);
        if (result == null) {
            return callback.error(provider.getConfig(), "Missing direct_post result");
        }
        if (!state.equals(result.getState())) {
            return callback.error(provider.getConfig(), "Mismatched direct_post result");
        }

        DirectPostRequest authorizationResponse = result.getAuthorizationResponse();
        if (authorizationResponse == null) {
            return callback.error(provider.getConfig(), "Missing direct_post authorization response");
        }

        if (authorizationResponse.isErrorResponse()) {
            String message = Optional.ofNullable(authorizationResponse.getErrorDescription()).orElse(authorizationResponse.getError());
            return callback.error(provider.getConfig(), message);
        }

        // TODO: Replace this static brokered identity with data derived from a validated VP token.
        String uniqueSuffix = authSession.getTabId() != null ? authSession.getTabId() : Integer.toString(Time.currentTime());
        BrokeredIdentityContext identity = new BrokeredIdentityContext("dummy-subject", provider.getConfig());
        identity.setIdp(provider);
        identity.setAuthenticationSession(authSession);
        identity.setUsername("oid4vp-" + uniqueSuffix);
        identity.setModelUsername("oid4vp-" + uniqueSuffix);
        identity.setEmail("oid4vp-" + uniqueSuffix + "@example.org");
        identity.setFirstName("OID4VP");
        identity.setLastName("User");
        return callback.authenticated(identity);
    }

    private SignatureSignerContext getSignatureSigner() {
        String algorithm = realm.getDefaultSignatureAlgorithm();
        if (algorithm == null || algorithm.isBlank()) {
            algorithm = Constants.DEFAULT_SIGNATURE_ALGORITHM;
        }

        KeyWrapper signingKey = session.keys().getActiveKey(realm, KeyUse.SIG, algorithm);
        if (signingKey == null) {
            throw new IllegalStateException("No active realm signing key available for OID4VP request objects using algorithm " + algorithm);
        }

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, algorithm);
        if (signatureProvider == null) {
            throw new IllegalStateException("No signature provider available for OID4VP request object algorithm " + algorithm);
        }

        SignatureSignerContext signer = signatureProvider.signer(signingKey);
        if (signer == null) {
            throw new IllegalStateException("No signer available for OID4VP request object algorithm " + algorithm);
        }

        return signer;
    }

    private <T> T readStored(String key, Class<T> type) {
        return Optional.ofNullable(session.singleUseObjects().get(key))
                .map(notes -> notes.get(OID4VPIdentityProvider.ENTRY_JSON))
                .map(json -> JsonSerialization.valueFromString(json, type))
                .orElse(null);
    }

    private <T> T removeStored(String key, Class<T> type) {
        return Optional.ofNullable(session.singleUseObjects().remove(key))
                .map(notes -> notes.get(OID4VPIdentityProvider.ENTRY_JSON))
                .map(json -> JsonSerialization.valueFromString(json, type))
                .orElse(null);
    }

    private AuthenticationSessionModel resolveAuthenticationSession(OID4VPIdentityProvider.OID4VPRequestHandleReference reference) {
        if (reference == null || reference.getRootSessionId() == null || reference.getTabId() == null) {
            return null;
        }

        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions()
                .getRootAuthenticationSession(realm, reference.getRootSessionId());
        if (rootAuthSession == null) {
            return null;
        }

        return rootAuthSession.getAuthenticationSessions().get(reference.getTabId());
    }

    static class OID4VPDirectPostResult {

        private String state;
        private DirectPostRequest authorizationResponse;

        public String getState() {
            return state;
        }

        public OID4VPDirectPostResult setState(String state) {
            this.state = state;
            return this;
        }

        public DirectPostRequest getAuthorizationResponse() {
            return authorizationResponse;
        }

        public OID4VPDirectPostResult setAuthorizationResponse(DirectPostRequest authorizationResponse) {
            this.authorizationResponse = authorizationResponse;
            return this;
        }
    }
}
