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

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.OID4VCConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider.AuthenticationCallback;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.consumer.PresentationRequirements;
import org.keycloak.sdjwt.consumer.SdJwtPresentationConsumer;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.sdjwt.vp.SdJwtVpVerificationResult;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * JAX-RS endpoint backing the OID4VP wallet flow, routed at
 * {@code /realms/{realm}/broker/{alias}/endpoint}.
 *
 * <ul>
 *   <li>{@code GET /request-object/{handle}} serves the signed authorization request object.</li>
 *   <li>{@code POST /} receives the wallet's {@code direct_post} presentation, verifies it, and
 *       returns the browser redirect that finalizes login.</li>
 *   <li>{@code GET /complete-auth} resolves the deferred identity and hands control back to Keycloak.</li>
 * </ul>
 *
 * <p>TODO add the cross device flow.
 */
public class OID4VPIdentityProviderEndpoint {

    private static final Logger logger = Logger.getLogger(OID4VPIdentityProviderEndpoint.class);

    private static final int CLOCK_SKEW_SECONDS = 60;
    private static final int KB_JWT_MAX_AGE_SECONDS = 300;

    // Broker-internal endpoint routing, not part of the OID4VP protocol.
    public static final String REQUEST_OBJECT_PATH = "request-object";
    public static final String COMPLETE_AUTH_PATH = "complete-auth";

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OID4VPIdentityProvider provider;
    private final AuthenticationCallback callback;
    private final EventBuilder event;

    public OID4VPIdentityProviderEndpoint(
            KeycloakSession session,
            RealmModel realm,
            OID4VPIdentityProvider provider,
            AuthenticationCallback callback,
            EventBuilder event) {
        this.session = session;
        this.realm = realm;
        this.provider = provider;
        this.callback = callback;
        this.event = event;
    }

