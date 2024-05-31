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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.encoder.QRCode;
import jakarta.annotation.Nullable;
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
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProvider;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService;
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
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantType;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.utils.MediaType;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides the (REST-)endpoints required for the OID4VCI protocol.
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerEndpoint {

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerEndpoint.class);

    public static final String CREDENTIAL_PATH = "credential";
    public static final String CREDENTIAL_OFFER_PATH = "credential-offer/";
    public static final String RESPONSE_TYPE_IMG_PNG = "image/png";
    private final KeycloakSession session;
    private final AppAuthManager.BearerTokenAuthenticator bearerTokenAuthenticator;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    private final String issuerDid;
    // lifespan of the preAuthorizedCodes in seconds
    private final int preAuthorizedCodeLifeSpan;

    private final Map<Format, VerifiableCredentialsSigningService> signingServices;

    public OID4VCIssuerEndpoint(KeycloakSession session,
                                String issuerDid,
                                Map<Format, VerifiableCredentialsSigningService> signingServices,
                                AppAuthManager.BearerTokenAuthenticator authenticator,
                                ObjectMapper objectMapper, TimeProvider timeProvider, int preAuthorizedCodeLifeSpan) {
        this.session = session;
        this.bearerTokenAuthenticator = authenticator;
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
        this.issuerDid = issuerDid;
        this.signingServices = signingServices;
        this.preAuthorizedCodeLifeSpan = preAuthorizedCodeLifeSpan;

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
        Format format = supportedCredentialConfiguration.getFormat();

        // check that the user is allowed to get such credential
        if (getClientsOfType(supportedCredentialConfiguration.getScope(), format).isEmpty()) {
            LOGGER.debugf("No OID4VP-Client supporting type %s registered.", supportedCredentialConfiguration.getScope());
            throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
        }

        String nonce = generateNonce();
        try {
            clientSession.setNote(nonce, objectMapper.writeValueAsString(supportedCredentialConfiguration));
        } catch (JsonProcessingException e) {
            LOGGER.errorf("Could not convert Supported Credential POJO to JSON: %s", e.getMessage());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
        }

        return switch (type) {
            case URI -> getOfferUriAsUri(nonce);
            case QR_CODE -> getOfferUriAsQr(nonce, width, height);
        };

    }

    private Response getOfferUriAsUri(String nonce) {
        CredentialOfferURI credentialOfferURI = new CredentialOfferURI()
                .setIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH)
                .setNonce(nonce);

        return Response.ok()
                .type(MediaType.APPLICATION_JSON)
                .entity(credentialOfferURI)
                .build();
    }

    private Response getOfferUriAsQr(String nonce, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        String endcodedOfferUri = URLEncoder.encode(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH + nonce, StandardCharsets.UTF_8);
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode("openid-credential-offer://?credential_offer_uri=" + endcodedOfferUri, BarcodeFormat.QR_CODE, width, height);
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
    @Path(CREDENTIAL_OFFER_PATH + "{nonce}")
    public Response getCredentialOffer(@PathParam("nonce") String nonce) {
        if (nonce == null) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
        }

        AuthenticatedClientSessionModel clientSession = getAuthenticatedClientSession();

        String note = clientSession.getNote(nonce);
        if (note == null) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
        }

        SupportedCredentialConfiguration offeredCredential;
        try {
            offeredCredential = objectMapper.readValue(note,
                    SupportedCredentialConfiguration.class);
            LOGGER.debugf("Creating an offer for %s - %s", offeredCredential.getScope(),
                    offeredCredential.getFormat());
            clientSession.removeNote(nonce);
        } catch (JsonProcessingException e) {
            LOGGER.errorf("Could not convert SupportedCredential JSON to POJO: %s", e);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_CREDENTIAL_REQUEST));
        }

        String preAuthorizedCode = generateAuthorizationCodeForClientSession(clientSession);

        CredentialsOffer theOffer = new CredentialsOffer()
                .setCredentialIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()))
                .setCredentialConfigurationIds(List.of(offeredCredential.getId()))
                .setGrants(
                        new PreAuthorizedGrant()
                                .setPreAuthorizedCode(
                                        new PreAuthorizedCode()
                                                .setPreAuthorizedCode(preAuthorizedCode)));

        LOGGER.debugf("Responding with offer: %s", theOffer);
        return Response.ok()
                .entity(theOffer)
                .build();
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

        // do first to fail fast on auth
        UserSessionModel userSessionModel = getUserSessionModel();

        Format requestedFormat = credentialRequestVO.getFormat();
        String requestedCredential = credentialRequestVO.getCredentialIdentifier();

        SupportedCredentialConfiguration supportedCredentialConfiguration = Optional
                .ofNullable(OID4VCIssuerWellKnownProvider.getSupportedCredentials(this.session)
                        .get(requestedCredential))
                .orElseThrow(
                        () -> {
                            LOGGER.debugf("Unsupported credential %s was requested.", requestedCredential);
                            return new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
                        });

        if (!supportedCredentialConfiguration.getFormat().equals(requestedFormat)) {
            LOGGER.debugf("Format %s is not supported for credential %s.", requestedFormat, requestedCredential);
            throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_FORMAT));
        }

        CredentialResponse responseVO = new CredentialResponse();

        Object theCredential = getCredential(userSessionModel, supportedCredentialConfiguration.getScope(), credentialRequestVO.getFormat());
        switch (requestedFormat) {
            case LDP_VC, JWT_VC, SD_JWT_VC -> responseVO.setCredential(theCredential);
            default -> throw new BadRequestException(
                    getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
        }
        return Response.ok().entity(responseVO)
                .build();
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

    // return the current UserSessionModel
    private UserSessionModel getUserSessionModel() {
        return getAuthResult(
                new BadRequestException(getErrorResponse(ErrorType.INVALID_TOKEN))).getSession();
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
     * @param userSessionModel userSession to create the credential for
     * @param vcType           type of the credential to be created
     * @param format           format of the credential to be created
     * @return the signed credential
     */
    private Object getCredential(UserSessionModel userSessionModel, String vcType, Format format) {

        List<OID4VCClient> clients = getClientsOfType(vcType, format);

        List<OID4VCMapper> protocolMappers = getProtocolMappers(clients)
                .stream()
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

        VerifiableCredential credentialToSign = getVCToSign(protocolMappers, vcType, userSessionModel);

        return Optional.ofNullable(signingServices.get(format))
                .map(verifiableCredentialsSigningService -> verifiableCredentialsSigningService.signCredential(credentialToSign))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Requested format %s is not supported.", format)));
    }

    private List<ProtocolMapperModel> getProtocolMappers(List<OID4VCClient> oid4VCClients) {

        return oid4VCClients.stream()
                .map(OID4VCClient::getClientDid)
                .map(this::getClient)
                .flatMap(ProtocolMapperContainerModel::getProtocolMappersStream)
                .toList();
    }

    private String generateNonce() {
        return SecretGenerator.getInstance().randomString();
    }

    private String generateAuthorizationCodeForClientSession(AuthenticatedClientSessionModel clientSessionModel) {
        int expiration = timeProvider.currentTimeSeconds() + preAuthorizedCodeLifeSpan;
        return PreAuthorizedCodeGrantType.getPreAuthorizedCode(session, clientSessionModel, expiration);
    }

    private Response getErrorResponse(ErrorType errorType) {
        var errorResponse = new ErrorResponse();
        errorResponse.setError(errorType);
        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }

    // Return all {@link  OID4VCClient}s that support the given type and format
    private List<OID4VCClient> getClientsOfType(String vcType, Format format) {
        LOGGER.debugf("Retrieve all clients of type %s, supporting format %s", vcType, format.toString());

        if (Optional.ofNullable(vcType).filter(type -> !type.isEmpty()).isEmpty()) {
            throw new BadRequestException("No VerifiableCredential-Type was provided in the request.");
        }

        return getOID4VCClientsFromSession()
                .stream()
                .filter(oid4VCClient -> oid4VCClient.getSupportedVCTypes()
                        .stream()
                        .anyMatch(supportedCredential -> supportedCredential.getScope().equals(vcType)))
                .toList();
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
    private VerifiableCredential getVCToSign(List<OID4VCMapper> protocolMappers, String vcType,
                                             UserSessionModel userSessionModel) {
        // set the required claims
        VerifiableCredential vc = new VerifiableCredential()
                .setIssuer(URI.create(issuerDid))
                .setIssuanceDate(Date.from(Instant.ofEpochMilli(timeProvider.currentTimeMillis())))
                .setType(List.of(vcType));

        Map<String, Object> subjectClaims = new HashMap<>();
        protocolMappers
                .stream()
                .filter(mapper -> mapper.isTypeSupported(vcType))
                .forEach(mapper -> mapper.setClaimsForSubject(subjectClaims, userSessionModel));

        subjectClaims.forEach((key, value) -> vc.getCredentialSubject().setClaims(key, value));

        protocolMappers
                .stream()
                .filter(mapper -> mapper.isTypeSupported(vcType))
                .forEach(mapper -> mapper.setClaimsForCredential(vc, userSessionModel));

        LOGGER.debugf("The credential to sign is: %s", vc);
        return vc;
    }
}