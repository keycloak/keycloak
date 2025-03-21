/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProvider;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.keybinding.ProofValidator;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSigner;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.OID4VCClient;
import org.keycloak.protocol.oid4vc.model.OfferUriType;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.Proof;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantType;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.protocol.oid4vc.model.Format.JWT_VC;
import static org.keycloak.protocol.oid4vc.model.Format.LDP_VC;
import static org.keycloak.protocol.oid4vc.model.Format.SD_JWT_VC;
import static org.keycloak.protocol.oid4vc.model.Format.SUPPORTED_FORMATS;

/**
 * Provides the (REST-)endpoints required for the OID4VCI protocol.
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerEndpoint {

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerEndpoint.class);

    private Cors cors;

    private static final String CODE_LIFESPAN_REALM_ATTRIBUTE_KEY = "preAuthorizedCodeLifespanS";
    private static final int DEFAULT_CODE_LIFESPAN_S = 30;

    public static final String CREDENTIAL_PATH = "credential";
    public static final String CREDENTIAL_OFFER_PATH = "credential-offer/";
    public static final String RESPONSE_TYPE_IMG_PNG = "image/png";
    public static final String CREDENTIAL_OFFER_URI_CODE_SCOPE = "credential-offer";
    private final KeycloakSession session;
    private final AppAuthManager.BearerTokenAuthenticator bearerTokenAuthenticator;
    private final TimeProvider timeProvider;

    // lifespan of the preAuthorizedCodes in seconds
    private final int preAuthorizedCodeLifeSpan;

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

    private final boolean isIgnoreScopeCheck;

    public OID4VCIssuerEndpoint(KeycloakSession session,
                                Map<String, CredentialBuilder> credentialBuilders,
                                AppAuthManager.BearerTokenAuthenticator authenticator,
                                TimeProvider timeProvider, int preAuthorizedCodeLifeSpan,
                                boolean isIgnoreScopeCheck) {
        this.session = session;
        this.bearerTokenAuthenticator = authenticator;
        this.timeProvider = timeProvider;
        this.credentialBuilders = credentialBuilders;
        this.preAuthorizedCodeLifeSpan = preAuthorizedCodeLifeSpan;
        this.isIgnoreScopeCheck = isIgnoreScopeCheck;
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
        this.isIgnoreScopeCheck = false;
    }

    /**
     * Create credential builders from configured component models in Keycloak.
     *
     * @return a map of the created credential builders with their supported formats as keys.
     */
    private Map<String, CredentialBuilder> loadCredentialBuilders(KeycloakSession keycloakSession) {
        KeycloakSessionFactory keycloakSessionFactory = keycloakSession.getKeycloakSessionFactory();
        RealmModel realm = keycloakSession.getContext().getRealm();
        Stream<ComponentModel> componentModels = realm.getComponentsStream(
                realm.getId(), CredentialBuilder.class.getName());

        return componentModels.map(componentModel -> {
                    ProviderFactory<CredentialBuilder> providerFactory = keycloakSessionFactory
                            .getProviderFactory(CredentialBuilder.class, componentModel.getProviderId());

                    if (!(providerFactory instanceof ComponentFactory<?, ?>)) {
                        throw new IllegalArgumentException(String.format(
                                "Component %s is unexpectedly not a ComponentFactory",
                                componentModel.getProviderId()
                        ));
                    }

                    var componentFactory = (ComponentFactory<CredentialBuilder, CredentialBuilder>) providerFactory;
                    return componentFactory.create(keycloakSession, componentModel);
                })
                .collect(Collectors.toMap(CredentialBuilder::getSupportedFormat, component -> component));
    }

    /**
     * Provides the URI to the OID4VCI compliant credentials offer
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, RESPONSE_TYPE_IMG_PNG})
    @Path("credential-offer-uri")
    public Response getCredentialOfferURI(@QueryParam("credential_configuration_id") String vcId, @QueryParam("type") @DefaultValue("uri") OfferUriType type, @QueryParam("width") @DefaultValue("200") int width, @QueryParam("height") @DefaultValue("200") int height) {

        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();

        Map<String, SupportedCredentialConfiguration> credentialsMap = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
        LOGGER.debugf("Get an offer for %s", vcId);
        if (!credentialsMap.containsKey(vcId)) {
            LOGGER.debugf("No credential with id %s exists.", vcId);
            LOGGER.debugf("Supported credentials are %s.", credentialsMap);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
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
        } catch (JsonProcessingException e) {
            LOGGER.errorf("Could not convert the offer POJO to JSON: %s", e.getMessage());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
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

        return Response.ok()
                .type(MediaType.APPLICATION_JSON)
                .entity(credentialOfferURI)
                .build();
    }

    private Response getOfferUriAsQr(String sessionCode, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        String encodedOfferUri = URLEncoder.encode(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH + sessionCode, StandardCharsets.UTF_8);
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode("openid-credential-offer://?credential_offer_uri=" + encodedOfferUri, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
            return Response.ok().type(RESPONSE_TYPE_IMG_PNG).entity(bos.toByteArray()).build();
        } catch (WriterException | IOException e) {
            LOGGER.warnf("Was not able to create a qr code of dimension %s:%s.", width, height, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Was not able to generate qr.").build();
        }
    }

    /**
     * Provides an OID4VCI compliant credentials offer
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(CREDENTIAL_OFFER_PATH + "{sessionCode}")
    public Response getCredentialOffer(@PathParam("sessionCode") String sessionCode) {
        if (sessionCode == null) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
        }

        CredentialsOffer credentialsOffer = getOfferFromSessionCode(sessionCode);
        LOGGER.debugf("Responding with offer: %s", credentialsOffer);

        return Response.ok()
                .entity(credentialsOffer)
                .build();
    }

    private void checkScope(CredentialRequest credentialRequestVO) {
        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();
        String vcIssuanceFlow = clientSession.getNote(PreAuthorizedCodeGrantType.VC_ISSUANCE_FLOW);
        if (vcIssuanceFlow == null || !vcIssuanceFlow.equals(PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE)) {
            // authz code flow
            ClientModel client = clientSession.getClient();
            String credentialIdentifier = credentialRequestVO.getCredentialIdentifier();
            String scope = client.getAttributes().get("vc." + credentialIdentifier + ".scope"); // following credential identifier in client attribute
            AccessToken accessToken = bearerTokenAuthenticator.authenticate().getToken();
            if (Arrays.stream(accessToken.getScope().split(" ")).sequential().noneMatch(i -> i.equals(scope))) {
                LOGGER.debugf("Scope check failure: credentialIdentifier = %s, required scope = %s, scope in access token = %s.", credentialIdentifier, scope, accessToken.getScope());
                throw new CorsErrorResponseException(cors, ErrorType.UNSUPPORTED_CREDENTIAL_TYPE.toString(), "Scope check failure", Response.Status.BAD_REQUEST);
            } else {
                LOGGER.debugf("Scope check success: credentialIdentifier = %s, required scope = %s, scope in access token = %s.", credentialIdentifier, scope, accessToken.getScope());
            }
        } else {
            clientSession.removeNote(PreAuthorizedCodeGrantType.VC_ISSUANCE_FLOW);
        }
    }

    /**
     * Returns a verifiable credential
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(CREDENTIAL_PATH)
    public Response requestCredential(
            CredentialRequest credentialRequestVO) {
        LOGGER.debugf("Received credentials request %s.", credentialRequestVO);

        cors = Cors.builder().auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        // do first to fail fast on auth
        AuthenticationManager.AuthResult authResult = getAuthResult();

        if (!isIgnoreScopeCheck) {
            checkScope(credentialRequestVO);
        }

        // Both Format and identifier are optional.
        // If the credential_identifier is present, Format can't be present. But this implementation will
        // tolerate the presence of both, waiting for clarity in specifications.
        // This implementation will privilege the presence of the credential config identifier.
        String requestedCredentialId = credentialRequestVO.getCredentialIdentifier();
        String requestedFormat = credentialRequestVO.getFormat();

        // Check if at least one of both is available.
        if (requestedCredentialId == null && requestedFormat == null) {
            LOGGER.debugf("Missing both configuration id and requested format. At least one shall be specified.");
            throw new BadRequestException(getErrorResponse(ErrorType.MISSING_CREDENTIAL_CONFIG_AND_FORMAT));
        }

        Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(this.session);

        // resolve from identifier first
        SupportedCredentialConfiguration supportedCredentialConfiguration = null;
        if (requestedCredentialId != null) {
            supportedCredentialConfiguration = supportedCredentials.get(requestedCredentialId);
            if (supportedCredentialConfiguration == null) {
                LOGGER.debugf("Credential with configuration id %s not found.", requestedCredentialId);
                throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
            }
            // Then for format. We know spec does not allow both parameter. But we are tolerant if you send both
            // Was found by id, check that the format matches.
            if (requestedFormat != null && !requestedFormat.equals(supportedCredentialConfiguration.getFormat())) {
                LOGGER.debugf("Credential with configuration id %s does not support requested format %s, but supports %s.", requestedCredentialId, requestedFormat, supportedCredentialConfiguration.getFormat());
                throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_FORMAT));
            }
        }

        if (supportedCredentialConfiguration == null && requestedFormat != null) {
            // Search by format
            supportedCredentialConfiguration = getSupportedCredentialConfiguration(credentialRequestVO, supportedCredentials, requestedFormat);
            if (supportedCredentialConfiguration == null) {
                LOGGER.debugf("Credential with requested format %s, not supported.", requestedFormat);
                throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_FORMAT));
            }
        }

        CredentialResponse responseVO = new CredentialResponse();

        Object theCredential = getCredential(authResult, supportedCredentialConfiguration, credentialRequestVO);
        if (SUPPORTED_FORMATS.contains(requestedFormat)) {
            responseVO.setCredential(theCredential);
        } else {
            throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
        }
        return Response.ok().entity(responseVO).build();
    }

    private SupportedCredentialConfiguration getSupportedCredentialConfiguration(CredentialRequest credentialRequestVO, Map<String, SupportedCredentialConfiguration> supportedCredentials, String requestedFormat) {
        // 1. Format resolver
        List<SupportedCredentialConfiguration> configs = supportedCredentials.values().stream()
                .filter(supportedCredential -> Objects.equals(supportedCredential.getFormat(), requestedFormat))
                .collect(Collectors.toList());

        List<SupportedCredentialConfiguration> matchingConfigs;

        switch (requestedFormat) {
            case SD_JWT_VC:
                // Resolve from vct for sd-jwt
                matchingConfigs = configs.stream()
                        .filter(supportedCredential -> Objects.equals(supportedCredential.getVct(), credentialRequestVO.getVct()))
                        .collect(Collectors.toList());
                break;
            case JWT_VC:
            case LDP_VC:
                // Will detach this when each format provides logic on how to resolve from definition.
                matchingConfigs = configs.stream()
                        .filter(supportedCredential -> Objects.equals(supportedCredential.getCredentialDefinition(), credentialRequestVO.getCredentialDefinition()))
                        .collect(Collectors.toList());
                break;
            default:
                throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_FORMAT));
        }

        if (matchingConfigs.isEmpty()) {
            throw new BadRequestException(getErrorResponse(ErrorType.MISSING_CREDENTIAL_CONFIG));
        }

        return matchingConfigs.iterator().next();
    }

    private AuthenticatedClientSessionModel getAuthenticatedClientSession() {
        AuthenticationManager.AuthResult authResult = getAuthResult();
        UserSessionModel userSessionModel = authResult.getSession();

        AuthenticatedClientSessionModel clientSession = userSessionModel.
                getAuthenticatedClientSessionByClient(
                        authResult.getClient().getId());
        if (clientSession == null) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_TOKEN));
        }
        return clientSession;
    }

    private AuthenticationManager.AuthResult getAuthResult() {
        return getAuthResult(new BadRequestException(getErrorResponse(ErrorType.INVALID_TOKEN)));
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
    private Object getCredential(AuthenticationManager.AuthResult authResult, SupportedCredentialConfiguration credentialConfig, CredentialRequest credentialRequestVO) {

        // Get the client scope model from the credential configuration
        ClientScopeModel clientScopeModel = getClientScopeModel(credentialConfig);

        // Get the protocol mappers from the client scope
        List<OID4VCMapper> protocolMappers = clientScopeModel.getProtocolMappersStream()
                .map(pm -> {
                    if (session.getProvider(ProtocolMapper.class, pm.getProtocolMapper()) instanceof OID4VCMapper mapperFactory) {
                        ProtocolMapper protocolMapper = mapperFactory.create(session);
                        if (protocolMapper instanceof OID4VCMapper oid4VCMapper) {
                            oid4VCMapper.setMapperModel(pm);
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
        String format = credentialRequestVO.getFormat();
        CredentialSigner<?> credentialSigner = session.getProvider(CredentialSigner.class, format);

        return Optional.ofNullable(credentialSigner)
                .map(signer -> signer.signCredential(
                        vcIssuanceContext.getCredentialBody(),
                        credentialConfig.getCredentialBuildConfig()
                ))
                .orElseThrow(() -> new BadRequestException(
                        String.format("No signer found for format '%s'.", format)
                ));
    }

    private ClientScopeModel getClientScopeModel(SupportedCredentialConfiguration credentialConfig) {
        // Get the current client from the session
        ClientModel clientModel = session.getContext().getClient();

        // Get the client scope that matches the credentialConfig scope
        Map<String, ClientScopeModel> clientScopes = clientModel.getClientScopes(false);
        ClientScopeModel clientScopeModel = clientScopes.get(credentialConfig.getScope());

        if (clientScopeModel == null) {
            throw new BadRequestException("Client scope not found for the specified scope: " + credentialConfig.getScope());
        }

        return clientScopeModel;
    }

    private List<ProtocolMapperModel> getProtocolMappers(List<OID4VCClient> oid4VCClients) {

        return oid4VCClients.stream()
                .map(OID4VCClient::getClientDid)
                .map(this::getClient)
                .flatMap(ProtocolMapperContainerModel::getProtocolMappersStream)
                .toList();
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
        var errorResponse = new ErrorResponse();
        errorResponse.setError(errorType);
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private ClientModel getClient(String clientId) {
        return session.clients().getClientByClientId(session.getContext().getRealm(), clientId);
    }

    private List<OID4VCClient> getOID4VCClientsFromSession() {
        return session.clients().getClientsStream(session.getContext().getRealm())
                .filter(clientModel -> clientModel.getProtocol() != null)
                .filter(clientModel -> clientModel.getProtocol()
                        .equals(OID4VCLoginProtocolFactory.PROTOCOL_ID))
                .map(clientModel -> OID4VCClientRegistrationProvider.fromClientAttributes(clientModel.getClientId(), clientModel.getAttributes()))
                .toList();
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
                .stream()
                .filter(mapper -> mapper.isScopeSupported(credentialConfig.getScope()))
                .forEach(mapper -> mapper.setClaimsForSubject(subjectClaims, authResult.getSession()));

        subjectClaims.forEach((key, value) -> vc.getCredentialSubject().setClaims(key, value));

        protocolMappers
                .stream()
                .filter(mapper -> mapper.isScopeSupported(credentialConfig.getScope()))
                .forEach(mapper -> mapper.setClaimsForCredential(vc, authResult.getSession()));

        LOGGER.debugf("The credential to sign is: %s", vc);

        // Build format-specific credential
        CredentialBody credentialBody = findCredentialBuilder(credentialConfig)
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
        Proof proof = vcIssuanceContext.getCredentialRequest().getProof();
        if (proof == null) {
            LOGGER.debugf("No proof provided, skipping key binding");
            return;
        }

        ProofValidator proofValidator = session.getProvider(ProofValidator.class, proof.getProofType());
        if (proofValidator == null) {
            throw new BadRequestException(String.format("Unable to validate proofs of type %s", proof.getProofType()));
        }

        // Validate proof and bind public key to credential
        try {
            Optional.ofNullable(proofValidator.validateProof(vcIssuanceContext))
                    .ifPresent(jwk -> vcIssuanceContext.getCredentialBody().addKeyBinding(jwk));
        } catch (VCIssuerException e) {
            throw new BadRequestException("Could not validate provided proof", e);
        }
    }

    private CredentialBuilder findCredentialBuilder(SupportedCredentialConfiguration credentialConfig) {
        String format = credentialConfig.getFormat();
        CredentialBuilder credentialBuilder = credentialBuilders.get(format);

        if (credentialBuilder == null) {
            throw new BadRequestException(String.format("No credential builder found for format %s", format));
        }

        return credentialBuilder;
    }
}
