package org.keycloak.protocol.oid4vc.issuance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.OID4VCAbstractWellKnownProvider;
import org.keycloak.protocol.oid4vc.OID4VCClient;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProvider;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VPMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VPMapperFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.SupportedCredential;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.utils.MediaType;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    public static final String GRANT_TYPE_PRE_AUTHORIZED_CODE = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
    private static final String ACCESS_CONTROL_HEADER = "Access-Control-Allow-Origin";

    private final KeycloakSession session;
    private final AppAuthManager.BearerTokenAuthenticator bearerTokenAuthenticator;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    private final String issuerDid;

    private final Map<Format, VerifiableCredentialsSigningService> signingServices;

    public OID4VCIssuerEndpoint(KeycloakSession session,
                                String issuerDid,
                                Map<Format, VerifiableCredentialsSigningService> signingServices,
                                AppAuthManager.BearerTokenAuthenticator authenticator,
                                ObjectMapper objectMapper, TimeProvider timeProvider) {
        this.session = session;
        this.bearerTokenAuthenticator = authenticator;
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
        this.issuerDid = issuerDid;
        this.signingServices = signingServices;

    }

    /**
     * Provides URI to the OID4VCI compliant credentials offer
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("credential-offer-uri")
    public Response getCredentialOfferURI(@QueryParam("credentialId") String vcId) {

        Map<String, SupportedCredential> credentialsMap = OID4VCAbstractWellKnownProvider
                .getSupportedCredentials(session);

        LOGGER.debugf("Get an offer for %s", vcId);
        if (!credentialsMap.containsKey(vcId)) {
            LOGGER.debugf("No credential with id %s exists.", vcId);
            LOGGER.debugf("Supported credentials are %s.", credentialsMap);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_REQUEST));
        }
        SupportedCredential supportedCredential = credentialsMap.get(vcId);
        Format format = supportedCredential.getFormat();

        // check that the user is allowed to get such credential
        getClientsOfType(supportedCredential.getTypes(), format);

        String nonce = generateAuthorizationCode();

        AuthenticationManager.AuthResult authResult = getAuthResult();
        UserSessionModel userSessionModel = getUserSessionModel();

        AuthenticatedClientSessionModel clientSession = userSessionModel.
                getAuthenticatedClientSessionByClient(
                        authResult.getClient().getId());
        try {
            clientSession.setNote(nonce, objectMapper.writeValueAsString(supportedCredential));
        } catch (JsonProcessingException e) {
            LOGGER.errorf("Could not convert Supported Credential POJO to JSON: %s", e.getMessage());
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_REQUEST));
        }

        CredentialOfferURI credentialOfferURI = new CredentialOfferURI()
                .setIssuer(OID4VCAbstractWellKnownProvider.getIssuer(session.getContext()))
                .setNonce(nonce);

        LOGGER.debugf("Responding with nonce: %s", nonce);
        return Response.ok()
                .entity(credentialOfferURI)
                .header(ACCESS_CONTROL_HEADER, "*")
                .build();

    }

    /**
     * Provides an OID4VCI compliant credentials offer
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("credential-offer/{nonce}")
    public Response getCredentialOffer(@PathParam("nonce") String nonce) {

        OAuth2CodeParser.ParseResult result = parseNonce(nonce);

        SupportedCredential offeredCredential;
        try {
            offeredCredential = objectMapper.readValue(result.getClientSession().getNote(nonce),
                    SupportedCredential.class);
            LOGGER.debugf("Creating an offer for %s - %s", offeredCredential.getTypes(),
                    offeredCredential.getFormat());
            result.getClientSession().removeNote(nonce);
        } catch (JsonProcessingException e) {
            LOGGER.errorf("Could not convert SupportedCredential JSON to POJO: %s", e);
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_REQUEST));
        }

        String preAuthorizedCode = generateAuthorizationCodeForClientSession(result.getClientSession());
        CredentialsOffer theOffer = new CredentialsOffer()
                .setCredentialIssuer(OID4VCAbstractWellKnownProvider.getIssuer(session.getContext()))
                .setCredentials(List.of(offeredCredential))
                .setGrants(
                        new PreAuthorizedGrant()
                                .setPreAuthorizedCode(
                                        new PreAuthorizedCode()
                                                .setPreAuthorizedCode(preAuthorizedCode)
                                                .setUserPinRequired(false)));

        LOGGER.debugf("Responding with offer: %s", theOffer);
        return Response.ok()
                .entity(theOffer)
                .header(ACCESS_CONTROL_HEADER, "*")
                .build();
    }

    /**
     * Options endpoint to serve the cors-preflight requests.
     * Since we cannot know the address of the requesting wallets in advance, we have to accept all origins.
     */
    @OPTIONS
    @Path("{any: .*}")
    public Response optionCorsResponse() {
        return Response.ok().header(ACCESS_CONTROL_HEADER, "*")
                .header("Access-Control-Allow-Methods", "POST,GET,OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type,Authorization")
                .build();
    }

    /**
     * Requests a credential from the issuer
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(CREDENTIAL_PATH)
    public Response requestCredential(
            CredentialRequest credentialRequestVO) {
        LOGGER.debugf("Received credentials request %s.", credentialRequestVO);

        Format requestedFormat = credentialRequestVO.getFormat();
        String requestedCredential = credentialRequestVO.getCredentialIdentifier();

        SupportedCredential supportedCredential = Optional
                .ofNullable(OID4VCAbstractWellKnownProvider.getSupportedCredentials(this.session)
                        .get(requestedCredential))
                .orElseThrow(
                        () -> {
                            LOGGER.debugf("Unsupported credential %s was requested.", requestedCredential);
                            throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
                        });

        if (!supportedCredential.getFormat().equals(requestedFormat)) {
            LOGGER.debugf("Format %s is not supported for credential %s.", requestedFormat, requestedCredential);
            throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_FORMAT));
        }

        CredentialResponse responseVO = new CredentialResponse().setFormat(credentialRequestVO.getFormat());

        Object theCredential = getCredential(supportedCredential.getTypes(), credentialRequestVO.getFormat());
        switch (requestedFormat) {
            case LDP_VC, JWT_VC, SD_JWT_VC -> responseVO.setCredential(theCredential);
            default -> throw new BadRequestException(
                    getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
        }
        return Response.ok().entity(responseVO)
                .header(ACCESS_CONTROL_HEADER, "*").build();
    }

    // return the current usersession model
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

    protected Object getCredential(List<String> vcTypes, Format format) {
        // do first to fail fast on auth
        UserSessionModel userSessionModel = getUserSessionModel();

        List<OID4VCClient> clients = getClientsOfType(vcTypes, format);
        List<OID4VPMapper> protocolMappers = getProtocolMappers(clients)
                .stream()
                .map(OID4VPMapperFactory::createOID4VCMapper)
                .toList();

        VerifiableCredential credentialToSign = getVCToSign(protocolMappers, vcTypes, userSessionModel);

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

    private OAuth2CodeParser.ParseResult parseNonce(String nonce) throws BadRequestException {
        EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session,
                session.getContext().getConnection());
        OAuth2CodeParser.ParseResult result = OAuth2CodeParser.parseCode(session, nonce,
                session.getContext().getRealm(),
                eventBuilder);
        if (result.isExpiredCode() || result.isIllegalCode()) {
            throw new BadRequestException(getErrorResponse(ErrorType.INVALID_TOKEN));
        }
        return result;
    }

    private String generateAuthorizationCode() {
        AuthenticationManager.AuthResult authResult = getAuthResult();
        UserSessionModel userSessionModel = getUserSessionModel();
        AuthenticatedClientSessionModel clientSessionModel = userSessionModel.
                getAuthenticatedClientSessionByClient(authResult.getClient().getId());
        return generateAuthorizationCodeForClientSession(clientSessionModel);
    }

    private String generateAuthorizationCodeForClientSession(AuthenticatedClientSessionModel clientSessionModel) {
        int expiration = Time.currentTime() + clientSessionModel.getUserSession().getRealm().getAccessCodeLifespan();

        String codeId = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        OAuth2Code oAuth2Code = new OAuth2Code(codeId, expiration, nonce, null, null, null, null,
                clientSessionModel.getUserSession().getId());
        LOGGER.debugf("Persist code for clientSession %s", clientSessionModel.getClient().getId());
        return OAuth2CodeParser.persistCode(session, clientSessionModel, oAuth2Code);
    }

    private Response getErrorResponse(ErrorType errorType) {
        var errorResponse = new ErrorResponse();
        errorResponse.setError(errorType);
        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }

    @NotNull
    private List<OID4VCClient> getClientsOfType(List<String> vcType, Format format) {
        LOGGER.debugf("Retrieve all clients of type %s, supporting format %s", vcType, format.toString());

        Optional.ofNullable(vcType).filter(type -> !type.isEmpty()).orElseThrow(() -> {
            LOGGER.info("No VC type was provided.");
            return new BadRequestException("No VerifiableCredential-Type was provided in the request.");
        });

        List<OID4VCClient> oid4VCClients = getOID4VCClientsFromSession()
                .stream()
                .filter(oid4VCClient -> oid4VCClient.getSupportedVCTypes()
                        .stream()
                        .anyMatch(supportedCredential -> Collections.indexOfSubList(supportedCredential
                                .getTypes(), vcType) != -1))
                .toList();


        if (oid4VCClients.isEmpty()) {
            LOGGER.debugf("No OID4VP-Client supporting type %s registered.", vcType);
            throw new BadRequestException(getErrorResponse(ErrorType.UNSUPPORTED_CREDENTIAL_TYPE));
        }
        return oid4VCClients;
    }

    private ClientModel getClient(String clientId) {
        return session.clients().getClientByClientId(session.getContext().getRealm(), clientId);
    }

    @NotNull
    private List<OID4VCClient> getOID4VCClientsFromSession() {
        return session.clients().getClientsStream(session.getContext().getRealm())
                .filter(clientModel -> clientModel.getProtocol() != null)
                .filter(clientModel -> clientModel.getProtocol()
                        .equals(OID4VCClientRegistrationProviderFactory.PROTOCOL_ID))
                .map(clientModel -> OID4VCClientRegistrationProvider.fromClientAttributes(clientModel.getClientId(), clientModel.getAttributes()))
                .toList();
    }

    @NotNull
    private VerifiableCredential getVCToSign(List<OID4VPMapper> protocolMappers, List<String> vcTypes,
                                             UserSessionModel userSessionModel) {
        // set the required claims
        VerifiableCredential vc = new VerifiableCredential()
                .setIssuer(URI.create(issuerDid))
                .setIssuanceDate(Date.from(Instant.ofEpochMilli(timeProvider.currentTimeMillis())))
                .setType(vcTypes);

        Map<String, Object> subjectClaims = new HashMap<>();
        protocolMappers
                .forEach(mapper -> mapper.setClaimsForSubject(subjectClaims, userSessionModel));
        LOGGER.debugf("Will set %s", subjectClaims);

        subjectClaims.forEach((key, value) -> vc.getCredentialSubject().setClaims(key, value));

        protocolMappers
                .forEach(mapper -> mapper.setClaimsForCredential(vc, userSessionModel));

        if (vc.getId() == null && vc.getAdditionalProperties().get("id") == null) {
            vc.setId(URI.create(String.format("uri:uuid:%s", UUID.randomUUID())));
        }
        return vc;
    }
}