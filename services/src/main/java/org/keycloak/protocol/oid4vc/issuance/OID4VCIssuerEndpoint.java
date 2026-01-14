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
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.SecretGenerator;
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
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilderFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage.CredentialOfferState;
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
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.JwtProof;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.protocol.oid4vc.model.OfferUriType;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.utils.ClaimsPathPointer;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantType;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
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

import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_CREDENTIAL_REQUEST;
import static org.keycloak.protocol.oid4vc.model.ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION;
import static org.keycloak.protocol.oid4vc.model.ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER;

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
     * Prefix for session note keys that store authorization details claims.
     * This is used to store claims from authorization details for later use during credential issuance.
     */
    public static final String AUTHORIZATION_DETAILS_CLAIMS_PREFIX = "AUTHORIZATION_DETAILS_CLAIMS_";

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
        RealmModel realm = session.getContext().getRealm();
        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_NONCE_REQUEST);

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
     * Creates a Credential Offer Uri that is bound to the calling user.
     */
    public Response getCredentialOfferURI(String credConfigId) {
        UserSessionModel userSession = getAuthenticatedClientSession().getUserSession();
        return getCredentialOfferURI(credConfigId, true, userSession.getLoginUsername());
    }

    /**
     * Creates a Credential Offer Uri that is bound to a specific user.
     */
    public Response getCredentialOfferURI(String credConfigId, boolean preAuthorized, String targetUser) {
        return getCredentialOfferURI(credConfigId, preAuthorized, null, targetUser, OfferUriType.URI, 0, 0);
    }

    /**
     * Creates a Credential Offer Uri that can be pre-authorized and hence bound to a specific client/user id.
     * <p>
     * Credential Offer Validity Matrix for the supported request parameters "pre_authorized", "client_id", "username" combinations.
     * </p>
     * +----------+-----------+---------+---------+-----------------------------------------------------+
     * | pre-auth | clientId  | username  | Valid   | Notes                                               |
     * +----------+-----------+---------+---------+-----------------------------------------------------+
     * | no       | no        | no      | yes     | Generic offer; any logged-in user may redeem.       |
     * | no       | no        | yes     | yes     | Offer restricted to a specific user.                |
     * | no       | yes       | no      | yes     | Bound to client; user determined at login.          |
     * | no       | yes       | yes     | yes     | Bound to both client and user.                      |
     * +----------+-----------+---------+---------+-----------------------------------------------------+
     * | yes      | no        | no      | no      | Pre-auth requires a user subject; missing username.   |
     * | yes      | yes       | no      | no      | Same as above; username required.                     |
     * | yes      | no        | yes     | yes     | Pre-auth for a specific user; client unconstrained. |
     * | yes      | yes       | yes     | yes     | Fully constrained: user + client.                   |
     * +----------+-----------+---------+---------+-----------------------------------------------------+
     *
     * @param credConfigId  A valid credential configuration id
     * @param preAuthorized A flag whether the offer should be pre-authorized (requires targetUser)
     * @param appClientId   The client id that the offer is authorized for
     * @param appUsername   The username that the offer is authorized for
     * @param type          The response type, which can be 'uri' or 'qr-code'
     * @param width         The width of the QR code image
     * @param height        The height of the QR code image
     * @see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer-endpoint
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, RESPONSE_TYPE_IMG_PNG})
    @Path("credential-offer-uri")
    public Response getCredentialOfferURI(
            @QueryParam("credential_configuration_id") String credConfigId,
            @QueryParam("pre_authorized") @DefaultValue("true") boolean preAuthorized,
            @QueryParam("client_id") String appClientId,
            @QueryParam("username") String appUsername,
            @QueryParam("type") @DefaultValue("uri") OfferUriType type,
            @QueryParam("width") @DefaultValue("200") int width,
            @QueryParam("height") @DefaultValue("200") int height
    ) {
        configureCors(true);

        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        UserModel userModel = clientSession.getUserSession().getUser();
        ClientModel clientModel = clientSession.getClient();
        RealmModel realmModel = clientModel.getRealm();

        EventBuilder eventBuilder = new EventBuilder(realmModel, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST)
                .client(clientModel)
                .user(userModel)
                .session(clientSession.getUserSession().getId())
                .detail(Details.USERNAME, userModel.getUsername())
                .detail(Details.CREDENTIAL_TYPE, credConfigId);

        cors.allowedOrigins(session, clientModel);
        checkClientEnabled();

        // Check required role to create a credential offer
        //
        boolean hasCredentialOfferRole = userModel.getRoleMappingsStream()
                .anyMatch(rm -> rm.getName().equals(CREDENTIAL_OFFER_CREATE.getName()));
        if (!hasCredentialOfferRole) {
            var errorMessage = "Credential offer creation requires role: " + CREDENTIAL_OFFER_CREATE.getName();
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors,
                    INVALID_CREDENTIAL_OFFER_REQUEST.toString(), errorMessage, Response.Status.FORBIDDEN);
        }

        LOGGER.debugf("Get an offer for %s", credConfigId);

        // Check whether given client/user ids actually exist
        //
        if (appClientId != null && session.clients().getClientByClientId(realmModel, appClientId) == null) {
            var errorMessage = "No such client id: " + appClientId;
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.CLIENT_NOT_FOUND);
            throw new CorsErrorResponseException(cors,
                    INVALID_CREDENTIAL_OFFER_REQUEST.toString(), errorMessage, Response.Status.BAD_REQUEST);
        }

        String userId = null;
        if (appUsername != null) {
            UserModel user = session.users().getUserByUsername(realmModel, appUsername);
            if (user == null) {
                var errorMessage = "Not found user with username: " + appUsername;
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.USER_NOT_FOUND);
                throw new CorsErrorResponseException(cors,
                        INVALID_CREDENTIAL_OFFER_REQUEST.toString(), errorMessage, Response.Status.BAD_REQUEST);
            }
            if (!user.isEnabled()) {
                var errorMessage = "User '" + appUsername + "' disabled";
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.USER_DISABLED);
                throw new CorsErrorResponseException(cors,
                        INVALID_CREDENTIAL_OFFER_REQUEST.toString(), errorMessage, Response.Status.BAD_REQUEST);
            }
            userId = user.getId();
        }

        if (preAuthorized) {
            if (appClientId == null) {
                appClientId = clientModel.getClientId();
                LOGGER.warnf("Using fallback client id for credential offer: %s", appClientId);
            }
            if (appUsername == null) {
                var errorMessage = "Pre-Authorized credential offer requires a target user";
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors,
                        INVALID_CREDENTIAL_OFFER_REQUEST.toString(), errorMessage, Response.Status.BAD_REQUEST);
            }
        }

        // Check whether the credential configuration exists in available client scopes
        //
        List<String> availableInClientScopes = session.clientScopes()
                .getClientScopesByProtocol(realmModel, OID4VC_PROTOCOL)
                .map(it -> it.getAttribute(CredentialScopeModel.CONFIGURATION_ID))
                .toList();
        if (!availableInClientScopes.contains(credConfigId)) {
            var errorMessage = "Invalid credential configuration id: " + credConfigId;
            LOGGER.debugf("%s not found in supported credential config ids: %s", credConfigId, availableInClientScopes);
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors,
                    INVALID_CREDENTIAL_OFFER_REQUEST.toString(), errorMessage, Response.Status.BAD_REQUEST);
        }

        CredentialsOffer credOffer = new CredentialsOffer()
                .setCredentialIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()))
                .setCredentialConfigurationIds(List.of(credConfigId));

        int expiration = timeProvider.currentTimeSeconds() + preAuthorizedCodeLifeSpan;
        CredentialOfferState offerState = new CredentialOfferState(credOffer, appClientId, userId, expiration);

        if (preAuthorized) {
            String code = "urn:oid4vci:code:" + SecretGenerator.getInstance().randomString(64);
            credOffer.setGrants(new PreAuthorizedGrant().setPreAuthorizedCode(
                    new PreAuthorizedCode().setPreAuthorizedCode(code)));
        }

        CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
        offerStorage.putOfferState(session, offerState);

        LOGGER.debugf("Stored credential offer state: [ids=%s, cid=%s, uid=%s, nonce=%s]",
                credOffer.getCredentialConfigurationIds(), offerState.getClientId(), offerState.getUserId(), offerState.getNonce());

        // Store the credential configuration IDs in a predictable location for token processing
        // This allows the authorization details processor to easily retrieve the configuration IDs
        // without having to search through all session notes or parse the full credential offer
        String credentialConfigIdsJson = JsonSerialization.valueAsString(credOffer.getCredentialConfigurationIds());
        clientSession.setNote(CREDENTIAL_CONFIGURATION_IDS_NOTE, credentialConfigIdsJson);
        LOGGER.debugf("Stored credential configuration IDs for token processing: %s", credentialConfigIdsJson);

        // Add event details
        eventBuilder.detail(Details.VERIFIABLE_CREDENTIAL_PRE_AUTHORIZED, String.valueOf(preAuthorized))
                .detail(Details.RESPONSE_TYPE, type.toString());
        if (appClientId != null) {
            eventBuilder.detail(Details.VERIFIABLE_CREDENTIAL_TARGET_CLIENT_ID, appClientId);
        }
        if (userId != null) {
            eventBuilder.detail(Details.VERIFIABLE_CREDENTIAL_TARGET_USER_ID, userId);
        }
        eventBuilder.success();

        return switch (type) {
            case URI -> getOfferUriAsUri(offerState.getNonce());
            case QR_CODE -> getOfferUriAsQr(offerState.getNonce(), width, height);
        };
    }

    private Response getOfferUriAsUri(String nonce) {
        CredentialOfferURI credentialOfferURI = new CredentialOfferURI()
                .setIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH)
                .setNonce(nonce);

        return cors.add(Response.ok()
                .type(MediaType.APPLICATION_JSON)
                .entity(credentialOfferURI));
    }

    private Response getOfferUriAsQr(String nonce, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        String encodedOfferUri = URLEncoder.encode(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH + nonce, StandardCharsets.UTF_8);
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
    @Path(CREDENTIAL_OFFER_PATH + "{nonce}")
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
    @Path(CREDENTIAL_OFFER_PATH + "{nonce}")
    public Response getCredentialOffer(@PathParam("nonce") String nonce) {
        configureCors(false);

        if (nonce == null) {
            var errorMessage = "No credential offer nonce";
            throw new BadRequestException(getErrorResponse(INVALID_CREDENTIAL_OFFER_REQUEST, errorMessage));
        }

        RealmModel realm = session.getContext().getRealm();

        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST);

        // Retrieve the associated credential offer state
        //
        CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
        CredentialOfferState offerState = offerStorage.findOfferStateByNonce(session, nonce);
        if (offerState == null) {
            var errorMessage = "No credential offer state for nonce: " + nonce;
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
            throw new BadRequestException(getErrorResponse(INVALID_CREDENTIAL_OFFER_REQUEST, errorMessage));
        }

        // We treat the credential offer URI as an unprotected capability URL and rely solely on the later authorization step
        // i.e. an authenticated client/user session is not required nor checked against the offer state

        CredentialsOffer credOffer = offerState.getCredentialsOffer();
        LOGGER.debugf("Found credential offer state: [ids=%s, cid=%s, uid=%s, nonce=%s]",
                credOffer.getCredentialConfigurationIds(), offerState.getClientId(), offerState.getUserId(), offerState.getNonce());

        if (offerState.isExpired()) {
            var errorMessage = "Credential offer already expired";
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.EXPIRED_CODE);
            throw new BadRequestException(getErrorResponse(INVALID_CREDENTIAL_OFFER_REQUEST, errorMessage));
        }

        // Add event details
        if (offerState.getClientId() != null) {
            eventBuilder.client(offerState.getClientId());
        }
        if (offerState.getUserId() != null) {
            eventBuilder.user(offerState.getUserId());
        }
        if (credOffer.getCredentialConfigurationIds() != null && !credOffer.getCredentialConfigurationIds().isEmpty()) {
            eventBuilder.detail(Details.CREDENTIAL_TYPE, String.join(",", credOffer.getCredentialConfigurationIds()));
        }

        LOGGER.debugf("Responding with offer: %s", JsonSerialization.valueAsString(credOffer));

        eventBuilder.success();
        return cors.add(Response.ok().entity(credOffer));
    }

    private void checkScope(CredentialScopeModel requestedCredential) {
        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        String vcIssuanceFlow = clientSession.getNote(PreAuthorizedCodeGrantType.VC_ISSUANCE_FLOW);

        if (vcIssuanceFlow == null || !vcIssuanceFlow.equals(PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE)) {
            // Use getAuthResult() instead of bearerTokenAuthenticator.authenticate() directly
            // This ensures we benefit from the cachedAuthResult caching that prevents DPoP proof reuse
            AccessToken accessToken = getAuthResult().token();
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

        RealmModel realm = session.getContext().getRealm();
        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
        eventBuilder.event(EventType.VERIFIABLE_CREDENTIAL_REQUEST);

        if (requestPayload == null || requestPayload.trim().isEmpty()) {
            String errorMessage = "Request payload is null or empty.";
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
            throw new BadRequestException(getErrorResponse(INVALID_CREDENTIAL_REQUEST, errorMessage));
        }

        cors = Cors.builder().auth().allowedMethods(HttpPost.METHOD_NAME).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        CredentialIssuer issuerMetadata = (CredentialIssuer) new OID4VCIssuerWellKnownProvider(session).getConfig();

        // Validate request encryption
        CredentialRequest credentialRequestVO;
        try {
            credentialRequestVO = validateRequestEncryption(requestPayload, issuerMetadata, eventBuilder);
        } catch (BadRequestException e) {
            // Event tracking already handled in validateRequestEncryption
            throw e;
        }

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
        CredentialResponseEncryption encryptionParams = credentialRequestVO.getCredentialResponseEncryption();
        CredentialResponseEncryptionMetadata encryptionMetadata = OID4VCIssuerWellKnownProvider.getCredentialResponseEncryption(session);
        boolean isEncryptionRequired = Optional.ofNullable(encryptionMetadata)
                .map(CredentialResponseEncryptionMetadata::getEncryptionRequired)
                .orElse(false);

        // Check if encryption is required but not provided
        if (isEncryptionRequired && encryptionParams == null) {
            String errorMessage = "Response encryption is required by the Credential Issuer, but no encryption parameters were provided.";
            LOGGER.debug(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage)
                    .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
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
                eventBuilder.detail(Details.REASON, errorMessage)
                        .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            // Check if enc is supported
            if (!encryptionMetadata.getEncValuesSupported().contains(encryptionParams.getEnc())) {
                String errorMessage = String.format("Unsupported content encryption algorithm: enc=%s",
                        encryptionParams.getEnc());
                LOGGER.debug(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage)
                        .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }

            // Check compression (unchanged)
            if (encryptionParams.getZip() != null &&
                    !isSupportedCompression(encryptionMetadata, encryptionParams.getZip())) {
                String errorMessage = String.format("Unsupported compression parameter: zip=%s",
                        encryptionParams.getZip());
                LOGGER.debug(errorMessage);
                eventBuilder.detail(Details.REASON, errorMessage)
                        .error(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue());
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_ENCRYPTION_PARAMETERS, errorMessage));
            }
        }

        // checkClientEnabled call after authentication
        checkClientEnabled();

        // Both credential_configuration_id and credential_identifier are optional.
        // If the credential_configuration_id is present, credential_identifier can't be present.
        // But this implementation will tolerate the presence of both, waiting for clarity in specifications.
        // This implementation will privilege the presence of credential_identifier.

        String credentialIdentifier = credentialRequestVO.getCredentialIdentifier();
        String credentialConfigurationId = credentialRequestVO.getCredentialConfigurationId();

        // Check if at least one of both is available.
        if (credentialIdentifier == null && credentialConfigurationId == null) {
            String errorMessage = "Missing both credential_configuration_id and credential_identifier. At least one must be specified.";
            LOGGER.debugf(errorMessage);
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
            throw new BadRequestException(getErrorResponse(ErrorType.MISSING_CREDENTIAL_IDENTIFIER_AND_CONFIGURATION_ID));
        }

        CredentialScopeModel requestedCredential;

        // When the CredentialRequest contains a credential identifier the caller must have gone through the
        // CredentialOffer process or otherwise have set up a valid CredentialOfferState

        if (credentialIdentifier != null) {

            // Retrieve the associated credential offer state
            //
            CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
            CredentialOfferState offerState = offerStorage.findOfferStateByCredentialId(session, credentialIdentifier);
            if (offerState == null) {
                var errorMessage = "No credential offer state for credential id: " + credentialIdentifier;
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
                throw new BadRequestException(getErrorResponse(UNKNOWN_CREDENTIAL_IDENTIFIER, errorMessage));
            }

            // Get the credential_configuration_id from AuthorizationDetails
            //
            OID4VCAuthorizationDetailsResponse authDetails = offerState.getAuthorizationDetails();
            String credConfigId = authDetails.getCredentialConfigurationId();
            if (credConfigId == null) {
                var errorMessage = "No credential_configuration_id in AuthorizationDetails";
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
                throw new BadRequestException(getErrorResponse(UNKNOWN_CREDENTIAL_CONFIGURATION, errorMessage));
            }

            // Find the credential configuration in the Issuer's metadata
            //
            SupportedCredentialConfiguration credConfig = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session).get(credConfigId);
            if (credConfig == null) {
                var errorMessage = "Mapped credential configuration not found: " + credConfigId;
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
                throw new BadRequestException(getErrorResponse(UNKNOWN_CREDENTIAL_CONFIGURATION, errorMessage));
            }

            // Verify the user login session
            //
            if (!userModel.getId().equals(offerState.getUserId())) {
                var errorMessage = "Unexpected login user: " + userModel.getUsername();
                LOGGER.errorf(errorMessage + " != %s", offerState.getUserId());
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_USER);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
            }

            // Verify the login client
            //
            if (offerState.getClientId() != null && !clientModel.getClientId().equals(offerState.getClientId())) {
                var errorMessage = "Unexpected login client: " + clientModel.getClientId();
                LOGGER.errorf(errorMessage + " != %s", offerState.getClientId());
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_CLIENT);
                throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST, errorMessage));
            }

            // Find the configured scope in the login client
            //
            ClientScopeModel clientScope = clientModel.getClientScopes(false).get(credConfig.getScope());
            if (clientScope == null) {
                var errorMessage = String.format("Client scope not found: %s", credConfig.getScope());
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
                throw new BadRequestException(getErrorResponse(UNKNOWN_CREDENTIAL_CONFIGURATION, errorMessage));
            }

            requestedCredential = new CredentialScopeModel(clientScope);
            LOGGER.debugf("Successfully mapped credential identifier %s to scope %s", credentialIdentifier, clientScope.getName());
            eventBuilder.detail(Details.CREDENTIAL_TYPE, credConfigId);

        } else if (credentialConfigurationId != null) {
            // Use credential_configuration_id for direct lookup
            requestedCredential = credentialRequestVO.findCredentialScope(session).orElseThrow(() -> {
                var errorMessage = "Credential scope not found for configuration id: " + credentialConfigurationId;
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
                return new BadRequestException(getErrorResponse(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, errorMessage));
            });
            eventBuilder.detail(Details.CREDENTIAL_TYPE, credentialConfigurationId);
        } else {
            // Neither provided - this should not happen due to earlier validation
            String errorMessage = "Missing both credential_configuration_id and credential_identifier";
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
            throw new BadRequestException(getErrorResponse(ErrorType.MISSING_CREDENTIAL_IDENTIFIER_AND_CONFIGURATION_ID));
        }

        checkScope(requestedCredential);

        SupportedCredentialConfiguration supportedCredential =
                OID4VCIssuerWellKnownProvider.toSupportedCredentialConfiguration(session, requestedCredential);

        // Get the list of all proofs (handles single proof, multiple proofs, or none)
        List<String> allProofs = getAllProofs(credentialRequestVO);

        // Generate credential response
        CredentialResponse responseVO = new CredentialResponse();

        if (allProofs.isEmpty()) {
            // Single issuance without proof
            Object theCredential = getCredential(authResult, supportedCredential, credentialRequestVO, eventBuilder);
            responseVO.addCredential(theCredential);
        } else {
            // Issue credentials for each proof
            Proofs originalProofs = credentialRequestVO.getProofs();
            // Determine the proof type from the original proofs
            String proofType = originalProofs != null ? originalProofs.getProofType() : null;

            for (String currentProof : allProofs) {
                Proofs proofForIteration = new Proofs();
                proofForIteration.setProofByType(proofType, currentProof);
                // Creating credential with keybinding to the current proof
                credentialRequestVO.setProofs(proofForIteration);
                Object theCredential = getCredential(authResult, supportedCredential, credentialRequestVO, eventBuilder);
                responseVO.addCredential(theCredential);
            }
            credentialRequestVO.setProofs(originalProofs);
        }

        // Encrypt all responses if encryption parameters are provided, except for error credential responses
        Response response;
        if (encryptionParams != null && !responseVO.getCredentials().isEmpty()) {
            String jwe = encryptCredentialResponse(responseVO, encryptionParams, encryptionMetadata);
            response = Response.ok()
                    .type(MediaType.APPLICATION_JWT)
                    .entity(jwe)
                    .build();
        } else {
            response = Response.ok().entity(responseVO).build();
        }

        // Mark event as successful
        eventBuilder.detail(Details.SCOPE, supportedCredential.getScope())
                .detail(Details.VERIFIABLE_CREDENTIAL_FORMAT, supportedCredential.getFormat())
                .detail(Details.VERIFIABLE_CREDENTIALS_ISSUED, String.valueOf(responseVO.getCredentials().size()));
        eventBuilder.success();

        return response;
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
            normalizeProofFields(credentialRequest);
            return credentialRequest;
        } catch (JsonProcessingException e) {
            String errorMessage = "Failed to parse JSON request: " + e.getMessage();
            LOGGER.errorf(e, "JSON parsing failed. Request payload length: %d",
                    requestPayload != null ? requestPayload.length() : 0);
            if (eventBuilder != null) {
                eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
            }
            throw new BadRequestException(getErrorResponse(INVALID_CREDENTIAL_REQUEST, errorMessage));
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
            throw new BadRequestException(getErrorResponse(INVALID_CREDENTIAL_REQUEST, message));
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
        Proofs proofs = credentialRequestVO.getProofs();
        if (proofs == null) {
            return new ArrayList<>(); // No proofs provided
        }

        // Validation already happened in normalizeProofFields, so we can safely extract proofs
        return proofs.getAllProofs();
    }

    private void validateProofTypes(Proofs proofs) {
        if (proofs == null) {
            return;
        }

        boolean hasJwtProofs = hasProofEntries(proofs.getJwt());
        boolean hasAttestationProofs = hasProofEntries(proofs.getAttestation());

        if (hasJwtProofs && hasAttestationProofs) {
            LOGGER.debug("The 'proofs' object must not contain multiple proof types.");
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_PROOF,
                    "The 'proofs' object must not contain multiple proof types."));
        }
    }

    private boolean hasProofEntries(List<String> proofs) {
        return proofs != null && !proofs.isEmpty();
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
        if (cachedAuthResult != null) {
            return cachedAuthResult;
        }

        AuthenticationManager.AuthResult authResult = bearerTokenAuthenticator.authenticate();
        if (authResult == null) {
            throw new CorsErrorResponseException(
                    cors,
                    ErrorType.INVALID_TOKEN.toString(),
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
                            ErrorType.INVALID_TOKEN.toString(),
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
     * @param credentialRequestVO the credential request
     * @param eventBuilder        the event builder for logging events
     * @return the signed credential
     */
    private Object getCredential(AuthenticationManager.AuthResult authResult,
                                 SupportedCredentialConfiguration credentialConfig,
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

        VCIssuanceContext vcIssuanceContext = getVCToSign(protocolMappers, credentialConfig, authResult, credentialRequestVO, credentialScopeModel, eventBuilder);

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
        return new BadRequestException(
                errorMessage,
                getErrorResponse(errorType, errorMessage)
        );
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
                                          AuthenticationManager.AuthResult authResult, CredentialRequest credentialRequestVO,
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
                .setType(List.of(credentialConfig.getScope()));

        Map<String, Object> subjectClaims = new HashMap<>();
        protocolMappers.forEach(mapper -> mapper.setClaim(subjectClaims, authResult.session()));

        Map<String, Object> subjectClaimsWithMetadataPrefix = new HashMap<>();
        protocolMappers
                .forEach(mapper -> mapper.setClaimWithMetadataPrefix(subjectClaims, subjectClaimsWithMetadataPrefix));

        // Validate that requested claims from authorization_details are present
        String credentialConfigId = credentialConfig.getId();
        validateRequestedClaimsArePresent(subjectClaimsWithMetadataPrefix, credentialConfig, authResult.session(), credentialConfigId, eventBuilder);

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
     * @param allClaims        all available claims. These are the claims including metadata prefix with the resolved path
     * @param credentialConfig Credential configuration
     * @param userSession      the user session
     * @param scope            the credential scope
     * @param eventBuilder     the event builder for logging error events
     * @throws BadRequestException if mandatory requested claims are missing
     */
    private void validateRequestedClaimsArePresent(Map<String, Object> allClaims, SupportedCredentialConfiguration credentialConfig,
                                                   UserSessionModel userSession, String scope, EventBuilder eventBuilder) {
        // Protocol mappers from configuration
        Map<List<Object>, ClaimsDescription> claimsConfig = credentialConfig.getCredentialMetadata().getClaims()
                .stream()
                .map(claim -> {
                    List<Object> pathObj = new ArrayList<>(claim.getPath());
                    return new ClaimsDescription(pathObj, claim.isMandatory());
                })
                .collect(Collectors.toMap(ClaimsDescription::getPath, claimsDescription -> claimsDescription));

        List<ClaimsDescription> claimsFromAuthzDetails = getClaimsFromAuthzDetails(scope, userSession);

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
            LOGGER.warnf("Requested claims validation failed for scope '%s', user '%s', client '%s': %s"
                    , scope, userSession.getUser().getUsername(), session.getContext().getClient().getClientId(), e.getMessage());
            // Add error event details with information about which mandatory claim is missing
            eventBuilder.detail(Details.REASON, errorMessage).error(Errors.INVALID_REQUEST);
            throw new BadRequestException(errorMessage);
        }
    }


    private List<ClaimsDescription> getClaimsFromAuthzDetails(String scope, UserSessionModel userSession) {
        String username = userSession.getUser().getUsername();
        String clientId = session.getContext().getClient().getClientId();

        // Look for stored claims in user session notes
        String claimsKey = AUTHORIZATION_DETAILS_CLAIMS_PREFIX + scope;
        String storedClaimsJson = userSession.getNote(claimsKey);

        if (storedClaimsJson != null && !storedClaimsJson.isEmpty()) {
            try {
                // Parse the stored claims from JSON
                return JsonSerialization.readValue(storedClaimsJson,
                        new TypeReference<>() {
                        });
            } catch (Exception e) {
                LOGGER.warnf(e, "Failed to parse stored claims for scope '%s', user '%s', client '%s'", scope, username, clientId);
            }
        } else {
            LOGGER.debugf("No stored claims found for scope '%s', user '%s', client '%s'", scope, username, clientId);
        }

        return Collections.emptyList();
    }
}
