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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
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
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilderFactory;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.ProofValidator;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSigner;
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
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.JwtProof;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.protocol.oid4vc.model.OfferUriType;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.utils.ClaimsPathPointer;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantType;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.saml.processing.api.util.DeflateUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.jboss.logging.Logger;

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
     * Session note key for storing credential configuration IDs from credential offer.
     * This allows the authorization details processor to easily retrieve the configuration IDs
     * without having to search through all session notes or parse the full credential offer.
     */
    public static final String CREDENTIAL_CONFIGURATION_IDS_NOTE = "CREDENTIAL_CONFIGURATION_IDS";

    /**
     * Prefix for session note keys that store the mapping between credential identifiers and configuration IDs.
     * This is used to store mappings generated during authorization details processing.
     */
    public static final String CREDENTIAL_IDENTIFIER_PREFIX = "credential_identifier_";

    /**
     * Prefix for session note keys that store authorization details claims.
     * This is used to store claims from authorization details for later use during credential issuance.
     */
    public static final String AUTHORIZATION_DETAILS_CLAIMS_PREFIX = "AUTHORIZATION_DETAILS_CLAIMS_";

    private Cors cors;

    private static final String CODE_LIFESPAN_REALM_ATTRIBUTE_KEY = "preAuthorizedCodeLifespanS";
    private static final int DEFAULT_CODE_LIFESPAN_S = 30;

    public static final String DEFLATE_COMPRESSION = "DEF";
    public static final String NONCE_PATH = "nonce";
    public static final String CREDENTIAL_PATH = "credential";
    public static final String CREDENTIAL_OFFER_PATH = "credential-offer/";
    public static final String RESPONSE_TYPE_IMG_PNG = OID4VCConstants.RESPONSE_TYPE_IMG_PNG;
    public static final String CREDENTIAL_OFFER_URI_CODE_SCOPE = OID4VCConstants.CREDENTIAL_OFFER_URI_CODE_SCOPE;
    private final KeycloakSession session;
    private final AppAuthManager.BearerTokenAuthenticator bearerTokenAuthenticator;
    private final TimeProvider timeProvider;

    // lifespan of the preAuthorizedCodes in seconds
    private final int preAuthorizedCodeLifeSpan;

    // constant for the OID4VCI enabled attribute key
    public static final String OID4VCI_ENABLED_ATTRIBUTE_KEY = "oid4vci.enabled";

    /**
     * Credential builders are responsible for initiating the production of
     * credentials in a specific format. Their output is an appropriate credential
     * representation to be signed by a credential signer of the same format.
     * <p></p>
     * Due to technical constraints, we explicitly load credential builders into
     * this map for they are configurable components. The key of the map is the
     * credential {@link Format} associated with the builder. The matching credential
     * signer is directly loaded from the Keycloak container.
     */
    private final Map<String, CredentialBuilder> credentialBuilders;

    public OID4VCIssuerEndpoint(KeycloakSession session,
                                Map<String, CredentialBuilder> credentialBuilders,
                                AppAuthManager.BearerTokenAuthenticator authenticator,
                                TimeProvider timeProvider,
                                int preAuthorizedCodeLifeSpan) {
        this.session = session;
        this.bearerTokenAuthenticator = authenticator;
        this.timeProvider = timeProvider;
        this.credentialBuilders = credentialBuilders;
        this.preAuthorizedCodeLifeSpan = preAuthorizedCodeLifeSpan;
    }

    public OID4VCIssuerEndpoint(KeycloakSession keycloakSession) {
        this.session = keycloakSession;
        this.bearerTokenAuthenticator = new AppAuthManager.BearerTokenAuthenticator(keycloakSession);
        this.timeProvider = new OffsetTimeProvider();

        this.credentialBuilders = loadCredentialBuilders(session);

        RealmModel realm = keycloakSession.getContext().getRealm();
        this.preAuthorizedCodeLifeSpan = Optional.ofNullable(realm.getAttribute(CODE_LIFESPAN_REALM_ATTRIBUTE_KEY))
                .map(Integer::valueOf)
                .orElse(DEFAULT_CODE_LIFESPAN_S);
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
     * Validates whether the authenticated client is enabled for OID4VCI features.
     * <p>
     * If the client is not enabled, this method logs the status and throws a
     * {@link CorsErrorResponseException} with an appropriate error message.
     * </p>
     *
     * @throws CorsErrorResponseException if the client is not enabled for OID4VCI.
     */
    private void checkClientEnabled() {
        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        ClientModel client = clientSession.getClient();

        boolean oid4vciEnabled = Boolean.parseBoolean(client.getAttributes().get(OID4VCI_ENABLED_ATTRIBUTE_KEY));

        if (!oid4vciEnabled) {
            LOGGER.debugf("Client '%s' is not enabled for OID4VCI features.", client.getClientId());
            throw new CorsErrorResponseException(
                    cors,
                    Errors.INVALID_CLIENT,
                    "Client not enabled for OID4VCI",
                    Response.Status.FORBIDDEN
            );
        }

        LOGGER.debugf("Client '%s' is enabled for OID4VCI features.", client.getClientId());
    }

    /**
     * Generates a unique notification ID for use in CredentialResponse.
     *
     * @return a unique string identifier
     */
    private String generateNotificationId() {
        return SecretGenerator.getInstance().randomString();
    }

    /**
     * the OpenId4VCI nonce-endpoint
     *
     * @return a short-lived c_nonce value that must be presented in key-bound proofs at the credential endpoint.
     * @see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-16.html#name-nonce-endpoint
     * @see https://datatracker.ietf.org/doc/html/draft-demarco-nonce-endpoint#name-nonce-response
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Path(NONCE_PATH)
    public Response getCNonce() {
        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
        NonceResponse nonceResponse = new NonceResponse();
        String sourceEndpoint = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
        String audience = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());

        // Generate c_nonce for the response body
        String bodyCNonce = cNonceHandler.buildCNonce(List.of(audience), Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, sourceEndpoint));

        // Generate separate DPoP nonce for the header
        String headerDPoPNonce = cNonceHandler.buildCNonce(List.of(audience), Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, sourceEndpoint));

        nonceResponse.setNonce(bodyCNonce);

        Response.ResponseBuilder responseBuilder = Response.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .entity(nonceResponse);

        if (headerDPoPNonce != null) {
            responseBuilder.header(OAuth2Constants.DPOP_NONCE_HEADER, headerDPoPNonce);
        }

        return responseBuilder.build();
    }

    /**
     * Handles CORS preflight requests for credential offer URI endpoint.
     * Preflight requests return CORS headers for all origins (standard CORS behavior).
     * The actual request will validate origins against client configuration.
     */
    @OPTIONS
    @Path("credential-offer-uri")
    public Response getCredentialOfferURIPreflight() {
        configureCors(true);
        cors.preflight();
        return cors.add(Response.ok());
    }

    /**
     * Provides the URI to the OID4VCI compliant credentials offer
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, RESPONSE_TYPE_IMG_PNG})
    @Path("credential-offer-uri")
    public Response getCredentialOfferURI(@QueryParam("credential_configuration_id") String vcId, @QueryParam("type") @DefaultValue("uri") OfferUriType type, @QueryParam("width") @DefaultValue("200") int width, @QueryParam("height") @DefaultValue("200") int height) {
        configureCors(true);

        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        cors.allowedOrigins(session, clientSession.getClient());
        checkClientEnabled();

        Map<String, SupportedCredentialConfiguration> credentialsMap = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
        LOGGER.debugf("Get an offer for %s", vcId);
        if (!credentialsMap.containsKey(vcId)) {
            LOGGER.debugf("No credential with id %s exists.", vcId);
            LOGGER.debugf("Supported credentials are %s.", credentialsMap);
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_CREDENTIAL_REQUEST.toString(),
                    "Invalid credential configuration ID",
                    Response.Status.BAD_REQUEST);
        }
        SupportedCredentialConfiguration supportedCredentialConfiguration = credentialsMap.get(vcId);

        // calculate the expiration of the preAuthorizedCode. The sessionCode will also expire at that time.
        int expiration = timeProvider.currentTimeSeconds() + preAuthorizedCodeLifeSpan;
        String preAuthorizedCode = generateAuthorizationCodeForClientSession(expiration, clientSession);

        CredentialsOffer theOffer = new CredentialsOffer()
                .setCredentialIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()))
                .setCredentialConfigurationIds(List.of(supportedCredentialConfiguration.getId()))
                .setGrants(
                        new PreAuthorizedGrant()
                                .setPreAuthorizedCode(
                                        new PreAuthorizedCode()
                                                .setPreAuthorizedCode(preAuthorizedCode)));

        String sessionCode = generateCodeForSession(expiration, clientSession);
        try {
            clientSession.setNote(sessionCode, JsonSerialization.mapper.writeValueAsString(theOffer));

            // Store the credential configuration IDs in a predictable location for token processing
            // This allows the authorization details processor to easily retrieve the configuration IDs
            // without having to search through all session notes or parse the full credential offer
            String credentialConfigIdsJson = JsonSerialization.mapper.writeValueAsString(theOffer.getCredentialConfigurationIds());
            clientSession.setNote(CREDENTIAL_CONFIGURATION_IDS_NOTE, credentialConfigIdsJson);
            LOGGER.debugf("Stored credential configuration IDs for token processing: %s", credentialConfigIdsJson);
        } catch (JsonProcessingException e) {
            LOGGER.errorf("Could not convert the offer POJO to JSON: %s", e.getMessage());
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_CREDENTIAL_REQUEST.toString(),
                    "Failed to process credential offer",
                    Response.Status.BAD_REQUEST);
        }

        return switch (type) {
            case URI -> getOfferUriAsUri(sessionCode);
            case QR_CODE -> getOfferUriAsQr(sessionCode, width, height);
        };
    }

    private Response getOfferUriAsUri(String sessionCode) {
        CredentialOfferURI credentialOfferURI = new CredentialOfferURI()
                .setIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH)
                .setNonce(sessionCode);

        return cors.add(Response.ok()
                .type(MediaType.APPLICATION_JSON)
                .entity(credentialOfferURI));
    }

    private Response getOfferUriAsQr(String sessionCode, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        String encodedOfferUri = URLEncoder.encode(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH + sessionCode, StandardCharsets.UTF_8);
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode("openid-credential-offer://?credential_offer_uri=" + encodedOfferUri, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
            return cors.add(Response.ok().type(RESPONSE_TYPE_IMG_PNG).entity(bos.toByteArray()));
        } catch (WriterException | IOException e) {
            LOGGER.warnf("Was not able to create a qr code of dimension %s:%s.", width, height, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Was not able to generate qr.").build();
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
    @Path(CREDENTIAL_OFFER_PATH + "{sessionCode}")
    public Response getCredentialOfferPreflight(@PathParam("sessionCode") String sessionCode) {
        configureCors(false);
        cors.preflight();
        return cors.add(Response.ok());
    }

    /**
     * Provides an OID4VCI compliant credential offer
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(CREDENTIAL_OFFER_PATH + "{sessionCode}")
    public Response getCredentialOffer(@PathParam("sessionCode") String sessionCode) {
        configureCors(false);

        if (sessionCode == null) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
        }

        CredentialsOffer credentialsOffer = getOfferFromSessionCode(sessionCode);
        LOGGER.debugf("Responding with offer: %s", credentialsOffer);

        return cors.add(Response.ok()
                .entity(credentialsOffer));
    }

    private void checkScope(CredentialScopeModel requestedCredential) {
        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        String vcIssuanceFlow = clientSession.getNote(PreAuthorizedCodeGrantType.VC_ISSUANCE_FLOW);

        if (vcIssuanceFlow == null || !vcIssuanceFlow.equals(PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE)) {
            AccessToken accessToken = bearerTokenAuthenticator.authenticate().token();
            if (Arrays.stream(accessToken.getScope().split(" "))
                    .noneMatch(tokenScope -> tokenScope.equals(requestedCredential.getScope()))) {
                LOGGER.debugf("Scope check failure: required scope = %s, " +
                                "scope in access token = %s.",
                        requestedCredential.getName(), accessToken.getScope());
                throw new CorsErrorResponseException(cors,
                        ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.toString(),
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
        LOGGER.debugf("Received credentials request with payload: %s", requestPayload);

        if (requestPayload == null || requestPayload.trim().isEmpty()) {
            String errorMessage = "Request payload is null or empty.";
            LOGGER.debug(errorMessage);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
        }

        cors = Cors.builder().auth().allowedMethods(HttpPost.METHOD_NAME).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        CredentialIssuer issuerMetadata = (CredentialIssuer) new OID4VCIssuerWellKnownProvider(session).getConfig();

        // Validate request encryption
        CredentialRequest credentialRequestVO = validateRequestEncryption(requestPayload, issuerMetadata);

        // Authenticate first to fail fast on auth errors
        AuthenticationManager.AuthResult authResult = getAuthResult();

        // Validate encryption parameters if present
        CredentialResponseEncryption encryptionParams = credentialRequestVO.getCredentialResponseEncryption();
        CredentialResponseEncryptionMetadata encryptionMetadata = OID4VCIssuerWellKnownProvider.getCredentialResponseEncryption(session);
        boolean isEncryptionRequired = Optional.ofNullable(encryptionMetadata)
                .map(CredentialResponseEncryptionMetadata::getEncryptionRequired)
                .orElse(false);

        // Check if encryption is required but not provided
        if (isEncryptionRequired && encryptionParams == null) {
            String errorMessage = "Response encryption is required by the Credential Issuer, but no encryption parameters were provided.";
            LOGGER.debug(errorMessage);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
        }

        // Validate encryption parameters if provided
        if (encryptionParams != null) {
            validateEncryptionParameters(encryptionParams);

            // Select and validate alg
            String selectedAlg = selectKeyManagementAlg(encryptionMetadata, encryptionParams.getJwk());
            if (selectedAlg == null) {
                String errorMessage = String.format("No supported key management algorithm (alg) for provided JWK (kty=%s)",
                        encryptionParams.getJwk().getKeyType());
                LOGGER.debug(errorMessage);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            // Check if enc is supported
            if (!encryptionMetadata.getEncValuesSupported().contains(encryptionParams.getEnc())) {
                String errorMessage = String.format("Unsupported content encryption algorithm: enc=%s",
                        encryptionParams.getEnc());
                LOGGER.debug(errorMessage);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            // Check compression (unchanged)
            if (encryptionParams.getZip() != null &&
                    !isSupportedCompression(encryptionMetadata, encryptionParams.getZip())) {
                String errorMessage = String.format("Unsupported compression parameter: zip=%s",
                        encryptionParams.getZip());
                LOGGER.debug(errorMessage);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }
        }

        // checkClientEnabled call after authentication
        checkClientEnabled();

        // Both credential_configuration_id and credential_identifier are optional.
        // If the credential_configuration_id is present, credential_identifier can't be present.
        // But this implementation will tolerate the presence of both, waiting for clarity in specifications.
        // This implementation will privilege the presence of the credential_configuration_id.
        String requestedCredentialConfigurationId = credentialRequestVO.getCredentialConfigurationId();
        String requestedCredentialIdentifier = credentialRequestVO.getCredentialIdentifier();

        // Check if at least one of both is available.
        if (requestedCredentialConfigurationId == null && requestedCredentialIdentifier == null) {
            LOGGER.debugf("Missing both credential_configuration_id and credential_identifier. At least one must be specified.");
            throw new BadRequestException(getErrorResponse(ErrorType.MISSING_CREDENTIAL_IDENTIFIER_AND_CONFIGURATION_ID));
        }

        CredentialScopeModel requestedCredential;

        // If credential_identifier is provided, retrieve the mapping from client session
        if (credentialRequestVO.getCredentialIdentifier() != null) {
            String mappingKey = CREDENTIAL_IDENTIFIER_PREFIX + credentialRequestVO.getCredentialIdentifier();

            // First try to get the client session and look for the mapping there
            UserSessionModel userSession = authResult.session();
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(authResult.client().getId());
            String mappedCredentialConfigurationId = null;

            if (clientSession != null) {
                mappedCredentialConfigurationId = clientSession.getNote(mappingKey);
                if (mappedCredentialConfigurationId != null) {
                    LOGGER.debugf("Found credential configuration ID mapping in client session for identifier %s: %s",
                            credentialRequestVO.getCredentialIdentifier(), mappedCredentialConfigurationId);
                }
            }

            if (mappedCredentialConfigurationId != null) {
                // Use the mapped credential configuration ID to find the credential scope
                Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
                if (supportedCredentials.containsKey(mappedCredentialConfigurationId)) {
                    SupportedCredentialConfiguration config = supportedCredentials.get(mappedCredentialConfigurationId);
                    ClientModel client = session.getContext().getClient();
                    Map<String, ClientScopeModel> clientScopes = client.getClientScopes(false);
                    ClientScopeModel clientScope = clientScopes.get(config.getScope());

                    if (clientScope != null) {
                        requestedCredential = new CredentialScopeModel(clientScope);
                        LOGGER.debugf("Successfully mapped credential identifier %s to configuration %s",
                                credentialRequestVO.getCredentialIdentifier(), mappedCredentialConfigurationId);
                    } else {
                        LOGGER.errorf("Client scope not found for mapped credential configuration: %s", config.getScope());
                        throw new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION));
                    }
                } else {
                    LOGGER.errorf("Mapped credential configuration not found: %s", mappedCredentialConfigurationId);
                    throw new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION));
                }
            } else {
                // No mapping found, try to use credential_identifier as a direct scope name
                LOGGER.debugf("No mapping found for credential identifier %s, trying direct scope lookup",
                        credentialRequestVO.getCredentialIdentifier());
                try {
                    requestedCredential = credentialRequestVO.findCredentialScope(session).orElseThrow(() -> {
                        LOGGER.errorf("Credential scope not found for identifier: %s", credentialRequestVO.getCredentialIdentifier());
                        return new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER));
                    });
                } catch (Exception e) {
                    LOGGER.errorf(e, "Failed to find credential scope for identifier: %s", credentialRequestVO.getCredentialIdentifier());
                    throw new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER));
                }
            }
        } else if (credentialRequestVO.getCredentialConfigurationId() != null) {
            // Use credential_configuration_id for direct lookup
            requestedCredential = credentialRequestVO.findCredentialScope(session).orElseThrow(() -> {
                LOGGER.errorf("Credential scope not found for configuration ID: %s", credentialRequestVO.getCredentialConfigurationId());
                return new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION));
            });
        } else {
            // Neither provided - this should not happen due to earlier validation
            throw new BadRequestException(getErrorResponse(ErrorType.MISSING_CREDENTIAL_IDENTIFIER_AND_CONFIGURATION_ID));
        }

        checkScope(requestedCredential);

        SupportedCredentialConfiguration supportedCredential =
                OID4VCIssuerWellKnownProvider.toSupportedCredentialConfiguration(session, requestedCredential);

        // Get the list of all proofs (handles single proof, multiple proofs, or none)
        List<String> allProofs = getAllProofs(credentialRequestVO);

        // Generate credential response
        CredentialResponse responseVO = new CredentialResponse();
        responseVO.setNotificationId(generateNotificationId());

        if (allProofs.isEmpty()) {
            // Single issuance without proof
            Object theCredential = getCredential(authResult, supportedCredential, credentialRequestVO);
            responseVO.addCredential(theCredential);
        } else {
            // Issue credentials for each proof
            Proofs originalProofs = credentialRequestVO.getProofs();
            for (String currentProof : allProofs) {
                credentialRequestVO.setProofs(new Proofs().setJwt(List.of(currentProof)));
                Object theCredential = getCredential(authResult, supportedCredential, credentialRequestVO);
                responseVO.addCredential(theCredential);
            }
            credentialRequestVO.setProofs(originalProofs);
        }

        // Encrypt all responses if encryption parameters are provided, except for error credential responses
        if (encryptionParams != null && !responseVO.getCredentials().isEmpty()) {
            String jwe = encryptCredentialResponse(responseVO, encryptionParams, encryptionMetadata);
            return Response.ok()
                    .type(MediaType.APPLICATION_JWT)
                    .entity(jwe)
                    .build();
        }
        return Response.ok().entity(responseVO).build();
    }

    private CredentialRequest validateRequestEncryption(String requestPayload, CredentialIssuer issuerMetadata) throws BadRequestException {
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

        if (isRequestEncryptionRequired || contentTypeIsJwt) {
            if (requestEncryptionMetadata == null && contentTypeIsJwt) {
                String errorMessage = "Received JWT content-type request, but credential_request_encryption is not supported.";
                LOGGER.debug(errorMessage);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            try {
                return decryptCredentialRequest(requestPayload, requestEncryptionMetadata);
            } catch (Exception e) {
                if (isRequestEncryptionRequired) {
                    String errorMessage = "Encryption is required but request is not a valid JWE: " + e.getMessage();
                    LOGGER.debug(errorMessage);
                    throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
                }
                if (contentTypeIsJwt) {
                    String errorMessage = "Request has JWT content-type but is not a valid JWE: " + e.getMessage();
                    LOGGER.debug(errorMessage);
                    throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
                }
            }
        }

        try {
            CredentialRequest credentialRequest = JsonSerialization.mapper.readValue(requestPayload, CredentialRequest.class);
            normalizeProofFields(credentialRequest);
            return credentialRequest;
        } catch (JsonProcessingException e) {
            String errorMessage = "Failed to parse JSON request: " + e.getMessage();
            LOGGER.debug(errorMessage);
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
            LOGGER.debugf(errorMessage);
            throw new JWEException(String.valueOf(ErrorType.INVALID_ENCRYPTION_PARAMETERS));
        }

        // Handle compression if present
        String zip = header.getCompressionAlgorithm();
        if (zip != null) {
            if (!DEFLATE_COMPRESSION.equals(zip) || metadata.getZipValuesSupported() == null || !metadata.getZipValuesSupported().contains(zip)) {
                String errorMessage = String.format("Unsupported compression algorithm: zip=%s", zip);
                LOGGER.debugf(errorMessage);
                throw new JWEException(String.valueOf(ErrorType.INVALID_ENCRYPTION_PARAMETERS));
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
            JwtProof singleProof = credentialRequest.getProof();
            Proofs proofsArray = new Proofs();
            if (singleProof.getJwt() != null) {
                proofsArray.setJwt(List.of(singleProof.getJwt()));
            }
            credentialRequest.setProofs(proofsArray);
            credentialRequest.setProof(null);
        }
    }

    private String selectKeyManagementAlg(CredentialResponseEncryptionMetadata metadata, JWK jwk) {
        List<String> supportedAlgs = metadata.getAlgValuesSupported();
        if (supportedAlgs == null || supportedAlgs.isEmpty()) {
            return null;
        }

        // The alg parameter MUST be present in the JWK
        String jwkAlg = jwk.getAlgorithm();
        if (jwkAlg == null) {
            // If alg is missing from JWK, this is invalid
            LOGGER.debugf("JWK is missing required 'alg' parameter for key type: %s", jwk.getKeyType());
            return null;
        }

        // Verify the alg is supported by the server
        if (supportedAlgs.contains(jwkAlg)) {
            return jwkAlg;
        }

        // If the JWK's alg is not supported, we cannot proceed
        LOGGER.debugf("JWK algorithm '%s' is not supported by the server. Supported algorithms: %s",
                jwkAlg, supportedAlgs);
        throw new IllegalArgumentException(String.format("JWK algorithm '%s' is not supported. Supported algorithms: %s", jwkAlg, supportedAlgs));

    }

    private List<String> getAllProofs(CredentialRequest credentialRequestVO) {
        List<String> allProofs = new ArrayList<>();

        Proofs proofs = credentialRequestVO.getProofs();
        if (proofs == null) {
            return allProofs; // No proofs provided
        }

        if (proofs.getJwt() == null || proofs.getJwt().isEmpty()) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_PROOF,
                    "The 'proofs' object must contain exactly one proof type with non-empty array."));
        }

        allProofs.addAll(proofs.getJwt());
        return allProofs;
    }

    /**
     * Encrypts a CredentialResponse as a JWE using the provided encryption parameters.
     *
     * @param response The CredentialResponse to encrypt
     * @param encryptionParams The encryption parameters (alg, enc, jwk)
     * @return The compact JWE serialization
     * @throws BadRequestException If encryption parameters are invalid
     * @throws WebApplicationException If encryption fails due to server issues
     */
    private String encryptCredentialResponse(CredentialResponse response,
                                             CredentialResponseEncryption encryptionParams,
                                             CredentialResponseEncryptionMetadata metadata) {
        validateEncryptionParameters(encryptionParams);

        String enc = encryptionParams.getEnc();
        String zip = encryptionParams.getZip();
        JWK jwk = encryptionParams.getJwk();

        // Parse public key
        PublicKey publicKey;
        try {
            publicKey = JWKParser.create(jwk).toPublicKey();
            if (publicKey == null) {
                LOGGER.debug("Invalid JWK: Failed to parse public key");
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS,
                        "Invalid JWK: Failed to parse public key."));
            }
        } catch (Exception e) {
            LOGGER.debugf("Failed to parse JWK: %s", e.getMessage());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS,
                    "Invalid JWK: Failed to parse public key."));
        }

        // Select alg
        String selectedAlg = selectKeyManagementAlg(metadata, jwk);

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

    private AuthenticatedClientSessionModel getAuthenticatedClientSession() {
        AuthenticationManager.AuthResult authResult = getAuthResult();
        UserSessionModel userSessionModel = authResult.session();

        AuthenticatedClientSessionModel clientSession = userSessionModel.
                getAuthenticatedClientSessionByClient(
                        authResult.client().getId());
        if (clientSession == null) {
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_TOKEN.toString(),
                    "Invalid or missing token",
                    Response.Status.BAD_REQUEST);
        }
        return clientSession;
    }

    private AuthenticationManager.AuthResult getAuthResult() {
        AuthenticationManager.AuthResult authResult = bearerTokenAuthenticator.authenticate();
        if (authResult == null) {
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_TOKEN.toString(),
                    "Invalid or missing token",
                    Response.Status.BAD_REQUEST);
        }

        // Validate DPoP nonce if present in the DPoP proof
        DPoP dPoP = session.getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE, DPoP.class);
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
                            ErrorType.INVALID_TOKEN.toString(),
                            "Invalid or missing token",
                            Response.Status.BAD_REQUEST);
                }
            }
        }

        return authResult;
    }

    // get the auth result from the authentication manager
    private AuthenticationManager.AuthResult getAuthResult(WebApplicationException errorResponse) {
        AuthenticationManager.AuthResult authResult = bearerTokenAuthenticator.authenticate();
        if (authResult == null) {
            throw errorResponse;
        }
        return authResult;
    }

    /**
     * Get a signed credential
     *
     * @param authResult          authResult containing the userSession to create the credential for
     * @param credentialConfig    the supported credential configuration
     * @param credentialRequestVO the credential request
     * @return the signed credential
     */
    private Object getCredential(AuthenticationManager.AuthResult authResult,
                                 SupportedCredentialConfiguration credentialConfig,
                                 CredentialRequest credentialRequestVO
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

        VCIssuanceContext vcIssuanceContext = getVCToSign(protocolMappers, credentialConfig, authResult, credentialRequestVO);

        // Enforce key binding prior to signing if necessary
        enforceKeyBindingIfProofProvided(vcIssuanceContext);

        // Retrieve matching credential signer
        CredentialSigner<?> credentialSigner = session.getProvider(CredentialSigner.class,
                credentialConfig.getFormat());

        return Optional.ofNullable(credentialSigner)
                .map(signer -> signer.signCredential(
                        vcIssuanceContext.getCredentialBody(),
                        credentialConfig.getCredentialBuildConfig()
                ))
                .orElseThrow(() -> {
                    String message = String.format("No signer found for format '%s'.", credentialConfig.getFormat());
                    return new BadRequestException(
                            message,
                            getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, message)
                    );
                });
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

    private String generateCodeForSession(int expiration, AuthenticatedClientSessionModel clientSession) {
        String codeId = SecretGenerator.getInstance().randomString();
        String nonce = SecretGenerator.getInstance().randomString();
        OAuth2Code oAuth2Code = new OAuth2Code(codeId, expiration, nonce, CREDENTIAL_OFFER_URI_CODE_SCOPE, null, null, null, null,
                clientSession.getUserSession().getId());

        return OAuth2CodeParser.persistCode(session, clientSession, oAuth2Code);
    }

    private CredentialsOffer getOfferFromSessionCode(String sessionCode) {
        EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session,
                session.getContext().getConnection());
        OAuth2CodeParser.ParseResult result = OAuth2CodeParser.parseCode(session, sessionCode,
                session.getContext().getRealm(),
                eventBuilder);
        if (result.isExpiredCode() || result.isIllegalCode() || !result.getCodeData().getScope().equals(CREDENTIAL_OFFER_URI_CODE_SCOPE)) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_TOKEN));
        }
        try {
            String offer = result.getClientSession().getNote(sessionCode);
            return JsonSerialization.mapper.readValue(offer, CredentialsOffer.class);
        } catch (JsonProcessingException e) {
            LOGGER.errorf("Could not convert JSON to POJO: %s", e);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_TOKEN));
        } finally {
            result.getClientSession().removeNote(sessionCode);
        }
    }

    private String generateAuthorizationCodeForClientSession(int expiration, AuthenticatedClientSessionModel clientSessionModel) {
        return PreAuthorizedCodeGrantType.getPreAuthorizedCode(session, clientSessionModel, expiration);
    }

    private Response getErrorResponse(ErrorType errorType) {
        return getErrorResponse(errorType, null);
    }

    private Response getErrorResponse(ErrorType errorType, String errorDescription) {
        var errorResponse = new ErrorResponse();
        errorResponse.setError(errorType).setErrorDescription(errorDescription);
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // builds the unsigned credential by applying all protocol mappers.
    private VCIssuanceContext getVCToSign(List<OID4VCMapper> protocolMappers, SupportedCredentialConfiguration credentialConfig,
                                          AuthenticationManager.AuthResult authResult, CredentialRequest credentialRequestVO) {
        // set the required claims
        VerifiableCredential vc = new VerifiableCredential()
                .setIssuanceDate(Instant.ofEpochMilli(timeProvider.currentTimeMillis()))
                .setType(List.of(credentialConfig.getScope()));

        Map<String, Object> subjectClaims = new HashMap<>();
        protocolMappers
                .forEach(mapper -> mapper.setClaimsForSubject(subjectClaims, authResult.session()));

        // Validate that requested claims from authorization_details are present
        validateRequestedClaimsArePresent(subjectClaims, authResult.session(), credentialConfig.getScope());

        // Include all available claims
        subjectClaims.forEach((key, value) -> vc.getCredentialSubject().setClaims(key, value));

        protocolMappers
                .forEach(mapper -> mapper.setClaimsForCredential(vc, authResult.session()));

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

        // Validate each JWT proof if present
        if (proofs.getJwt() != null && !proofs.getJwt().isEmpty()) {
            validateProofs(vcIssuanceContext, ProofType.JWT);
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
            if (e.getErrorType() == ErrorType.INVALID_NONCE) {
                throw new ErrorResponseException(
                        ErrorType.INVALID_NONCE.getValue(),
                        "The proofs parameter in the Credential Request uses an invalid nonce",
                        Response.Status.BAD_REQUEST
                );
            }
            throw new BadRequestException("Could not validate provided proof", e);
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
     * @param allClaims   all available claims
     * @param userSession the user session
     * @param scope       the credential scope
     * @throws BadRequestException if mandatory requested claims are missing
     */
    private void validateRequestedClaimsArePresent(Map<String, Object> allClaims, UserSessionModel userSession, String scope) {
        try {
            // Look for stored claims in user session notes
            String claimsKey = AUTHORIZATION_DETAILS_CLAIMS_PREFIX + scope;
            String storedClaimsJson = userSession.getNote(claimsKey);

            if (storedClaimsJson != null && !storedClaimsJson.isEmpty()) {
                try {
                    // Parse the stored claims from JSON
                    List<ClaimsDescription> storedClaims =
                            JsonSerialization.readValue(storedClaimsJson,
                                    new TypeReference<List<ClaimsDescription>>() {
                                    });

                    if (storedClaims != null && !storedClaims.isEmpty()) {
                        // Validate that all requested claims are present in the available claims
                        // We use filterClaimsByAuthorizationDetails to check if claims can be found
                        // but we don't actually filter - we just validate presence
                        try {
                            ClaimsPathPointer.filterClaimsByAuthorizationDetails(allClaims, storedClaims);
                            LOGGER.debugf("All requested claims are present for scope %s", scope);
                        } catch (IllegalArgumentException e) {
                            // If filtering fails, it means some requested claims are missing
                            LOGGER.errorf("Requested claims validation failed for scope %s: %s", scope, e.getMessage());
                            throw new BadRequestException("Credential issuance failed: " + e.getMessage() +
                                    ". The requested claims are not available in the user profile.");
                        }
                    } else {
                        LOGGER.infof("Stored claims list is null or empty");
                    }
                } catch (Exception e) {
                    LOGGER.errorf(e, "Failed to parse stored claims for scope %s", scope);
                }
            } else {
                LOGGER.infof("No stored claims found for scope %s", scope);
            }
            // No claims filtering requested, all claims are valid

        } catch (IllegalArgumentException e) {
            // Mandatory claim missing - this should fail credential issuance
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Mandatory claim not found:")) {
                LOGGER.errorf("Mandatory claim missing during claims filtering for scope %s: %s", scope, errorMessage);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST,
                        "Credential issuance failed: " + errorMessage +
                                ". The requested mandatory claim is not available in the user profile."));
            } else {
                LOGGER.errorf("Claims filtering error for scope %s: %s", scope, errorMessage);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST,
                        "Credential issuance failed: " + errorMessage));
            }
        } catch (BadRequestException e) {
            // Re-throw BadRequestException to ensure client receives proper error response
            throw e;
        } catch (Exception e) {
            // Log error but continue with all claims to avoid breaking existing functionality
            LOGGER.errorf(e, "Unexpected error during claims validation for scope %s, continuing with all claims", scope);
        }
    }
}
