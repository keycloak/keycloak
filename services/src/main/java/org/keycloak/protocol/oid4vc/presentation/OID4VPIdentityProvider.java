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

import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DcqlClaimQuery;
import org.keycloak.protocol.oid4vc.model.presentation.DcqlCredentialMeta;
import org.keycloak.protocol.oid4vc.model.presentation.DcqlCredentialQuery;
import org.keycloak.protocol.oid4vc.model.presentation.DcqlQuery;
import org.keycloak.util.JsonSerialization;

public class OID4VPIdentityProvider extends AbstractIdentityProvider<OID4VPIdentityProviderConfig> {

    static final String REQUEST_HANDLE_PREFIX = "oid4vp.request-handle.";
    static final String STATE_REFERENCE_PREFIX = "oid4vp.state.";
    static final String RESPONSE_CODE_PREFIX = "oid4vp.response-code.";
    static final String ENTRY_JSON = "json";
    static final String CREDENTIAL_QUERY_ID = "credential";

    public OID4VPIdentityProvider(KeycloakSession session, OID4VPIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        int lifespan = getConfig().getRequestObjectLifespan();
        RealmModel realm = request.getRealm();
        String verifierEndpoint = getVerifierEndpoint(realm);
        ClientIdentifier clientIdentifier = resolveClientIdentifier(verifierEndpoint);
        String rootSessionId = request.getAuthenticationSession().getParentSession() != null
                ? request.getAuthenticationSession().getParentSession().getId()
                : null;
        String nonce = UUID.randomUUID().toString();
        OID4VPRequestHandleReference handleReference = new OID4VPRequestHandleReference()
                .setState(request.getState().getEncoded())
                .setNonce(nonce)
                .setClientId(clientIdentifier.getValue())
                .setRootSessionId(rootSessionId)
                .setTabId(request.getAuthenticationSession().getTabId());
        session.singleUseObjects().put(
                STATE_REFERENCE_PREFIX + request.getState().getEncoded(),
                lifespan,
                Map.of(ENTRY_JSON, JsonSerialization.valueAsString(handleReference)));

        URI walletUri = switch (getConfig().getAuthorizationRequestTransport()) {
            case REQUEST_URI -> {
                String requestHandle = UUID.randomUUID().toString();
                URI requestUri = URI.create(verifierEndpoint + "/request-object/" + requestHandle);
                // The request handle must remain dereferenceable for repeated request_uri fetches.
                session.singleUseObjects().put(
                        REQUEST_HANDLE_PREFIX + requestHandle,
                        lifespan,
                        Map.of(ENTRY_JSON, JsonSerialization.valueAsString(handleReference)));
                yield createRequestUriWalletUri(clientIdentifier, requestUri);
            }
            case QUERY_PARAMETERS -> createQueryParameterWalletUri(
                    clientIdentifier.getValue(), request.getState().getEncoded(), verifierEndpoint, nonce);
        };
        return Response.seeOther(walletUri).build();
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new OID4VPEndpoint(session, realm, this, callback);
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return exchangeNotSupported();
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity, UserSessionModel userSession, UserModel user) {
        return exchangeNotSupported();
    }

    AuthorizationRequest createAuthorizationRequest(String clientId, String state, String responseUri, String nonce) {
        int now = Time.currentTime();
        return new AuthorizationRequest()
                .setJti(UUID.randomUUID().toString())
                .setIssuer(clientId)
                .setAudience(OID4VPConstants.AUD_SELF_ISSUED_V2)
                .setIssuedAt((long) now)
                .setExpiration((long) now + getConfig().getRequestObjectLifespan())
                .setClientId(clientId)
                .setResponseType(OID4VPConstants.RESPONSE_TYPE_VP_TOKEN)
                .setResponseMode(OID4VPConstants.RESPONSE_MODE_DIRECT_POST)
                .setResponseUri(responseUri)
                .setState(state)
                .setNonce(nonce)
                .setDcqlQuery(createDcqlQuery())
                .setClientMetadata(createClientMetadata());
    }

    ClientIdentifier resolveClientIdentifier(String responseUri) {
        ClientIdentifierPrefix clientIdentifierPrefix = getConfig().getClientIdentifierPrefix();
        X509Certificate certificate = clientIdentifierPrefix == ClientIdentifierPrefix.X509_HASH
                || clientIdentifierPrefix == ClientIdentifierPrefix.X509_SAN_DNS
                ? RequestObjectSigner.parseCertificate(getConfig().getX509CertificatePem())
                : null;
        return ClientIdentifier.resolve(
                clientIdentifierPrefix,
                responseUri,
                certificate);
    }

