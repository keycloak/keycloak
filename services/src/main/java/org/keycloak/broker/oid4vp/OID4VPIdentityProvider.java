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
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.TrustMaterialIdentityProvider;
import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.broker.provider.TrustMaterialSdJwtIssuerResolver;
import org.keycloak.broker.trust.TrustKeyUtil;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sdjwt.vp.TrustedSdJwtIssuerResolver;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Identity provider that authenticates users with an OpenID4VP (OID4VP) wallet presentation.
 *
 * <p>Supports the same device flow with a single SD-JWT VC. {@link #performLogin} renders a page with
 * an {@code openid4vp://} link, the wallet fetches a signed request object and posts the presentation
 * back to {@link OID4VPIdentityProviderEndpoint}, which verifies it and drives the normal broker
 * machinery (existing or new user, first and post broker login).
 *
 * <p>The provider also acts as its own {@link TrustMaterialIdentityProvider}: the credential issuer
 * signature is verified against the inline JWKS configured on this provider.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html">OID4VP 1.0</a>
 */
public class OID4VPIdentityProvider extends AbstractIdentityProvider<OID4VPIdentityProviderConfig>
        implements TrustMaterialIdentityProvider<OID4VPIdentityProviderConfig> {

    private static final Logger logger = Logger.getLogger(OID4VPIdentityProvider.class);

    // The flow object is written when the login page is rendered and read while serving the request
    // object and the presentation. It holds the root authentication session id and tab id needed to
    // recover that session, and the nonce the wallet must echo in the key binding JWT.
    public static final String FLOW_PREFIX = "oid4vp.flow.";
    // The deferred object is written once the presentation is verified and read when the browser
    // returns to complete-auth. It marks that a verified identity, serialized under IDENTITY_NOTE in
    // the authentication session, is waiting to finish the broker login.
    public static final String DEFERRED_PREFIX = "oid4vp.deferred.";
    public static final String IDENTITY_NOTE = "OID4VP_IDENTITY";
    public static final String KEY_ROOT_SESSION_ID = "rootSessionId";
    public static final String KEY_TAB_ID = "tabId";
    public static final String KEY_NONCE = "nonce";

    public OID4VPIdentityProvider(KeycloakSession session, OID4VPIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        // TODO support the cross device flow (QR code and SSE polling).
        try {
            AuthenticationSessionModel authSession = request.getAuthenticationSession();
            // The OID4VP state correlates the request object, the wallet's direct_post and complete-auth.
            String state = UUID.randomUUID().toString();
            String nonce = Base64Url.encode(SecretGenerator.getInstance().randomBytes(32));
            String rootSessionId = authSession.getParentSession() != null
                    ? authSession.getParentSession().getId()
                    : null;

            session.singleUseObjects().put(
                    FLOW_PREFIX + state,
                    loginTimeoutSeconds(),
                    Map.of(
                            KEY_ROOT_SESSION_ID, rootSessionId != null ? rootSessionId : "",
                            KEY_TAB_ID, authSession.getTabId() != null ? authSession.getTabId() : "",
                            KEY_NONCE, nonce));

            URI requestUri = OID4VPIdentityProviderEndpoint
                    .endpointBaseUri(request.getUriInfo().getBaseUriBuilder(), request.getRealm().getName(),
                            getConfig().getAlias())
                    .path(OID4VPIdentityProviderEndpoint.REQUEST_OBJECT_PATH).path(state)
                    .build();
            String walletUrl = buildWalletUrl(clientId(), requestUri);

            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setAttribute("sameDeviceWalletUrl", walletUrl)
                    .createForm("login-oid4vp.ftl");
        } catch (Exception e) {
            logger.errorf(e, "Failed to initiate OID4VP login: %s", e.getMessage());
            throw new IdentityBrokerException("Failed to initiate wallet login", e);
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new OID4VPIdentityProviderEndpoint(session, realm, this, callback, event);
    }

    // TODO support delegating to an external trust material identity provider by alias.
    @Override
    public Stream<JWK> resolveKeys(TrustMaterialRequest request) {
        String jwksJson = getConfig().getTrustedIssuerJwks();
        if (StringUtil.isBlank(jwksJson)) {
            return Stream.empty();
        }
        try {
            JSONWebKeySet jwks = JsonSerialization.readValue(jwksJson, JSONWebKeySet.class);
            return TrustKeyUtil.filterKeys(Arrays.stream(jwks.getKeys()), request);
        } catch (Exception e) {
            logger.warnf("Failed to parse OID4VP trusted issuer JWKS: %s", e.getMessage());
            return Stream.empty();
        }
    }

    // How trusted issuer keys are resolved for a presented credential. The default pins the inline
    // trusted issuer JWKS through this provider's own trust material.
    // TODO add a trust list backed resolver (e.g. ETSI), selected from configuration.
    protected TrustedSdJwtIssuerResolver trustedIssuerResolver() {
        return new TrustMaterialSdJwtIssuerResolver(this);
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return null;
    }

    @Override
    public Response retrieveToken(
            KeycloakSession session, FederatedIdentityModel identity, UserSessionModel userSession, UserModel user) {
        return null;
    }

    // The verifier signs request objects with a realm ES256 key, whose certificate also yields the
    // client identifier. By default the realm's active key is used. Since the verifier certificate
    // must be registered in the wallet ecosystem upfront and may need a lifecycle independent of
    // realm keys, a dedicated key can be pinned by its kid. The pinned key may be passive or even
    // disabled, so it can be reserved for the verifier without serving regular realm signing.
    public KeyWrapper signingKey() {
        RealmModel realm = session.getContext().getRealm();
        String kid = getConfig().getSigningKeyId();
        KeyWrapper key = StringUtil.isNotBlank(kid)
                ? session.keys().getKeyIncludingDisabled(realm, kid, KeyUse.SIG, Algorithm.ES256)
                : session.keys().getActiveKey(realm, KeyUse.SIG, Algorithm.ES256);
        if (key == null) {
            throw new IdentityBrokerException(StringUtil.isNotBlank(kid)
                    ? "No ES256 realm key found for the configured OID4VP signing key id"
                    : "No active ES256 realm signing key found for the OID4VP verifier");
        }
        if (key.getCertificate() == null) {
            throw new IdentityBrokerException("The OID4VP verifier signing key has no certificate");
        }
        return key;
    }

    protected ClientIdentifier clientIdentifier() {
        // TODO make the client identifier prefix configurable.
        return ClientIdentifier.X509_HASH;
    }

    public String clientId() {
        return clientIdentifier().forCertificate(signingKey().getCertificate());
    }

    protected String buildWalletUrl(String clientId, URI requestUri) {
        String scheme = getConfig().getWalletScheme();
        if (!scheme.endsWith("://")) {
            scheme = scheme + "://";
        }
        return scheme + "?" + OAuth2Constants.CLIENT_ID + "=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&request_uri=" + URLEncoder.encode(requestUri.toString(), StandardCharsets.UTF_8);
    }

    protected int loginTimeoutSeconds() {
        return session.getContext().getRealm().getAccessCodeLifespanLogin();
    }
}
