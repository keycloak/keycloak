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
package org.keycloak.broker.oid4vp;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vp.OID4VPConstants;
import org.keycloak.protocol.oid4vp.model.OID4VPAuthorizationRequest;
import org.keycloak.protocol.oid4vp.model.OID4VPDcqlClaimQuery;
import org.keycloak.protocol.oid4vp.model.OID4VPDcqlCredentialMeta;
import org.keycloak.protocol.oid4vp.model.OID4VPDcqlCredentialQuery;
import org.keycloak.protocol.oid4vp.model.OID4VPDcqlQuery;
import org.keycloak.services.Urls;
import org.keycloak.util.JsonSerialization;

public class OID4VPIdentityProvider extends AbstractIdentityProvider<OID4VPIdentityProviderConfig> {

    static final String REQUEST_HANDLE_PREFIX = "oid4vp.request-handle.";
    static final String STATE_REFERENCE_PREFIX = "oid4vp.state.";
    static final String RESPONSE_CODE_PREFIX = "oid4vp.response-code.";
    static final String ENTRY_JSON = "json";

    public OID4VPIdentityProvider(KeycloakSession session, OID4VPIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        String requestHandle = UUID.randomUUID().toString();
        int lifespan = getConfig().getRequestObjectLifespan();
        String rootSessionId = request.getAuthenticationSession().getParentSession() != null
                ? request.getAuthenticationSession().getParentSession().getId()
                : null;
        OID4VPRequestHandleReference handleReference = new OID4VPRequestHandleReference()
                .setState(request.getState().getEncoded())
                .setRootSessionId(rootSessionId)
                .setTabId(request.getAuthenticationSession().getTabId());
        // The request handle must remain dereferenceable for repeated request_uri fetches,
        // while the state entry is used later to correlate and consume the direct_post callback.
        session.singleUseObjects().put(
                REQUEST_HANDLE_PREFIX + requestHandle,
                lifespan,
                Map.of(ENTRY_JSON, JsonSerialization.valueAsString(handleReference)));
        session.singleUseObjects().put(
                STATE_REFERENCE_PREFIX + request.getState().getEncoded(),
                lifespan,
                Map.of(ENTRY_JSON, JsonSerialization.valueAsString(handleReference)));

        String verifierEndpoint = getVerifierEndpoint(request.getRealm());
        URI requestUri = URI.create(verifierEndpoint + "/request-object/" + requestHandle);
        String clientId = urlEncode(verifierEndpoint);
        String encodedRequestUri = urlEncode(requestUri.toString());
        URI walletUri = URI.create(getConfig().getWalletScheme()
                + "?client_id=" + clientId
                + "&request_uri=" + encodedRequestUri);
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

    OID4VPAuthorizationRequest createAuthorizationRequest(RealmModel realm, String clientId, String state, String responseUri) {
        int now = Time.currentTime();
        return new OID4VPAuthorizationRequest()
                .setJti(UUID.randomUUID().toString())
                .setIssuer(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))
                .setAudience(OID4VPConstants.AUD_SELF_ISSUED_V2)
                .setIssuedAt((long) now)
                .setExpiration((long) now + getConfig().getRequestObjectLifespan())
                .setClientId(clientId)
                .setResponseType(OID4VPConstants.RESPONSE_TYPE_VP_TOKEN)
                .setResponseMode(OID4VPConstants.RESPONSE_MODE_DIRECT_POST)
                .setResponseUri(responseUri)
                .setState(state)
                .setNonce(UUID.randomUUID().toString())
                .setDcqlQuery(createStaticDcqlQuery());
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

    private OID4VPDcqlQuery createStaticDcqlQuery() {
        // TODO: Replace this hardcoded DCQL query with configurable verifier policy input.
        return new OID4VPDcqlQuery().setCredentials(List.of(
                new OID4VPDcqlCredentialQuery()
                        .setId("keycloak-oid4vp-credential")
                        .setFormat("dc+sd-jwt")
                        .setMeta(new OID4VPDcqlCredentialMeta()
                                .setVctValues(List.of("urn:keycloak:oid4vp:credential")))
                        .setClaims(List.of(
                                new OID4VPDcqlClaimQuery().setPath(List.of("sub")),
                                new OID4VPDcqlClaimQuery().setPath(List.of("given_name")),
                                new OID4VPDcqlClaimQuery().setPath(List.of("family_name"))))));
    }

    static String storageKey(String prefix, String value) {
        return prefix + value;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static class OID4VPRequestHandleReference {

        private String state;
        private String rootSessionId;
        private String tabId;

        public String getState() {
            return state;
        }

        public OID4VPRequestHandleReference setState(String state) {
            this.state = state;
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