    private URI createRequestUriWalletUri(ClientIdentifier clientIdentifier, URI requestUri) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put(OID4VPConstants.CLIENT_ID, clientIdentifier.getValue());
        queryParams.put(OID4VPConstants.REQUEST_URI, requestUri.toString());
        return createWalletUri(queryParams);
    }

    URI createQueryParameterWalletUri(String clientId, String state, String responseUri, String nonce) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put(OID4VPConstants.CLIENT_ID, clientId);
        queryParams.put(OID4VPConstants.RESPONSE_TYPE, OID4VPConstants.RESPONSE_TYPE_VP_TOKEN);
        queryParams.put(OID4VPConstants.RESPONSE_MODE, OID4VPConstants.RESPONSE_MODE_DIRECT_POST);
        queryParams.put(OID4VPConstants.RESPONSE_URI, responseUri);
        queryParams.put(OID4VPConstants.STATE, state);
        queryParams.put(OID4VPConstants.NONCE, nonce);
        queryParams.put(OID4VPConstants.DCQL_QUERY, JsonSerialization.valueAsString(createDcqlQuery()));
        queryParams.put(OID4VPConstants.CLIENT_METADATA, JsonSerialization.valueAsString(createClientMetadata()));
        return createWalletUri(queryParams);
    }

    private URI createWalletUri(Map<String, String> queryParams) {
        KeycloakUriBuilder uri = KeycloakUriBuilder.fromUri(getConfig().getWalletScheme());
        queryParams.forEach(uri::queryParam);
        return uri.build();
    }

    private Map<String, Object> createClientMetadata() {
        // TODO: Build OID4VP client metadata from provider configuration instead of these minimal defaults.
        return Map.of(
                "vp_formats_supported", Map.of(
                        "dc+sd-jwt", Map.of(
                                "sd-jwt_alg_values", List.of("ES256"),
                                "kb-jwt_alg_values", List.of("ES256"))));
    }

    String getVerifierEndpoint(RealmModel realm) {
        return session.getContext().getUri().getBaseUriBuilder()
                .path("realms")
                .path(realm.getName())
                .path("broker")
                .path(getConfig().getAlias())
                .path("endpoint")
                .build()
                .toString();
    }

    String getCredentialQueryId() {
        List<DcqlCredentialQuery> credentialQueries = createDcqlQuery().getCredentials();
        if (credentialQueries == null || credentialQueries.size() != 1) {
            throw new IllegalArgumentException("OID4VP identity provider requires exactly one DCQL credential query");
        }

        String credentialQueryId = credentialQueries.get(0).getId();
        if (credentialQueryId == null || credentialQueryId.isBlank()) {
            throw new IllegalArgumentException("OID4VP identity provider DCQL credential query requires an id");
        }
        return credentialQueryId;
    }

    DcqlQuery createDcqlQuery() {
        String configured = getConfig().getDcqlQuery();
        if (configured != null) {
            try {
                return JsonSerialization.readValue(configured, DcqlQuery.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid DCQL query configuration", e);
            }
        }

        return new DcqlQuery().setCredentials(List.of(
                new DcqlCredentialQuery()
                        .setId(CREDENTIAL_QUERY_ID)
                        .setFormat("dc+sd-jwt")
                        .setMeta(new DcqlCredentialMeta()
                                .setVctValues(List.of("urn:keycloak:oid4vp:credential")))
                        .setClaims(List.of(
                                new DcqlClaimQuery().setPath(List.of(getConfig().getSubjectClaimName())),
                                new DcqlClaimQuery().setPath(List.of("given_name")),
                                new DcqlClaimQuery().setPath(List.of("family_name"))))));
    }

    static String storageKey(String prefix, String value) {
        return prefix + value;
    }

    static class OID4VPRequestHandleReference {

        private String state;
        private String nonce;
        private String clientId;
        private String rootSessionId;
        private String tabId;

        public String getState() {
            return state;
        }

        public OID4VPRequestHandleReference setState(String state) {
            this.state = state;
            return this;
        }

        public String getNonce() {
            return nonce;
        }

        public OID4VPRequestHandleReference setNonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public String getClientId() {
            return clientId;
        }

        public OID4VPRequestHandleReference setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public String getRootSessionId() {
            return rootSessionId;
        }

        public OID4VPRequestHandleReference setRootSessionId(String rootSessionId) {
            this.rootSessionId = rootSessionId;
            return this;
        }

        public String getTabId() {
            return tabId;
        }

        public OID4VPRequestHandleReference setTabId(String tabId) {
            this.tabId = tabId;
            return this;
        }
    }

}