    @GET
    @Path("/request-object/{state}")
    public Response requestObject(@PathParam("state") String state) {
        Map<String, String> stored = session.singleUseObjects().get(OID4VPIdentityProvider.CONTEXT_PREFIX + state);
        if (stored == null) {
            return loginError(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST,
                    "Unknown or expired state", Errors.INVALID_REQUEST);
        }
        String requestObject;
        try {
            requestObject = buildRequestObject(state, RequestContext.fromMap(stored))
                    .sign(provider.signingKey());
        } catch (RuntimeException e) {
            logger.errorf(e, "Failed to build the OID4VP request object: %s", e.getMessage());
            return loginError(Response.Status.INTERNAL_SERVER_ERROR, OAuthErrorException.SERVER_ERROR,
                    "Failed to build the request object", Errors.IDENTITY_PROVIDER_ERROR);
        }
        return Response.ok(requestObject).type(OID4VCConstants.REQUEST_OBJECT_MEDIA_TYPE)
                .cacheControl(CacheControlUtil.noCache()).build();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response directPost(@FormParam(OID4VCConstants.VP_TOKEN) String vpToken,
            @FormParam(OAuth2Constants.STATE) String state,
            @FormParam(OAuth2Constants.RESPONSE) String response) {
        RequestContext requestContext;
        if (StringUtil.isNotBlank(response)) {
            // direct_post.jwt, a JWE whose plaintext carries vp_token and state.
            DecryptedResponse decrypted;
            try {
                decrypted = decryptResponse(response);
            } catch (VerificationException e) {
                logger.warnf("OID4VP encrypted response rejected: %s", e.getMessage());
                return loginError(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST,
                        e.getMessage(), Errors.INVALID_REQUEST);
            }
            vpToken = decrypted.vpToken();
            state = decrypted.state();
            requestContext = decrypted.context();
        } else {
            if (StringUtil.isBlank(vpToken) || StringUtil.isBlank(state)) {
                return loginError(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST,
                        "Missing vp_token or state", Errors.INVALID_REQUEST);
            }
            Map<String, String> stored = session.singleUseObjects().get(OID4VPIdentityProvider.CONTEXT_PREFIX + state);
            if (stored == null) {
                return loginError(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST,
                        "Unknown or expired state", Errors.INVALID_REQUEST);
            }
            requestContext = RequestContext.fromMap(stored);
            // The request object promised an encrypted response, so a plain post is a downgrade.
            if (requestContext.isEncrypted()) {
                return loginError(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST,
                        "Expected an encrypted response", Errors.INVALID_REQUEST);
            }
        }

        AuthenticationSessionModel authSession =
                resolveAuthSession(requestContext.rootSessionId(), requestContext.tabId());
        if (authSession == null) {
            return loginError(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST,
                    "Authentication session not found", Errors.INVALID_REQUEST);
        }

        BrokeredIdentityContext context;
        try {
            SdJwtVpVerificationResult result = verifyPresentation(vpToken, requestContext.nonce());
            context = toBrokeredContext(result, authSession);
        } catch (Exception e) {
            logger.warnf("OID4VP presentation rejected: %s", e.getMessage());
            return loginError(Response.Status.BAD_REQUEST, OAuthErrorException.ACCESS_DENIED, e.getMessage(),
                    Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
        }

        // The wallet cannot finish the browser login, so hand the verified identity to the browser.
        // Stash it in the authentication session and key the deferred marker by a fresh response_code
        // rather than reusing the state, which may have leaked through the request_uri. The browser
        // gets the response_code only from this direct_post response (OID4VP session fixation defense).
        String responseCode = UUID.randomUUID().toString();
        SerializedBrokeredIdentityContext.serialize(context)
                .saveToAuthenticationSession(authSession, OID4VPIdentityProvider.IDENTITY_NOTE);
        session.singleUseObjects().put(
                OID4VPIdentityProvider.DEFERRED_PREFIX + responseCode,
                realm.getAccessCodeLifespanLogin(),
                Map.of(
                        OID4VPIdentityProvider.KEY_ROOT_SESSION_ID, requestContext.rootSessionId(),
                        OID4VPIdentityProvider.KEY_TAB_ID, requestContext.tabId()));
        session.singleUseObjects().remove(OID4VPIdentityProvider.CONTEXT_PREFIX + state);

        return Response.ok(Map.of(OAuth2Constants.REDIRECT_URI, completeAuthUrl(responseCode)))
                .type(MediaType.APPLICATION_JSON_TYPE).cacheControl(CacheControlUtil.noCache()).build();
    }

    @GET
    @Path("/complete-auth")
    public Response completeAuth(@QueryParam(OID4VCConstants.RESPONSE_CODE) String responseCode) {
        Map<String, String> deferred =
                session.singleUseObjects().remove(OID4VPIdentityProvider.DEFERRED_PREFIX + responseCode);
        if (deferred == null) {
            return loginErrorPage(null, Messages.SESSION_NOT_ACTIVE);
        }
        AuthenticationSessionModel authSession = resolveAuthSession(
                deferred.get(OID4VPIdentityProvider.KEY_ROOT_SESSION_ID), deferred.get(OID4VPIdentityProvider.KEY_TAB_ID));
        if (authSession == null) {
            return loginErrorPage(null, Messages.SESSION_NOT_ACTIVE);
        }

        // Bind completion to the browser that started the login. Its authentication session cookie must
        // match the session the request handle was issued for, so a leaked handle alone cannot finish it.
        RootAuthenticationSessionModel cookieRootSession =
                new AuthenticationSessionManager(session).getCurrentRootAuthenticationSession(realm);
        if (cookieRootSession == null
                || !cookieRootSession.getId().equals(deferred.get(OID4VPIdentityProvider.KEY_ROOT_SESSION_ID))) {
            return loginErrorPage(authSession, Messages.SESSION_NOT_ACTIVE);
        }

        SerializedBrokeredIdentityContext serialized = SerializedBrokeredIdentityContext.readFromAuthenticationSession(
                authSession, OID4VPIdentityProvider.IDENTITY_NOTE);
        if (serialized == null) {
            return loginErrorPage(authSession, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }
        authSession.removeAuthNote(OID4VPIdentityProvider.IDENTITY_NOTE);

        session.getContext().setAuthenticationSession(authSession);
        session.getContext().setClient(authSession.getClient());

        BrokeredIdentityContext context = serialized.deserialize(session, authSession);
        context.setAuthenticationSession(authSession);

        return callback.authenticated(context);
    }

    protected RequestObject buildRequestObject(String state, RequestContext requestContext) {
        String clientId = provider.clientId();
        Instant now = Instant.now();

        RequestObject requestObject = new RequestObject()
                .clientId(clientId)
                .responseType(OID4VCConstants.VP_TOKEN)
                .responseMode(requestContext.isEncrypted()
                        ? OID4VCConstants.RESPONSE_MODE_DIRECT_POST_JWT
                        : OID4VCConstants.RESPONSE_MODE_DIRECT_POST)
                .responseUri(endpointBaseUrl())
                .nonce(requestContext.nonce())
                .state(state)
                .clientMetadata(requestContext.clientMetadata())
                .dcqlQuery(dcqlQuery());
        requestObject.id(UUID.randomUUID().toString());
        requestObject.iat(now.getEpochSecond());
        requestObject.exp(now.plusSeconds(realm.getAccessCodeLifespanLogin()).getEpochSecond());
        requestObject.issuer(clientId);
        requestObject.audience(OID4VCConstants.SELF_ISSUED_V2);
        return requestObject;
    }

    // TODO infer the DCQL from identity provider mappers.
    protected JsonNode dcqlQuery() {
        String dcql = provider.getConfig().getDcqlQuery();
        if (StringUtil.isBlank(dcql)) {
            throw new IllegalStateException("No DCQL query configured on the OID4VP identity provider");
        }
        try {
            return JsonSerialization.readValue(dcql, JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("The configured OID4VP DCQL query is not valid JSON", e);
        }
    }

    protected DecryptedResponse decryptResponse(String response) throws VerificationException {
        ParsedResponse parsed;
        try {
            parsed = ResponseEncryption.parse(response);
        } catch (JWEException e) {
            throw new VerificationException("Malformed encrypted response", e);
        }
        // The ephemeral key was issued with the state as its kid.
        String state = parsed.keyId();
        if (StringUtil.isBlank(state)) {
            throw new VerificationException("Encrypted response is missing the key id");
        }

        Map<String, String> stored = session.singleUseObjects().get(OID4VPIdentityProvider.CONTEXT_PREFIX + state);
        if (stored == null) {
            throw new VerificationException("Unknown or expired response encryption key");
        }
        RequestContext requestContext = RequestContext.fromMap(stored);
        if (!requestContext.isEncrypted()) {
            // A plain flow never advertised a key, so a JWE naming its state as kid must not resolve one.
            throw new VerificationException("Unknown or expired response encryption key");
        }

        JsonNode payload;
        try {
            String plaintext = ResponseEncryption.decrypt(parsed,
                    requestContext.encryptionKey().privateKey());
            payload = JsonSerialization.readValue(plaintext, JsonNode.class);
        } catch (JWEException | IOException e) {
            throw new VerificationException("Failed to decrypt the response", e);
        }

        JsonNode vpTokenNode = payload.get(OID4VCConstants.VP_TOKEN);
        if (vpTokenNode == null || vpTokenNode.isNull()
                || (vpTokenNode.isTextual() && StringUtil.isBlank(vpTokenNode.asText()))) {
            throw new VerificationException("Encrypted response is missing vp_token");
        }
        // The state sealed inside the ciphertext must match the one this key id was issued for.
        JsonNode stateNode = payload.get(OAuth2Constants.STATE);
        if (stateNode == null || !state.equals(stateNode.asText())) {
            throw new VerificationException("Encrypted response state mismatch");
        }

        String vpToken;
        try {
            vpToken = vpTokenNode.isTextual() ? vpTokenNode.asText()
                    : JsonSerialization.writeValueAsString(vpTokenNode);
        } catch (IOException e) {
            throw new VerificationException("Malformed vp_token in the encrypted response", e);
        }
        return new DecryptedResponse(vpToken, state, requestContext);
    }

    protected SdJwtVpVerificationResult verifyPresentation(String vpToken, String nonce) throws VerificationException {
        SdJwtVP sdJwtVP;
        try {
            sdJwtVP = SdJwtVP.of(extractCredential(vpToken));
        } catch (IllegalArgumentException e) {
            throw new VerificationException("Malformed vp_token presentation", e);
        }

        requireAcceptedAlgorithm(sdJwtVP.getIssuerSignedJWT().getJwsHeader().getRawAlgorithm(), "issuer signed JWT");
        Optional<KeyBindingJWT> keyBindingJwt = sdJwtVP.getKeyBindingJWT();
        if (keyBindingJwt.isPresent()) {
            requireAcceptedAlgorithm(keyBindingJwt.get().getJwsHeader().getRawAlgorithm(), "key binding JWT");
        }

        // Credentials can be arbitrarily old, so the issuer JWT iat is not bounded. Expiry is still
        // enforced via exp. The key binding JWT must instead be fresh.
        IssuerSignedJwtVerificationOpts issuerOpts = IssuerSignedJwtVerificationOpts.builder()
                .withClockSkew(CLOCK_SKEW_SECONDS)
                .withIatCheck(null)
                .withExpCheck(true)
                .withNbfCheck(true)
                .build();
        KeyBindingJwtVerificationOpts kbOpts = KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .withClockSkew(CLOCK_SKEW_SECONDS)
                .withIatCheck(KB_JWT_MAX_AGE_SECONDS)
                .withExpCheck(true)
                .withNbfCheck(true)
                .withAudCheck(provider.clientId())
                .withNonceCheck(nonce)
                .build();

        // TODO bind the presentation to the requested credential type and claims, so that not any
        // credential from a trusted issuer satisfies the login.
        PresentationRequirements noAdditionalRequirements = disclosedPayload -> {
        };
        TrustedSdJwtIssuer trustedIssuer = provider.trustedIssuerResolver().resolve(sdJwtVP.getIssuerSignedJWT());
        return new SdJwtPresentationConsumer().verifySdJwtPresentation(
                sdJwtVP, noAdditionalRequirements, List.of(trustedIssuer), issuerOpts, kbOpts);
    }

    private static void requireAcceptedAlgorithm(String algorithm, String jwt) throws VerificationException {
        if (!OID4VPIdentityProvider.ACCEPTED_ALGORITHMS.contains(algorithm)) {
            throw new VerificationException("Unsupported " + jwt + " signature algorithm " + algorithm);
        }
    }

    // The DCQL vp_token is a JSON object keyed by credential id whose value is the presentation, or an
    // array of them. A bare SD-JWT compact string is not valid JSON and is used as is.
    // TODO support multiple credentials (mapped by credential id set in the DCQL).
    static String extractCredential(String vpToken) throws VerificationException {
        JsonNode token;
        try {
            token = JsonSerialization.readValue(vpToken, JsonNode.class);
        } catch (IOException notJson) {
            return vpToken.trim();
        }

        JsonNode presentation = token;
        if (token.isObject()) {
            if (token.size() != 1) {
                throw new VerificationException("vp_token must carry exactly one credential");
            }
            presentation = token.elements().next();
        }
        if (presentation.isArray()) {
            if (presentation.size() != 1) {
                throw new VerificationException("vp_token must carry exactly one credential");
            }
            presentation = presentation.get(0);
        }
        if (!presentation.isTextual()) {
            throw new VerificationException("vp_token does not contain a credential presentation");
        }
        return presentation.textValue();
    }

    protected BrokeredIdentityContext toBrokeredContext(SdJwtVpVerificationResult result, AuthenticationSessionModel authSession) {
        JsonNode claims = result.getClaims();
        JsonNode principal = claims.get(provider.getConfig().getPrincipalAttribute());
        if (principal == null || principal.isNull() || !principal.isValueNode()
                || StringUtil.isBlank(principal.asText())) {
            throw new IllegalStateException(
                    "Credential does not contain a usable value for the configured principal attribute '"
                            + provider.getConfig().getPrincipalAttribute() + "'");
        }
        String subject = principal.asText();

        BrokeredIdentityContext context = new BrokeredIdentityContext(subject, provider.getConfig());
        context.setIdp(provider);
        context.setUsername(subject);
        context.setModelUsername(subject);
        context.setBrokerUserId(provider.getConfig().getAlias() + "." + subject);
        context.setAuthenticationSession(authSession);

        JsonNode email = claims.get(IDToken.EMAIL);
        if (email != null && email.isValueNode()) {
            context.setEmail(email.asText());
        }

        // Expose disclosed claims so identity provider attribute mappers can consume them.
        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(context, claims, provider.getConfig().getAlias());
        return context;
    }

    protected AuthenticationSessionModel resolveAuthSession(String rootSessionId, String tabId) {
        if (StringUtil.isBlank(rootSessionId) || StringUtil.isBlank(tabId)) {
            return null;
        }
        RootAuthenticationSessionModel root = session.authenticationSessions().getRootAuthenticationSession(realm, rootSessionId);
        return root == null ? null : root.getAuthenticationSessions().get(tabId);
    }

    // The broker endpoint base, shared with the provider that builds the request_uri.
    public static UriBuilder endpointBaseUri(UriBuilder baseUriBuilder, String realmName, String alias) {
        return baseUriBuilder.path("realms").path(realmName).path("broker").path(alias).path("endpoint");
    }

    protected String endpointBaseUrl() {
        return endpointBaseUri(session.getContext().getUri().getBaseUriBuilder(), realm.getName(),
                provider.getConfig().getAlias()).build().toString();
    }

    protected String completeAuthUrl(String responseCode) {
        return endpointBaseUri(session.getContext().getUri().getBaseUriBuilder(), realm.getName(),
                provider.getConfig().getAlias())
                .path(COMPLETE_AUTH_PATH)
                .queryParam(OID4VCConstants.RESPONSE_CODE, responseCode)
                .build().toString();
    }

    // Wallet facing failures, fired as a broker login error and returned as an OAuth error response.
    protected Response loginError(Response.Status status, String error, String description, String eventError) {
        event.event(EventType.IDENTITY_PROVIDER_LOGIN).detail(Details.REASON, description).error(eventError);
        return errorResponse(status, error, description);
    }

    // Browser facing failures during complete-auth, fired as a broker login error and rendered as a
    // localized error page from the given message bundle key.
    protected Response loginErrorPage(AuthenticationSessionModel authSession, String messageKey) {
        event.event(EventType.IDENTITY_PROVIDER_LOGIN).detail(Details.REASON, messageKey)
                .error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
        return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, messageKey);
    }

    protected static Response errorResponse(Response.Status status, String error, String description) {
        return Response.status(status)
                .entity(new OAuth2ErrorRepresentation(error, description))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .cacheControl(CacheControlUtil.noCache())
                .build();
    }
}
