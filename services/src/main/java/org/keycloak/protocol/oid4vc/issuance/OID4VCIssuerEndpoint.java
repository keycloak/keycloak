/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance;

import java.io.IOException;
import java.security.PublicKey;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OID4VCConstants;
import org.keycloak.VCFormat;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilderFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.ProofValidator;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSigner;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.model.AttestationProof;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialRequestEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryption;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.JwtProof;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.OfferResponseType;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.utils.ClaimsPathPointer;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeModelUtils;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantType;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.saml.processing.api.util.DeflateUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.QRCodeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.zxing.WriterException;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.jboss.logging.Logger;

import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.getSupportedCredentials;
import static org.keycloak.protocol.oid4vc.model.AuthorizationCodeGrant.AUTH_CODE_GRANT_TYPE;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_NONCE;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_PROOF;
import static org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE;

/**
 * Provides the (REST-)endpoints required for the OID4VCI protocol.
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerEndpoint {

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerEndpoint.class);

    /**
     * AccessToken claim attribute for storing a potential credentials offer id
     */
    public static final String CREDENTIALS_OFFER_ID_ATTR = "CREDENTIALS_OFFER_ID";

    /**
     * The maximum QR Code dimension
     */
    public static final Integer MAX_QR_CODE_DIMENSION = 800;

    private Cors cors;

    /**
     * Cached authentication result to prevent DPoP proof reuse.
     * <p>
     * This cache ensures that when authentication is performed multiple times during
     * a single request processing (e.g., when issuing multiple credentials), the same
     * authentication result is reused instead of re-authenticating, which would allow
     * the same DPoP proof to be used multiple times.
     */
    private AuthenticationManager.AuthResult cachedAuthResult;

    public static final String CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY = "credentialOfferLifespanS";
    public static final int DEFAULT_CREDENTIAL_OFFER_LIFESPAN_S = 30;

    public static final String DEFLATE_COMPRESSION = "DEF";
    public static final String NONCE_PATH = "nonce";
    public static final String CREDENTIAL_PATH = "credential";
    public static final String CREDENTIAL_OFFER_PATH = "credential-offer";
    public static final String CREATE_CREDENTIAL_OFFER_PATH = "create-credential-offer";
    public static final String RESPONSE_TYPE_IMG_PNG = OID4VCConstants.RESPONSE_TYPE_IMG_PNG;
    public static final String CREDENTIAL_OFFER_URI_CODE_SCOPE = OID4VCConstants.CREDENTIAL_OFFER_URI_CODE_SCOPE;
    private final KeycloakSession session;
    private final AppAuthManager.BearerTokenAuthenticator bearerTokenAuthenticator;
    private final TimeProvider timeProvider;

    // lifespan of credential offers in seconds
    private final int credentialOfferLifespan;

    /**
     * Credential builders are responsible for initiating the production of
     * credentials in a specific format. Their output is an appropriate credential
     * representation to be signed by a credential signer of the same format.
     * <p></p>
     * Due to technical constraints, we explicitly load credential builders into
     * this map for they are configurable components. The key of the map is the
     * credential {@link VCFormat} associated with the builder. The matching credential
     * signer is directly loaded from the Keycloak container.
     */
    private final Map<String, CredentialBuilder> credentialBuilders;

    public OID4VCIssuerEndpoint(KeycloakSession session,
                                Map<String, CredentialBuilder> credentialBuilders,
                                AppAuthManager.BearerTokenAuthenticator authenticator,
                                TimeProvider timeProvider,
                                int credentialOfferLifespan) {
        this.session = session;
        this.bearerTokenAuthenticator = authenticator;
        this.timeProvider = timeProvider;
        this.credentialBuilders = credentialBuilders;
        this.credentialOfferLifespan = credentialOfferLifespan;
    }

    public OID4VCIssuerEndpoint(KeycloakSession keycloakSession) {
        this.session = keycloakSession;
        this.bearerTokenAuthenticator = new AppAuthManager.BearerTokenAuthenticator(keycloakSession);
        this.timeProvider = new OffsetTimeProvider();

        this.credentialBuilders = loadCredentialBuilders(session);

        RealmModel realm = keycloakSession.getContext().getRealm();
        this.credentialOfferLifespan = Optional.ofNullable(realm.getAttribute(CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY))
                .map(Integer::valueOf)
                .orElse(DEFAULT_CREDENTIAL_OFFER_LIFESPAN_S);
    }

    /**
     * Create credential builders from configured component models in Keycloak.
     *
     * @return a map of the created credential builders with their supported formats as keys.
     */
    private Map<String, CredentialBuilder> loadCredentialBuilders(KeycloakSession keycloakSession) {
        KeycloakSessionFactory keycloakSessionFactory = keycloakSession.getKeycloakSessionFactory();
        return keycloakSessionFactory.getProviderFactoriesStream(CredentialBuilder.class)
                .map(factory -> (CredentialBuilderFactory) factory)
                .map(factory -> factory.create(keycloakSession, null))
                .collect(Collectors.toMap(CredentialBuilder::getSupportedFormat,
                        credentialBuilder ->  credentialBuilder));
    }

    /**
     * Validates whether OID4VCI functionality is enabled for the realm.
     * If disabled, logs the status, optionally records an event error, and throws
     * a {@link CorsErrorResponseException}.
     */
    private void checkIsOid4vciEnabled(EventBuilder eventBuilder) {
        RealmModel realm = session.getContext().getRealm();
        if (!realm.isVerifiableCredentialsEnabled()) {
            LOGGER.debugf("OID4VCI functionality is disabled for realm '%s'. Verifiable Credentials switch is off.", realm.getName());
            if (eventBuilder != null) {
                eventBuilder.error(ErrorType.INVALID_CLIENT.getValue());
            }
            if (cors == null) {
                configureCors(false);
            }
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_CLIENT.getValue(),
                    "OID4VCI functionality is disabled for this realm",
                    Response.Status.FORBIDDEN
            );
        }
    }

    /**
     * Validates if the REST credential offer feature is enabled.
     * If disabled, logs the status and throws
     * a {@link CorsErrorResponseException}.
     */
    private void checkRestCredentialOfferEnabled(EventBuilder eventBuilder) {
        if (!Profile.isFeatureEnabled(org.keycloak.common.Profile.Feature.OID4VC_VCI_REST_CREDENTIAL_OFFER)) {
            LOGGER.debugf("REST credential offer endpoint is disabled. Feature oid4vci-rest-credential-offer is not enabled.");
            if (eventBuilder != null) {
                eventBuilder.error(ErrorType.INVALID_CLIENT.getValue());
            }
            if (cors == null) {
                configureCors(false);
            }
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_CLIENT.getValue(),
                    "REST credential offer functionality is not enabled",
                    Response.Status.FORBIDDEN
            );
        }
    }

    /**
     * Validates whether the authenticated client is enabled for OID4VCI features.
     * <p>
     * If the client is not enabled, this method logs the status and throws a
     * {@link CorsErrorResponseException} with an appropriate error message.
     * </p>
     *
     * @throws CorsErrorResponseException if the client is not enabled for OID4VCI.
     */
    private void checkClientEnabled(EventBuilder eventBuilder) {
        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        ClientModel client = clientSession.getClient();

        boolean oid4vciEnabled = Boolean.parseBoolean(client.getAttributes().get(OID4VCI_ENABLED_ATTRIBUTE_KEY));
        boolean clientEnabled = client.isEnabled();
        String errorDescription = null;

        if (!oid4vciEnabled) {
            LOGGER.debugf("Client '%s' is not enabled for OID4VCI features.", client.getClientId());
            errorDescription = "Client not enabled for OID4VCI";
        }
        if (!clientEnabled) {
            LOGGER.debugf("Client '%s' disabled.", client.getClientId());
            errorDescription = "Client not enabled for OID4VCI";
        }
        if (!oid4vciEnabled || !clientEnabled) {
            if (eventBuilder != null) {
                eventBuilder.client(client).error(ErrorType.INVALID_CLIENT.getValue());
            }
            if (cors == null) {
                configureCors(false);
            }
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_CLIENT.getValue(),
                    errorDescription,
                    Response.Status.FORBIDDEN
            );
        }

        LOGGER.debugf("Client '%s' is enabled for OID4VCI features.", client.getClientId());
    }

    /**
     * the OpenId4VCI nonce-endpoint
     *
     * @return a short-lived c_nonce value that must be presented in key-bound proofs at the credential endpoint.
     * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-16.html#name-nonce-endpoint">Nonce endpoint</a>
     * @see <a href="https://datatracker.ietf.org/doc/html/draft-demarco-nonce-endpoint#name-nonce-response">Nonce response</a>
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Path(NONCE_PATH)
    public Response getCNonce() {
        RealmModel realm = session.getContext().getRealm();
        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_NONCE_REQUEST);

        checkIsOid4vciEnabled(eventBuilder);

        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
        NonceResponse nonceResponse = new NonceResponse();
        String sourceEndpoint = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
        String audience = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());

        // Generate c_nonce for the response body
        String bodyCNonce = cNonceHandler.buildCNonce(List.of(audience), Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, sourceEndpoint));

        // Generate separate DPoP nonce for the header
        String headerDPoPNonce = cNonceHandler.buildCNonce(List.of(audience), Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, sourceEndpoint));

        nonceResponse.setNonce(bodyCNonce);

        eventBuilder.success();

        // RFC7231 recommends Date on origin server responses; OID4VCI conformance checks this explicitly.
        Response.ResponseBuilder responseBuilder = Response.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                .entity(nonceResponse);

        if (headerDPoPNonce != null) {
            responseBuilder.header(OAuth2Constants.DPOP_NONCE_HEADER, headerDPoPNonce);
        }

        return responseBuilder.build();
    }

    /**
     * Handles CORS preflight requests for the /create-credential-offer endpoint.
     * Preflight requests return CORS headers for all origins (standard CORS behavior).
     * The actual request will validate origins against client configuration.
     */
    @OPTIONS
    @Path(CREATE_CREDENTIAL_OFFER_PATH)
    public Response createCredentialOfferPreflight() {
        configureCors(true);
        cors.preflight();
        return cors.add(Response.ok());
    }

    /**
     * Creates an authorization code grant offer that is bound to the calling user.
     */
    public Response createCredentialOffer(String credConfigId) {
        return createCredentialOffer(credConfigId, false, null);
    }

    /**
     * Creates a Credential Offer that is bound to a specific user.
     */
    public Response createCredentialOffer(String credConfigId, boolean preAuthorized, String targetUser) {
        return createCredentialOffer(credConfigId, preAuthorized, targetUser, null, OfferResponseType.URI, 0, 0);
    }

    /**
     * Creates a Credential Offer that can be pre-authorized and/or bound to a specific target user.
     * <p>
     * Credential Offer Validity Matrix for the supported request parameters "pre_authorized", "targetUser" combinations.
     * </p>
     * +----------+----------+---------+--------------------------------------------+
     * | Pre-Auth | Username | Valid   | Notes                                      |
     * +----------+----------+---------+--------------------------------------------+
     * | no       | no       | yes     | Anonymous offer; works for any login user. |
     * | no       | yes      | yes     | Offer restricted to a specific user.       |
     * +----------+----------+---------+--------------------------------------------+
     * | yes      | no       | yes     | Self issued pre-auth offer.                |
     * | yes      | yes      | yes     | Offer restricted to a specific user.       |
     * +----------+----------+---------+--------------------------------------------+
     * </p>
     * <b>Pre-Authorized Offer</b>
     * <ul>
     *   <li>A pre-authorized offer is authorized for the clientId from the current login session</li>
     *   <li>If targetUser is null or empty, it defaults to the user from the current login session</li>
     *   <li>If targetUser is equal to the current login, the generated offer is "self issued"</li>
     *   <li>To create an offer for another user, the issuing user must hold the {@code credential_offer_create} role</li>
     *   <li>A pre-authorized offer can optionally have an associated tx_code</li>
     *   <li>An offer can optionally have a predefined expiry date</li>
     * </ul>
     *
     * <b>Non Pre-Authorized Offer</b>
     * <ul>
     *   <li>If targetUser is null or empty, the generated offer is "anonymous"</li>
     *   <li>If targetUser is equal to the current login, the offer is "self issued"</li>
     *   <li>If targetUser is none of the above, the offer is "targeted"</li>
     *   <li>For a targeted offer, the issuing user must hold the {@code credential_offer_create} role</li>
     *   <li>An offer can optionally have a predefined expiry date</li>
     * </ul>
     *
     * The responseType supports "Same Device" and "Cross Device" use cases.
     * </p>
     * +---------+------------------+-------------------------------------------+
     * | Type    | Mime-Type        | Notes                                     |
     * +---------+------------------+-------------------------------------------+
     * | uri     | application/json | JSON document that contains the offer uri |
     * | uri_qr  | application/json | Same as 'uri' plus url encoded qr-code    |
     * | qr      | image/png        | Credential offer encoded as qr-code image |
     * +---------+------------------+---------+---------------------------------+
     * </p>
     * This endpoint creates an internal credential offer state, which can then be accessed via
     * a uniquely generated credential offer uri. It is the responsibility of the caller to
     * communicate the credential offer to the target user in a secure manner.
     * </p>
     * If the response contains a generated tx_code, which protects a pre-auth offer with a second layer of security,
     * this tx_code must be sent over an alternative communication channel (i.e. not together with the offer itself)
     *
     * @param credentialConfigurationId  A valid credential configuration id
     * @param preAuthorized A flag whether the offer should be pre-authorized
     * @param targetUser    The username that the offer is authorized for
     * @param expiresAt     The date/time when the offer expires (in Unix timestamp seconds)
     * @param responseType  The response type, which can be 'uri', 'qr' or 'uri+qr'
     * @param width         The width of the QR code image
     * @param height        The height of the QR code image
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, RESPONSE_TYPE_IMG_PNG})
    @Path(CREATE_CREDENTIAL_OFFER_PATH)
    public Response createCredentialOffer(
            @QueryParam("credential_configuration_id") String credentialConfigurationId,
            @QueryParam("pre_authorized") @DefaultValue("false") Boolean preAuthorized,
            @QueryParam("target_user") String targetUser,
            @QueryParam("expire") Integer expiresAt,
            @QueryParam("type") @DefaultValue("uri") OfferResponseType responseType,
            @QueryParam("width") @DefaultValue("200") int width,
            @QueryParam("height") @DefaultValue("200") int height
    ) {
        configureCors(true);
        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel loginUserModel = userSession.getUser();
        ClientModel clientModel = clientSession.getClient();
        RealmModel realmModel = clientModel.getRealm();

        EventBuilder eventBuilder = new EventBuilder(realmModel, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER)
                .client(clientModel)
                .user(loginUserModel)
                .session(userSession.getId())
                .detail(Details.CREDENTIAL_TYPE, credentialConfigurationId)
                .detail(Details.USERNAME, targetUser);

        cors.checkAllowedOrigins(session, clientModel);
        checkIsOid4vciEnabled(eventBuilder);
        checkRestCredentialOfferEnabled(eventBuilder);
        checkClientEnabled(eventBuilder);

        // Verify required credConfigId
        //
        if (Strings.isEmpty(credentialConfigurationId)) {
            var errorMessage = "Missing credential configuration id";
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_REQUEST.getValue());
            throw new CorsErrorResponseException(cors,
                    ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.getValue(), errorMessage, Response.Status.BAD_REQUEST);
        }

        // Verify image dimensions
        //
        if (List.of(OfferResponseType.QR, OfferResponseType.URI_QR).contains(responseType)) {
            if (width < 1 || height < 1 || MAX_QR_CODE_DIMENSION < width || MAX_QR_CODE_DIMENSION < height) {
                var dim = String.format("%dx%d", MAX_QR_CODE_DIMENSION, MAX_QR_CODE_DIMENSION);
                var errorMessage = "Requested QR Code too large, allowed maximum is " + dim;
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_REQUEST.getValue());
                throw new CorsErrorResponseException(cors,
                        ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.getValue(), errorMessage, Response.Status.BAD_REQUEST);
            }
        }

        LOGGER.debugf("Create a credential offer for %s", credentialConfigurationId);

        // For pre-auth offers, default the targetUser to the login user (self-issued offer)
        //
        if (preAuthorized && Strings.isEmpty(targetUser)) {
            targetUser = loginUserModel.getUsername();
        }

        if (expiresAt == null) {
            expiresAt = timeProvider.currentTimeSeconds() + credentialOfferLifespan;
        }

        // Create the CredentialsOffer
        //
        String targetClientId = clientModel.getClientId();
        String grantType = preAuthorized ? PRE_AUTH_GRANT_TYPE : AUTH_CODE_GRANT_TYPE;
        List<String> credentialConfigurationIds = List.of(credentialConfigurationId);

        CredentialOfferState offerState;
        try {

            CredentialOfferProvider offerProvider = session.getProvider(CredentialOfferProvider.class);
            offerState = offerProvider.createCredentialOffer(loginUserModel, grantType,
                    credentialConfigurationIds, targetClientId, targetUser, expiresAt);

        } catch (CredentialOfferException ex) {
            eventBuilder.detail(Details.REASON, ex.getMessage()).error(ex.getErrorType());
            throw new CorsErrorResponseException(cors,
                    ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.getValue(), ex.getMessage(), Response.Status.BAD_REQUEST);
        }

        // Store the CredentialOfferState
        //
        CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
        offerStorage.putOfferState(offerState);

        String targetUserId = offerState.getTargetUserId();
        LOGGER.debugf("Stored credential offer state: [grant=%s, ids=%s, cid=%s, uid=%s, nonce=%s]",
                grantType, credentialConfigurationIds, offerState.getTargetClientId(), targetUserId, offerState.getNonce());

        // Add event details
        eventBuilder.detail(Details.VERIFIABLE_CREDENTIAL_PRE_AUTHORIZED, String.valueOf(preAuthorized))
                .detail(Details.RESPONSE_TYPE, responseType.toString())
                .detail(Details.VERIFIABLE_CREDENTIAL_TARGET_CLIENT_ID, targetClientId)
                .detail(Details.VERIFIABLE_CREDENTIAL_TARGET_USER_ID, targetUserId)
                .success();

        CredentialsOffer credOffer = offerState.getCredentialsOffer();
        CredentialOfferURI credOfferURI = new CredentialOfferURI()
                .setIssuer(credOffer.getCredentialIssuer() + "/protocol/" + OID4VC_PROTOCOL + "/" + CREDENTIAL_OFFER_PATH)
                .setNonce(offerState.getNonce());

        // Respond with QR-Code as 'image/png'
        if (responseType == OfferResponseType.QR) {
            byte[] qrBytes = generateQrCode(credOfferURI, width, height);
            return cors.add(Response.ok()
                    .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                    .type(RESPONSE_TYPE_IMG_PNG)
                    .entity(qrBytes));
        }

        // Respond with URI + QR-Code as 'application/json'
        if (responseType == OfferResponseType.URI_QR) {
            byte[] qrBytes = generateQrCode(credOfferURI, width, height);
            String encodedBytes = Base64.getEncoder().encodeToString(qrBytes);
            credOfferURI.setQrCode("data:image/png;base64," + encodedBytes);
            return cors.add(Response.ok()
                    .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                    .type(MediaType.APPLICATION_JSON)
                    .entity(credOfferURI));
        }

        // Respond with URI as 'application/json'
        return cors.add(Response.ok()
                .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                .type(MediaType.APPLICATION_JSON)
                .entity(credOfferURI));
    }

    private byte[] generateQrCode(CredentialOfferURI credOfferURI, int width, int height) {
        String offerUri = OID4VCUtil.getOfferAsUri(session, credOfferURI.getNonce());
        try {
            return QRCodeUtils.encodeAsQRBytes(offerUri, width, height);
        } catch (WriterException | IOException e) {
            String msg = String.format("Cannot create a qr code of dimension %s:%s", width, height);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Configures basic CORS for error responses before authentication
     */
    private void configureCors(boolean authenticated) {
        cors = Cors.builder()
                .allowedMethods(HttpGet.METHOD_NAME, HttpOptions.METHOD_NAME)
                .allowAllOrigins()
                .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS, HttpHeaders.CONTENT_TYPE);
        if (authenticated) {
            cors = cors.auth();
        }
    }

    /**
     * Handles CORS preflight requests for credential offer endpoint
     */
    @OPTIONS
    @Path(CREDENTIAL_OFFER_PATH + "/{nonce}")
    public Response getCredentialOfferPreflight(@PathParam("nonce") String nonce) {
        configureCors(false);
        cors.preflight();
        return cors.add(Response.ok());
    }

    /**
     * Provides an OID4VCI compliant credential offer
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(CREDENTIAL_OFFER_PATH + "/{nonce}")
    public Response getCredentialOffer(@PathParam("nonce") String nonce) {
        configureCors(false);

        if (nonce == null) {
            var errorMessage = "No credential offer nonce";
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST, errorMessage));
        }

        RealmModel realm = session.getContext().getRealm();

        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST);

        checkIsOid4vciEnabled(eventBuilder);

        // Retrieve the associated credential offer state
        // The credential offer state remains in storage until it expires or the associated credential is fetched
        CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
        CredentialOfferState offerState = offerStorage.getOfferStateByNonce(nonce);
        if (offerState == null) {
            var errorMessage = "Credential offer not found or already consumed";
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST, errorMessage));
        }

        // We treat the credential offer URI as an unprotected capability URL and rely solely on the later authorization step
        // i.e. an authenticated client/user session is not required nor checked against the offer state
        CredentialsOffer credOffer = offerState.getCredentialsOffer();
        LOGGER.debugf("Found credential offer: [ids=%s, cid=%s, uid=%s, nonce=%s]",
                credOffer.getCredentialConfigurationIds(), offerState.getTargetClientId(), offerState.getTargetUserId(), offerState.getNonce());

        if (offerState.isExpired()) {
            var errorMessage = "Credential offer has already expired";
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.EXPIRED_CODE);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST, errorMessage));
        }

        // Add event details
        if (offerState.getTargetClientId() != null) {
            eventBuilder.client(offerState.getTargetClientId());
        }
        if (offerState.getTargetUserId() != null) {
            eventBuilder.user(offerState.getTargetUserId());
        }
        if (credOffer.getCredentialConfigurationIds() != null && !credOffer.getCredentialConfigurationIds().isEmpty()) {
            eventBuilder.detail(Details.CREDENTIAL_TYPE, String.join(",", credOffer.getCredentialConfigurationIds()));
        }

        LOGGER.debugf("Responding with offer: %s", JsonSerialization.valueAsString(credOffer));

        eventBuilder.success();
        return cors.add(Response.ok()
                .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                .entity(credOffer));
    }

    private void checkScope(CredentialScopeModel requestedCredential) {
        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        String vcIssuanceFlow = clientSession.getNote(PreAuthorizedCodeGrantType.VC_ISSUANCE_FLOW);

        if (vcIssuanceFlow == null || !vcIssuanceFlow.equals(PRE_AUTH_GRANT_TYPE)) {
            // Use getAuthResult() instead of bearerTokenAuthenticator.authenticate() directly
            // This ensures we benefit from the cachedAuthResult caching that prevents DPoP proof reuse
            AccessToken accessToken = getAuthResult().token();
            if (Arrays.stream(accessToken.getScope().split(" "))
                    .noneMatch(tokenScope -> tokenScope.equals(requestedCredential.getScope()))) {
                LOGGER.debugf("Scope check failure: required scope = %s, " +
                                "scope in access token = %s.",
                        requestedCredential.getName(), accessToken.getScope());
                throw new CorsErrorResponseException(cors,
                        ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue(),
                        "Scope check failure",
                        Response.Status.BAD_REQUEST);
            } else {
                LOGGER.debugf("Scope check success: required scope = %s, #" +
                                "scope in access token = %s.",
                        requestedCredential.getScope(), accessToken.getScope());
            }
        } else {
            clientSession.removeNote(PreAuthorizedCodeGrantType.VC_ISSUANCE_FLOW);
        }
    }

    /**
     * Returns a verifiable credential
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JWT})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JWT})
    @Path(CREDENTIAL_PATH)
    public Response requestCredential(String requestPayload) {
        RealmModel realmModel = session.getContext().getRealm();
        EventBuilder eventBuilder = new EventBuilder(realmModel, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_REQUEST);

        checkIsOid4vciEnabled(eventBuilder);

        LOGGER.debugf("Received credentials request with payload: %s", requestPayload);

        if (requestPayload == null || requestPayload.trim().isEmpty()) {
            String errorMessage = "Request payload is null or empty.";
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }

        cors = Cors.builder().auth().allowedMethods(HttpPost.METHOD_NAME).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        CredentialIssuer issuerMetadata = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();

        // Validate request encryption
        CredentialRequest credentialRequest = validateRequestEncryption(requestPayload, issuerMetadata, eventBuilder);

        // Authenticate first to fail fast on auth errors
        AuthenticationManager.AuthResult authResult = getAuthResult();

        // Set client and user info in event
        ClientModel clientModel = session.getContext().getClient();
        UserSessionModel userSession = authResult.session();
        UserModel userModel = userSession.getUser();
        eventBuilder.client(clientModel)
                .user(userModel)
                .session(userSession.getId())
                .detail(Details.USERNAME, userModel.getUsername());

        // Validate encryption parameters if present
        CredentialResponseEncryption encryptionParams = credentialRequest.getCredentialResponseEncryption();
        CredentialResponseEncryptionMetadata encryptionMetadata = OID4VCIssuerWellKnownProvider.getCredentialResponseEncryption(session);
        boolean isEncryptionRequired = Optional.ofNullable(encryptionMetadata)
                .map(CredentialResponseEncryptionMetadata::getEncryptionRequired)
                .orElse(false);

        // Validate encryption parameters if provided
        if (isEncryptionRequired || encryptionParams != null) {

            // Check if encryption is required but not provided
            if (encryptionParams == null) {
                String errorMessage = "Response encryption is required by the Credential Issuer, but no encryption parameters were provided.";
                LOGGER.debug(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            validateEncryptionParameters(encryptionParams);

            // Check if enc is supported
            if (!encryptionMetadata.getEncValuesSupported().contains(encryptionParams.getEnc())) {
                String errorMessage = String.format("Unsupported content encryption algorithm: enc=%s", encryptionParams.getEnc());
                LOGGER.debug(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            // Check compression (unchanged)
            if (encryptionParams.getZip() != null && !isSupportedCompression(encryptionMetadata, encryptionParams.getZip())) {
                String errorMessage = String.format("Unsupported compression parameter: zip=%s", encryptionParams.getZip());
                LOGGER.debug(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }
        }

        // Check that client is enabled - call after authentication
        //
        checkClientEnabled(eventBuilder);

        // Verify that we have credential_configuration_id or credential_identifier
        //
        String requestedCredentialIdentifier = credentialRequest.getCredentialIdentifier();
        String requestedCredentialConfigurationId = credentialRequest.getCredentialConfigurationId();
        if (Strings.isEmpty(requestedCredentialIdentifier) && Strings.isEmpty(requestedCredentialConfigurationId)) {
            String errorMessage = String.format("No credential_configuration_id nor credential_identifier in credential request: %s",
                    JsonSerialization.valueAsString(credentialRequest));
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }
        if (!Strings.isEmpty(requestedCredentialIdentifier) && !Strings.isEmpty(requestedCredentialConfigurationId)) {
            String errorMessage = String.format("Both credential_configuration_id and credential_identifier in credential request: %s",
                    JsonSerialization.valueAsString(credentialRequest));
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }

        AccessToken accessToken = authResult.token();

        // Retrieve the authorization_detail from the AccessToken
        // Note, we always have authorization_details associated with the AccessToken JWT
        //
        List<OID4VCAuthorizationDetail> tokenAuthDetails = getAuthorizationDetailsResponse(accessToken);
        if (tokenAuthDetails == null || tokenAuthDetails.isEmpty()) {
            var errorMessage = "Invalid AccessToken for credential request. No authorization_details";
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, errorMessage));
        }
        if (tokenAuthDetails.size() > 1) {
            var errorMessage = String.format("Multiple authorization_details not supported: %s", tokenAuthDetails);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }
        OID4VCAuthorizationDetail tokenAuthDetail = tokenAuthDetails.get(0);
        String authorizedCredentialConfigurationId = tokenAuthDetail.getCredentialConfigurationId();
        List<String> authorizedCredentialIdentifiers = Optional.ofNullable(tokenAuthDetail.getCredentialIdentifiers()).orElse(List.of());

        // AccessToken authorization_details MUST contain a credential_configuration_id
        if (authorizedCredentialConfigurationId == null) {
            var errorMessage = String.format("No credential_configuration_id in authorization_details: %s", tokenAuthDetail);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }

        // Retrieve the optional credential offer state
        // In case of credential request by scope, it will not be available
        //
        CredentialOfferState offerState = null;
        CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);

        String credOfferId = tokenAuthDetail.getCredentialsOfferId();
        if (credOfferId != null) {

            offerState = offerStorage.getOfferStateById(credOfferId);
            if (offerState == null) {
                var errorMessage = "No credential offer state for: " + credOfferId;
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
            }

            // Verify not expired
            // The cache should have evicted the expired CredentialOfferState - we check anyway
            if (offerState.isExpired()) {
                var errorMessage = "Credential offer has already expired";
                LOGGER.errorf(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
            }

            // Verify the user login session
            //
            if (offerState.getTargetUserId() != null && !offerState.getTargetUserId().equals(userModel.getId())) {
                var errorMessage = "Unexpected login user: " + userModel.getUsername();
                LOGGER.errorf(errorMessage + " != %s", offerState.getTargetUserId());
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
            }

            // Verify the login client
            //
            if (offerState.getTargetClientId() != null && !offerState.getTargetClientId().equals(clientModel.getClientId())) {
                var errorMessage = "Unexpected login client: " + clientModel.getClientId();
                LOGGER.errorf(errorMessage + " != %s", offerState.getTargetClientId());
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
            }
        }

        // Validate that authorization_details from the token matches the offer state
        // This ensures the correct access token is being used for the credential request
        if (offerState != null && !List.of(tokenAuthDetail).equals(offerState.getAuthorizationDetails())) {
            var errorMessage = "Authorization details in access token do not match the credential offer state. " +
                    "The access token may not be the one issued for this credential offer.";
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }

        // Verify that the requested credential_configuration_id in the request matches one in the authorization_details
        //
        if (!Strings.isEmpty(requestedCredentialConfigurationId)) {

            if (!authorizedCredentialConfigurationId.equals(requestedCredentialConfigurationId)) {
                var errorMessage = "Credential configuration '" + requestedCredentialConfigurationId + "' not found in authorization_details";
                LOGGER.debug(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, errorMessage));
            }

            if (!authorizedCredentialIdentifiers.isEmpty()) {
                var errorMessage = "Credential must be requested by credential identifier from authorization_details: " + authorizedCredentialIdentifiers;
                LOGGER.debug(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
            }
        }

        // Verify that the requested credential_identifier in the request matches one in the authorization_details
        //
        if (!Strings.isEmpty(requestedCredentialIdentifier) && !authorizedCredentialIdentifiers.contains(requestedCredentialIdentifier)) {
            var errorMessage = "Credential identifier '" + requestedCredentialIdentifier + "' not found in authorization_details. " +
                    "The credential_identifier must match one from the authorization_details in the access token.";
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER, errorMessage));
        }

        // Find the credential configuration in the Issuer's metadata
        //
        SupportedCredentialConfiguration credConfig = getSupportedCredentials(session).get(authorizedCredentialConfigurationId);
        if (credConfig == null) {
            var errorMessage = "Credential configuration not found in issuer metadata: " + authorizedCredentialConfigurationId;
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, errorMessage));
        }

        // Find credential client scope by requested/authorized credential_configuration_id
        //
        CredentialScopeModel authorizedCredentialScope = CredentialScopeModelUtils.findCredentialScopeModelByConfigurationId(
                realmModel, () -> clientModel.getClientScopes(false).values().stream(), authorizedCredentialConfigurationId);

        if (authorizedCredentialScope == null) {
            var errorMessage = String.format("Credential client scope not found: %s", authorizedCredentialConfigurationId);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }

        LOGGER.debugf("Found credential scope for credential_configuration_id: %s", authorizedCredentialConfigurationId);
        eventBuilder.detail(Details.CREDENTIAL_TYPE, authorizedCredentialConfigurationId);

        checkScope(authorizedCredentialScope);

        SupportedCredentialConfiguration supportedCredential =
                OID4VCIssuerWellKnownProvider.toSupportedCredentialConfiguration(session, authorizedCredentialScope);

        enforceProofContractForCredential(supportedCredential, credentialRequest.getProofs());

        // Get the list of all proofs (handles single proof, multiple proofs, or none)
        List<String> allProofs = getAllProofs(credentialRequest);

        // Generate credential response
        CredentialResponse responseVO = new CredentialResponse();

        if (allProofs.isEmpty()) {
            // Single issuance without proof
            Object theCredential = getCredential(authResult, supportedCredential, tokenAuthDetail, credentialRequest, eventBuilder);
            responseVO.addCredential(theCredential);
        } else {
            // Issue credentials for each proof
            Proofs originalProofs = credentialRequest.getProofs();
            // Determine the proof type from the original proofs
            String proofType = originalProofs.getProofType();

            for (String currentProof : allProofs) {
                Proofs proofForIteration = Proofs.create(proofType, currentProof);
                // Creating credential with keybinding to the current proof
                credentialRequest.setProofs(proofForIteration);
                Object theCredential = getCredential(authResult, supportedCredential, tokenAuthDetail, credentialRequest, eventBuilder);
                responseVO.addCredential(theCredential);
            }
            credentialRequest.setProofs(originalProofs);
        }

        // Encrypt all responses if encryption parameters are provided, except for error credential responses
        Response response;
        if (encryptionParams != null && !responseVO.getCredentials().isEmpty()) {
            String jwe = encryptCredentialResponse(eventBuilder, responseVO, encryptionParams, encryptionMetadata);
            response = Response.ok()
                    .type(MediaType.APPLICATION_JWT)
                    .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                    .entity(jwe)
                    .build();
        } else {
            response = Response.ok()
                    .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                    .entity(responseVO)
                    .build();
        }

        // Mark event as successful
        eventBuilder.detail(Details.SCOPE, supportedCredential.getScope())
                .detail(Details.VERIFIABLE_CREDENTIAL_FORMAT, supportedCredential.getFormat())
                .detail(Details.VERIFIABLE_CREDENTIALS_ISSUED, String.valueOf(responseVO.getCredentials().size()));
        eventBuilder.success();

        // Clean up offer state after successful credential issuance
        // This prevents memory leaks while ensuring the state remains available during the request
        if (offerState != null) {
            offerStorage.removeOfferState(offerState);
            LOGGER.debugf("Removed credential offer state after successful issuance");
        }

        return response;
    }

    private List<OID4VCAuthorizationDetail> getAuthorizationDetailsResponse(AccessToken accessToken) {
        List<AuthorizationDetailsJSONRepresentation> tokenAuthDetails = accessToken.getAuthorizationDetails();
        AuthorizationDetailsProcessor<OID4VCAuthorizationDetail> oid4vcProcessor = session.getProvider(AuthorizationDetailsProcessor.class, OPENID_CREDENTIAL);
        return oid4vcProcessor.getSupportedAuthorizationDetails(tokenAuthDetails);
    }

    private CredentialRequest validateRequestEncryption(String requestPayload, CredentialIssuer issuerMetadata, EventBuilder eventBuilder) throws BadRequestException {
        CredentialRequestEncryptionMetadata requestEncryptionMetadata = issuerMetadata.getCredentialRequestEncryption();
        boolean isRequestEncryptionRequired = Optional.ofNullable(requestEncryptionMetadata)
                .map(CredentialRequestEncryptionMetadata::isEncryptionRequired)
                .orElse(false);

        // Determine if the request is a JWE based on content-type
        String contentType = session.getContext().getHttpRequest().getHttpHeaders()
                .getHeaderString(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            contentType = contentType.split(";")[0].trim(); // Handle parameters like charset
        }
        boolean contentTypeIsJwt = MediaType.APPLICATION_JWT.equalsIgnoreCase(contentType);
        boolean payloadLooksLikeJwe = isCompactJwePayload(requestPayload);

        // OID4VCI Section 10: encrypted request payloads MUST use application/jwt media type.
        if (payloadLooksLikeJwe && !contentTypeIsJwt) {
            String errorMessage = "Encrypted Credential Request must use Content-Type application/jwt.";
            LOGGER.debug(errorMessage);
            if (eventBuilder != null) {
                eventBuilder.detail(Details.REASON, errorMessage)
                        .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
            }
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
        }

        if (isRequestEncryptionRequired || contentTypeIsJwt) {
            if (requestEncryptionMetadata == null && contentTypeIsJwt) {
                String errorMessage = "Received JWT content-type request, but credential_request_encryption is not supported.";
                LOGGER.debug(errorMessage);
                if (eventBuilder != null) {
                    eventBuilder.detail(Details.REASON, errorMessage)
                            .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                }
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            try {
                return decryptCredentialRequest(requestPayload, requestEncryptionMetadata);
            } catch (Exception e) {
                if (isRequestEncryptionRequired) {
                    String errorMessage = "Encryption is required but request is not a valid JWE: " + e.getMessage();
                    LOGGER.debug(errorMessage);
                    if (eventBuilder != null) {
                        eventBuilder.detail(Details.REASON, errorMessage)
                                .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                    }
                    throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
                }
                if (contentTypeIsJwt) {
                    String errorMessage = "Request has JWT content-type but is not a valid JWE: " + e.getMessage();
                    LOGGER.debug(errorMessage);
                    if (eventBuilder != null) {
                        eventBuilder.detail(Details.REASON, errorMessage)
                                .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                    }
                    throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
                }
            }
        }

        try {
            CredentialRequest credentialRequest = JsonSerialization.mapper.readValue(requestPayload, CredentialRequest.class);
            // OID4VCI 1.0 Section 8.2:
            // If credential_response_encryption is requested, Credential Request encryption MUST be used.
            if (credentialRequest.getCredentialResponseEncryption() != null) {
                String errorMessage = "credential_response_encryption requires encrypted Credential Request (JWE) on top of TLS.";
                LOGGER.debug(errorMessage);
                if (eventBuilder != null) {
                    eventBuilder.detail(Details.REASON, errorMessage)
                            .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                }
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }
            normalizeProofFields(credentialRequest);
            return credentialRequest;
        } catch (JsonProcessingException e) {
            var errorMessage = "Failed to parse JSON request: " + e.getMessage();
            LOGGER.errorf(e, "JSON parsing failed. Request payload length: %d",
                    requestPayload != null ? requestPayload.length() : 0);
            if (eventBuilder != null) {
                eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            }
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }
    }

    /**
     * Decrypts a JWE-encoded Credential Request and validates it against metadata.
     *
     * @param jweString The JWE compact serialization
     * @param metadata The CredentialRequestEncryptionMetadata
     * @return The parsed CredentialRequest
     * @throws JWEException If decryption or validation fails
     */
    private CredentialRequest decryptCredentialRequest(String jweString, CredentialRequestEncryptionMetadata metadata) throws Exception {
        JWE jwe = new JWE(jweString);
        JOSEHeader rawHeader = jwe.getHeader();
        if (!(rawHeader instanceof JWEHeader)) {
            throw new JWEException("Invalid header type: expected JWEHeader but got " + rawHeader.getClass().getName());
        }
        JWEHeader header = (JWEHeader) rawHeader;

        // Validate alg and enc against supported values
        String enc = header.getEncryptionAlgorithm();
        if (!metadata.getEncValuesSupported().contains(enc)) {
            String errorMessage = String.format("Unsupported content encryption algorithm: enc=%s", enc);
            LOGGER.debug(errorMessage);
            throw new JWEException(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
        }

        // Handle compression if present
        String zip = header.getCompressionAlgorithm();
        if (zip != null) {
            if (!DEFLATE_COMPRESSION.equals(zip) || metadata.getZipValuesSupported() == null || !metadata.getZipValuesSupported().contains(zip)) {
                String errorMessage = String.format("Unsupported compression algorithm: zip=%s", zip);
                LOGGER.debug(errorMessage);
                throw new JWEException(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
            }
        }

        // Get a private key from KeyManager based on kid
        String kid = header.getKeyId();
        if (kid == null) {
            throw new JWEException("Missing kid in JWE header");
        }

        RealmModel realm = session.getContext().getRealm();
        KeyManager keyManager = session.keys();
        List<KeyWrapper> matchingKeys = keyManager.getKeysStream(realm)
                .filter(key -> KeyUse.ENC.equals(key.getUse()) && kid.equals(key.getKid()))
                .collect(Collectors.toList());

        if (matchingKeys.isEmpty()) {
            throw new JWEException("No encryption key found for kid: " + kid);
        }
        if (matchingKeys.size() > 1) {
            throw new JWEException("Multiple encryption keys found for kid: " + kid);
        }
        KeyWrapper keyWrapper = matchingKeys.get(0);

        // Set the decryption key
        jwe.getKeyStorage().setDecryptionKey(keyWrapper.getPrivateKey());

        // Decrypt the JWE
        try {
            jwe.verifyAndDecodeJwe();
        } catch (JWEException e) {
            throw new JWEException("Failed to decrypt JWE: " + e.getMessage());
        }

        // Handle decompression if needed
        byte[] content = jwe.getContent();
        if (zip != null) {
            content = decompress(content, zip);
        }

        // Parse decrypted content to CredentialRequest
        try {
            CredentialRequest credentialRequest = JsonSerialization.mapper.readValue(content, CredentialRequest.class);
            normalizeProofFields(credentialRequest);
            return credentialRequest;
        } catch (JsonProcessingException e) {
            throw new JWEException("Failed to parse decrypted JWE payload: " + e.getMessage());
        }
    }


    /**
     * Decompresses content using the specified algorithm.
     *
     * @param content The compressed content
     * @param zipAlgorithm The compression algorithm (e.g., "DEF")
     * @return The decompressed content
     * @throws JWEException If decompression fails
     */
    // TODO handle compression/decompression transparently at the JWE software layer.
    private byte[] decompress(byte[] content, String zipAlgorithm) throws JWEException {
        if (DEFLATE_COMPRESSION.equals(zipAlgorithm)) {
            try {
                return IOUtils.toByteArray(DeflateUtil.decode(content));
            } catch (IOException e) {
                throw new JWEException("Failed to decompress: " + e.getMessage());
            }
        }
        throw new JWEException("Unsupported compression algorithm");
    }

    /**
     * Normalizes legacy 'proof' field into 'proofs' and validates mutual exclusivity.
     * <p>
     * If a single 'proof' is present and 'proofs' is absent, converts it into a
     * single-element JWT list under 'proofs' for backward compatibility.
     * If both are present, throws a BadRequestException.
     */
    private void normalizeProofFields(CredentialRequest credentialRequest) {
        if (credentialRequest == null) {
            return;
        }

        if (credentialRequest.getProof() != null && credentialRequest.getProofs() != null) {
            String message = "Both 'proof' and 'proofs' must not be present at the same time";
            LOGGER.debug(message);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, message));
        }

        if (credentialRequest.getProof() != null) {
            LOGGER.debugf("Converting single 'proof' field to 'proofs' array for backward compatibility");
            Object singleProof = credentialRequest.getProof();
            Proofs proofsArray = new Proofs();

            // Handle AttestationProof
            if (singleProof instanceof AttestationProof attestationProof) {
                String attestationValue = attestationProof.getAttestation();
                if (attestationValue != null) {
                    proofsArray.setAttestation(List.of(attestationValue));
                }
            }
            // Handle JwtProof
            else if (singleProof instanceof JwtProof jwtProof) {
                String jwtValue = jwtProof.getJwt();
                if (jwtValue != null) {
                    proofsArray.setJwt(List.of(jwtValue));
                }
            } else {
                String message = "Unsupported proof type: " + (singleProof != null ? singleProof.getClass().getName() : "null");
                LOGGER.debug(message);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, message));
            }

            credentialRequest.setProofs(proofsArray);
            credentialRequest.setProof(null);
        }

        validateProofTypes(credentialRequest.getProofs());
    }

    private String selectKeyManagementAlg(EventBuilder eventBuilder, CredentialResponseEncryptionMetadata metadata, JWK jwk) {

        List<String> supportedAlgs = metadata.getAlgValuesSupported();
        if (supportedAlgs == null || supportedAlgs.isEmpty()) {
            LOGGER.warn("No supported encryption algorithms");
            return null;
        }

        // The alg parameter MUST be present in the JWK
        String jwkAlg = jwk.getAlgorithm();
        if (jwkAlg == null) {
            LOGGER.warnf("JWK is missing required 'alg' parameter for key type: %s", jwk.getKeyType());
            return null;
        }

        // Verify the alg is supported by the server
        if (!supportedAlgs.contains(jwkAlg)) {
            String errorMessage = String.format("JWK algorithm '%s' is not supported. Supported algorithms: %s", jwkAlg, supportedAlgs);
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
        }

        return jwkAlg;
    }

    private List<String> getAllProofs(CredentialRequest credentialRequestVO) {
        Proofs proofs = credentialRequestVO.getProofs();
        if (proofs == null) {
            return new ArrayList<>(); // No proofs provided
        }

        // Validation already happened in normalizeProofFields, so we can safely extract proofs
        return proofs.getAllProofs();
    }

    private void enforceProofContractForCredential(SupportedCredentialConfiguration credentialConfiguration, Proofs proofs) {
        boolean proofConfigured = credentialConfiguration != null
                && credentialConfiguration.getProofTypesSupported() != null
                && credentialConfiguration.getProofTypesSupported().getSupportedProofTypes() != null
                && !credentialConfiguration.getProofTypesSupported().getSupportedProofTypes().isEmpty();
        boolean proofProvided = proofs != null && !proofs.getPresentProofTypes().isEmpty();

        if (proofConfigured && !proofProvided) {
            String message = "The 'proofs' parameter is required for this credential configuration.";
            LOGGER.debug(message);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_PROOF, message));
        }

        if (!proofConfigured && proofProvided) {
            String message = "The 'proofs' parameter is not supported for this credential configuration.";
            LOGGER.debug(message);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_PROOF, message));
        }
    }

    private void validateProofTypes(Proofs proofs) {
        if (proofs == null) {
            return;
        }

        boolean hasJwtProofs = hasProofEntries(proofs.getJwt());
        boolean hasAttestationProofs = hasProofEntries(proofs.getAttestation());

        if (hasJwtProofs && hasAttestationProofs) {
            LOGGER.debug("The 'proofs' object must not contain multiple proof types.");
            throw new BadRequestException(getErrorResponse(INVALID_PROOF,
                    "The 'proofs' object must not contain multiple proof types."));
        }
    }

    private boolean hasProofEntries(List<String> proofs) {
        return proofs != null && !proofs.isEmpty();
    }

    /**
     * Encrypts a CredentialResponse as a JWE using the provided encryption parameters.
     *
     * @param response         The CredentialResponse to encrypt
     * @param encryptionParams The encryption parameters (alg, enc, jwk)
     * @return The compact JWE serialization
     * @throws BadRequestException     If encryption parameters are invalid
     * @throws WebApplicationException If encryption fails due to server issues
     */
    private String encryptCredentialResponse(EventBuilder eventBuilder, CredentialResponse response,
                                             CredentialResponseEncryption encryptionParams,
                                             CredentialResponseEncryptionMetadata metadata) {
        validateEncryptionParameters(encryptionParams);

        String enc = encryptionParams.getEnc();
        String zip = encryptionParams.getZip();
        JWK jwk = encryptionParams.getJwk();

        // Parse public key
        PublicKey publicKey;
        {
            String message = "Invalid JWK: Failed to parse public key";
            try {
                publicKey = JWKParser.create(jwk).toPublicKey();
                if (publicKey == null) {
                    LOGGER.debug(message);
                    throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, message));
                }
            } catch (Exception e) {
                LOGGER.debug(message);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, message));
            }
        }

        // Select alg
        String selectedAlg = selectKeyManagementAlg(eventBuilder, metadata, jwk);

        // Perform encryption
        try {
            byte[] content = JsonSerialization.writeValueAsBytes(response);

            // Apply compression if specified
            if (zip != null) {
                content = compressContent(content, zip);
            }

            JWEHeader header = new JWEHeader.JWEHeaderBuilder()
                    .algorithm(selectedAlg)
                    .encryptionAlgorithm(enc)
                    .compressionAlgorithm(zip)
                    .keyId(jwk.getKeyId())
                    .build();

            JWE jwe = new JWE()
                    .header(header)
                    .content(content);
            jwe.getKeyStorage().setEncryptionKey(publicKey);


            return jwe.encodeJwe();
        } catch (IOException e) {
            LOGGER.errorf("Serialization failed: %s", e.getMessage());
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse().setErrorDescription("Failed to serialize response"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        } catch (JWEException e) {
            LOGGER.errorf("Encryption operation failed: %s", e.getMessage());
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse().setErrorDescription("Encryption operation failed: " + e.getMessage()))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
    }

    /**
     * Compress content using the specified algorithm
     */
    // TODO handle compression/decompression transparently at the JWE software layer.
    private byte[] compressContent(byte[] content, String zipAlgorithm) throws IOException {
        if (DEFLATE_COMPRESSION.equals(zipAlgorithm)) {
            return DeflateUtil.encode(content);
        }
        throw new IllegalArgumentException("Unsupported compression algorithm: " + zipAlgorithm);
    }


    /**
     * Validate the encryption parameters for a credential response.
     *
     * @param encryptionParams The encryption parameters to validate
     * @throws BadRequestException If the encryption parameters are invalid
     */
    private void validateEncryptionParameters(CredentialResponseEncryption encryptionParams) {
        if (encryptionParams == null) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS,
                    "Missing required encryption parameters (enc and jwk)."));
        }

        List<String> missingParams = new ArrayList<>();
        if (encryptionParams.getEnc() == null) missingParams.add("enc");
        if (encryptionParams.getJwk() == null) missingParams.add("jwk");

        if (!missingParams.isEmpty()) {
            throw new BadRequestException(getErrorResponse(
                    ErrorType.INVALID_ENCRYPTION_PARAMETERS,
                    String.format("Missing required parameters: %s", String.join(", ", missingParams))
            ));
        }

        if (!isValidJwkForEncryption(encryptionParams.getJwk())) {
            String errorMessage = "Invalid JWK: Not suitable for encryption";
            LOGGER.debug(errorMessage);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
        }
    }

    /**
     * Validates if the provided JWK is suitable for encryption.
     *
     * @param jwk The JWK to validate
     * @return true if the JWK is valid for encryption, false otherwise
     */
    private boolean isValidJwkForEncryption(JWK jwk) {
        if (jwk == null) {
            return false;
        }
        String publicKeyUse = jwk.getPublicKeyUse();
        return publicKeyUse == null || "enc".equals(publicKeyUse);
    }

    private boolean isSupportedCompression(CredentialResponseEncryptionMetadata metadata, String zip) {
        return metadata != null &&
                metadata.getZipValuesSupported() != null &&
                metadata.getZipValuesSupported().contains(zip);
    }

    /**
     * Detect whether payload is a compact JWE by delegating to Keycloak JOSE parser.
     */
    private boolean isCompactJwePayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return false;
        }
        try {
            JWE jwe = new JWE(payload.trim());
            return jwe.getHeader() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private AuthenticatedClientSessionModel getAuthenticatedClientSession() {
        AuthenticationManager.AuthResult authResult = getAuthResult();
        UserSessionModel userSessionModel = authResult.session();

        AuthenticatedClientSessionModel clientSession = userSessionModel.
                getAuthenticatedClientSessionByClient(
                        authResult.client().getId());
        if (clientSession == null) {
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_TOKEN.getValue(),
                    "Invalid or missing token",
                    Response.Status.BAD_REQUEST);
        }
        return clientSession;
    }

    private AuthenticationManager.AuthResult getAuthResult() {
        if (cachedAuthResult != null) {
            return cachedAuthResult;
        }

        AuthenticationManager.AuthResult authResult = bearerTokenAuthenticator.authenticate();
        if (authResult == null) {
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_TOKEN.getValue(),
                    "Invalid or missing token",
                    Response.Status.BAD_REQUEST);
        }

        // Validate DPoP nonce if present in the DPoP proof
        DPoP dPoP = (DPoP) session.getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE);
        if (dPoP != null) {
            Object nonceClaim = Optional.ofNullable(dPoP.getOtherClaims())
                    .map(m -> m.get("nonce"))
                    .orElse(null);
            if (nonceClaim instanceof String nonceJwt && !nonceJwt.isEmpty()) {
                try {
                    CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                    String expectedAudience = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
                    String expectedSource = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
                    cNonceHandler.verifyCNonce(
                            nonceJwt,
                            List.of(expectedAudience),
                            Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, expectedSource)
                    );
                } catch (VerificationException e) {
                    LOGGER.debugf("DPoP nonce validation failed: %s", e.getMessage());
                    throw new CorsErrorResponseException(
                            cors,
                            ErrorType.INVALID_TOKEN.getValue(),
                            "Invalid or missing token",
                            Response.Status.BAD_REQUEST);
                }
            }
        }

        cachedAuthResult = authResult;
        return cachedAuthResult;
    }

    /**
     * Get a signed credential
     *
     * @param authResult          authResult containing the userSession to create the credential for
     * @param credentialConfig    the supported credential configuration
     * @param authDetail          Parsed OID4VC authorization_detail
     * @param credentialRequestVO the credential request
     * @param eventBuilder        the event builder for logging events
     * @return the signed credential
     */
    private Object getCredential(AuthenticationManager.AuthResult authResult,
                                 SupportedCredentialConfiguration credentialConfig,
                                 OID4VCAuthorizationDetail authDetail,
                                 CredentialRequest credentialRequestVO,
                                 EventBuilder eventBuilder
    ) {

        // Get the client scope model from the credential configuration
        CredentialScopeModel credentialScopeModel = getClientScopeModel(credentialConfig);

        // Get the protocol mappers from the client scope
        List<OID4VCMapper> protocolMappers = credentialScopeModel.getProtocolMappersStream()
                .map(pm -> {
                    if (session.getProvider(ProtocolMapper.class, pm.getProtocolMapper()) instanceof OID4VCMapper mapperFactory) {
                        ProtocolMapper protocolMapper = mapperFactory.create(session);
                        if (protocolMapper instanceof OID4VCMapper oid4VCMapper) {
                            oid4VCMapper.setMapperModel(pm, credentialScopeModel.getFormat());
                            return oid4VCMapper;
                        }
                    }
                    LOGGER.warnf("The protocol mapper %s is not an instance of OID4VCMapper.", pm.getId());
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        VCIssuanceContext vcIssuanceContext = getVCToSign(protocolMappers, credentialConfig, authResult, authDetail, credentialRequestVO, credentialScopeModel, eventBuilder);

        // Enforce key binding prior to signing if necessary
        enforceKeyBindingIfProofProvided(vcIssuanceContext);

        // Retrieve matching credential signer
        CredentialSigner<?> credentialSigner = session.getProvider(CredentialSigner.class,
                credentialConfig.getFormat());

        return Optional.ofNullable(credentialSigner)
                .map(signer -> {
                    try {
                        return signer.signCredential(
                                vcIssuanceContext.getCredentialBody(),
                                credentialConfig.getCredentialBuildConfig()
                        );
                    } catch (CredentialSignerException cse) {
                        throw badRequestException(ErrorType.INVALID_CREDENTIAL_REQUEST, "Signing of credential failed: " + cse.getMessage(), eventBuilder);
                    }
                })
                .orElseThrow(() -> {
                    String message = String.format("No signer found for format '%s'.", credentialConfig.getFormat());
                    return badRequestException(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, message, eventBuilder);
                });
    }

    // Throw the error event and return corresponding BadRequestException
    private BadRequestException badRequestException(ErrorType errorType, String errorMessage, EventBuilder eventBuilder) {
        eventBuilder.detail(Details.REASON, errorMessage)
                .error(errorType.getValue());
        return new BadRequestException(getErrorResponse(errorType, errorMessage));
    }

    private CredentialScopeModel getClientScopeModel(SupportedCredentialConfiguration credentialConfig) {
        // Get the current client from the session
        ClientModel clientModel = session.getContext().getClient();

        // Get the client scope that matches the credentialConfig scope
        Map<String, ClientScopeModel> clientScopes = clientModel.getClientScopes(false);
        ClientScopeModel clientScopeModel = clientScopes.get(credentialConfig.getScope());

        if (clientScopeModel == null) {
            throw new BadRequestException("Client scope not found for the specified scope: " + credentialConfig.getScope());
        }

        return new CredentialScopeModel(clientScopeModel);
    }

    private Response.ResponseBuilder getErrorResponseBuilder(ErrorType errorType, String errorDescription) {
        var errorResponse = new ErrorResponse();
        errorResponse.setError(errorType).setErrorDescription(errorDescription);
        return Response
                .status(Response.Status.BAD_REQUEST)
                .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON);
    }

    private Response getErrorResponse(ErrorType errorType) {
        return getErrorResponseBuilder(errorType, null).build();
    }

    private Response getErrorResponse(ErrorType errorType, String errorDescription) {
        return getErrorResponseBuilder(errorType, errorDescription).build();
    }

    // builds the unsigned credential by applying all protocol mappers.
    private VCIssuanceContext getVCToSign(List<OID4VCMapper> protocolMappers, SupportedCredentialConfiguration credentialConfig,
                                          AuthenticationManager.AuthResult authResult, OID4VCAuthorizationDetail authDetail, CredentialRequest credentialRequestVO,
                                          CredentialScopeModel credentialScopeModel, EventBuilder eventBuilder) {

        // Compute issuance date and apply correlation-mitigation according to realm configuration
        Instant issuance = Instant.ofEpochMilli(timeProvider.currentTimeMillis());
        TimeClaimNormalizer timeClaimNormalizer = new TimeClaimNormalizer(session);
        Instant normalizedIssuance = timeClaimNormalizer.normalize(issuance);

        // Compute expiration date from client scope configuration and normalize it
        CredentialScopeModel clientScopeModel = credentialScopeModel;
        Integer expiryInSeconds = clientScopeModel.getExpiryInSeconds();
        Instant expiration = normalizedIssuance.plusSeconds(expiryInSeconds);
        Instant normalizedExpiration = timeClaimNormalizer.normalize(expiration);

        // set the required claims
        VerifiableCredential vc = new VerifiableCredential()
                .setIssuanceDate(normalizedIssuance)
                .setExpirationDate(normalizedExpiration)
                .setType(credentialScopeModel.getSupportedCredentialTypes());

        Map<String, Object> subjectClaims = new HashMap<>();
        protocolMappers.forEach(mapper -> mapper.setClaim(subjectClaims, authResult.session()));

        Map<String, Object> subjectClaimsWithMetadataPrefix = new HashMap<>();
        protocolMappers
                .forEach(mapper -> mapper.setClaimWithMetadataPrefix(subjectClaims, subjectClaimsWithMetadataPrefix));

        // Validate that requested claims from authorization_details are present
        String credentialConfigId = credentialConfig.getId();
        validateRequestedClaimsArePresent(subjectClaimsWithMetadataPrefix, credentialConfig, authResult.user(), authDetail, credentialConfigId, eventBuilder);

        // Include all available claims
        subjectClaims.forEach((key, value) -> vc.getCredentialSubject().setClaims(key, value));

        protocolMappers.forEach(mapper -> mapper.setClaim(vc, authResult.session()));

        LOGGER.debugf("The credential to sign is: %s", vc);

        // Build format-specific credential
        CredentialBody credentialBody = this.findCredentialBuilder(credentialConfig)
                .buildCredentialBody(vc, credentialConfig.getCredentialBuildConfig());

        return new VCIssuanceContext()
                .setAuthResult(authResult)
                .setCredentialBody(credentialBody)
                .setCredentialConfig(credentialConfig)
                .setCredentialRequest(credentialRequestVO);
    }

    /**
     * Enforce key binding: Validate proof and bind associated key to credential in issuance context.
     */
    private void enforceKeyBindingIfProofProvided(VCIssuanceContext vcIssuanceContext) {
        Proofs proofs = vcIssuanceContext.getCredentialRequest().getProofs();
        if (proofs == null) {
            LOGGER.debugf("No proofs provided, skipping key binding");
            return;
        }

        // Validate each proof type that is present
        for (String proofType : proofs.getPresentProofTypes()) {
            validateProofs(vcIssuanceContext, proofType);
        }
    }

    private void validateProofs(VCIssuanceContext vcIssuanceContext, String proofType) {
        ProofValidator proofValidator = session.getProvider(ProofValidator.class, proofType);
        if (proofValidator == null) {
            throw new BadRequestException(String.format("Unable to validate proofs of type %s", proofType));
        }

        // Validate proof and bind public keys to credential
        try {
            List<JWK> jwks = proofValidator.validateProof(vcIssuanceContext);
            if (jwks != null && !jwks.isEmpty()) {
                // Bind the first JWK to the credential
                vcIssuanceContext.getCredentialBody().addKeyBinding(jwks.get(0));
            }
        } catch (VCIssuerException e) {
            switch (e.getErrorType()) {
                case INVALID_NONCE:
                    throw new ErrorResponseException(INVALID_NONCE.getValue(), e.getMessage(), Response.Status.BAD_REQUEST);
                case INVALID_PROOF:
                    throw new ErrorResponseException(INVALID_PROOF.getValue(), e.getMessage(), Response.Status.BAD_REQUEST);
                default:
                    throw new BadRequestException("Could not validate provided proof", e);
            }
        }
    }

    private CredentialBuilder findCredentialBuilder(SupportedCredentialConfiguration credentialConfig) {
        String format = credentialConfig.getFormat();
        CredentialBuilder credentialBuilder = credentialBuilders.get(format);

        if (credentialBuilder == null) {
            String message = String.format("No credential builder found for format %s", format);
            throw new BadRequestException(message, getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, message));
        }

        return credentialBuilder;
    }

    /**
     * Validates that all requested claims from authorization_details are present in the available claims.
     *
     * @param allClaims        all available claims. These are the claims including metadata prefix with the resolved path
     * @param credentialConfig Credential configuration
     * @param user             the authenticated user
     * @param authzDetail      the parsed oid4vc authorization_detail
     * @param scope            the credential scope
     * @param eventBuilder     the event builder for logging error events
     * @throws BadRequestException if mandatory requested claims are missing
     */
    private void validateRequestedClaimsArePresent(Map<String, Object> allClaims, SupportedCredentialConfiguration credentialConfig,
                                                   UserModel user, OID4VCAuthorizationDetail authzDetail, String scope, EventBuilder eventBuilder) {
        // Protocol mappers from configuration
        Map<List<Object>, ClaimsDescription> claimsConfig = credentialConfig.getCredentialMetadata().getClaims()
                .stream()
                .map(claim -> {
                    List<Object> pathObj = new ArrayList<>(claim.getPath());
                    return new ClaimsDescription(pathObj, claim.isMandatory());
                })
                .collect(Collectors.toMap(ClaimsDescription::getPath, claimsDescription -> claimsDescription));

        List<ClaimsDescription> claimsFromAuthzDetails = getClaimsFromAuthzDetails(scope, user, authzDetail);

        // Merge claims from both protocolMappers and authorizationDetails. If either source specifies "mandatory" as true, claim is considered mandatory
        for (ClaimsDescription claimDescription : claimsFromAuthzDetails) {
            List<Object> path = claimDescription.getPath();
            ClaimsDescription existing = claimsConfig.get(path);
            if (existing == null) {
                claimsConfig.put(path, claimDescription);
            } else {
                if (claimDescription.isMandatory()) {
                    existing.setMandatory(true);
                }
            }
        }

        List<ClaimsDescription> claimsDescriptions = new ArrayList<>(claimsConfig.values());

        // Validate that all requested claims are present in the available claims
        // We use filterClaimsByAuthorizationDetails to check if claims can be found
        // but we don't actually filter - we just validate presence
        try {
            ClaimsPathPointer.filterClaimsByAuthorizationDetails(allClaims, claimsDescriptions);
            LOGGER.debugf("All requested claims are present for scope %s", scope);
        } catch (IllegalArgumentException e) {
            // If filtering fails, it means some requested claims are missing
            String errorMessage = "Credential issuance failed: " + e.getMessage() +
                    ". The requested claims are not available in the user profile.";
            LOGGER.warnf("Requested claims validation failed for scope '%s', user '%s', client '%s': %s",
                    scope, user.getUsername(), session.getContext().getClient().getClientId(), e.getMessage());
            // Use OID4VCI-specific error code and structured error response for HTTP clients
            if (eventBuilder != null) {
                eventBuilder.detail(Details.REASON, errorMessage)
                        .error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
            }
            throw new ErrorResponseException(
                    ErrorType.INVALID_CREDENTIAL_REQUEST.getValue(),
                    errorMessage,
                    Response.Status.BAD_REQUEST
            );
        }
    }

    private List<ClaimsDescription> getClaimsFromAuthzDetails(String scope, UserModel user, OID4VCAuthorizationDetail authzDetail) {
        List<ClaimsDescription> storedClaims = authzDetail == null ? null : authzDetail.getClaims();
        if (storedClaims == null || storedClaims.isEmpty()) {
            String username = user.getUsername();
            String clientId = session.getContext().getClient().getClientId();
            LOGGER.debugf("No stored claims found for scope '%s', user '%s', client '%s'", scope, username, clientId);
            return Collections.emptyList();
        } else {
            return storedClaims;
        }
    }
}
