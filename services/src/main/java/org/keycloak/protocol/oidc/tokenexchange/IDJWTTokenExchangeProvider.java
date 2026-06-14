package org.keycloak.protocol.oidc.tokenexchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

/**
 * Provider for token exchange of Identity Assertion JWT Authorization Grant (ID-JAG)(*),
 * which is based on the token exchange specification RFC8693(**).
 *  
 * (*)https://datatracker.ietf.org/doc/draft-ietf-oauth-identity-assertion-authz-grant/
 * (**)https://datatracker.ietf.org/doc/html/rfc8693
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */
public class IDJWTTokenExchangeProvider extends StandardTokenExchangeProvider {

    private static final Logger logger = Logger.getLogger(IDJWTTokenExchangeProvider.class);

    @Override
    public boolean supports(TokenExchangeContext context) {
  
        String requestedTokenType = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (!OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE.equals(requestedTokenType)) {
            context.setUnsupportedReason("Parameter 'requested_token_type' should be 'urn:ietf:params:oauth:token-type:id-jag' for IDJWT token exchange");
            return false;
        }

        if(!OIDCAdvancedConfigWrapper.fromClientModel(context.getClient()).isStandardTokenExchangeEnabled()) {
            context.setUnsupportedReason("Standard token exchange is not enabled for the requested client");
            return false;
        }

        return true;
    }

    @Override
    protected Response tokenExchange() {
        
        // Reject dynamically registered clients to prevent unauthorized privilege escalation.
        // DCR clients could potentially manipulate client attributes to spoof identity assertions
        // or bypass strict security boundaries required for ID-JAG.
        if (client != null && client.getRegistrationToken() != null) {
            event.detail(Details.REASON, "Dynamically registered clients are not allowed to request ID-JAG");
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Dynamically registered clients are not allowed", Response.Status.BAD_REQUEST);
        }

        String subjectToken = context.getParams().getSubjectToken();
        String subjectTokenType = context.getParams().getSubjectTokenType();
        if (subjectToken == null || subjectTokenType == null) {
            event.detail(Details.REASON, "Missing required exchange parameters");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                "Parameters 'subject_token' and 'subject_token_type' are required for ID-JAG token exchange",
                Response.Status.BAD_REQUEST);
        }

        if (!subjectTokenType.equals(OAuth2Constants.ID_TOKEN_TYPE)) {
            // For now, only IDToken is supported. SAML 2.0 assertion and refresh token may be supported in the future.
            event.detail(Details.REASON, "Unsupported subject token type");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                "Parameter 'subject_token' supports IDToken only",
                Response.Status.BAD_REQUEST);
        }
        
        if (context.getParams().getActorToken() != null) {
            // For now, actor tokens are not supported. It will be supported in the future.
            event.detail(Details.REASON, "Actor tokens are not supported for ID-JAG token exchange");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                "Actor tokens are not supported for ID-JAG token exchange", Response.Status.BAD_REQUEST);
        }

        event.detail(Details.REQUESTED_TOKEN_TYPE, context.getParams().getRequestedTokenType());

        IDToken token = null;
        try {
            String realmUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
            TokenVerifier<IDToken> verifier = TokenVerifier.create(subjectToken, IDToken.class)
                    .withChecks(
                            new TokenVerifier.RealmUrlCheck(realmUrl),
                            new TokenVerifier.TokenTypeCheck(List.of(TokenUtil.TOKEN_TYPE_ID)),
                            TokenVerifier.IS_ACTIVE
                    );

            String algorithm = verifier.getHeader().getAlgorithm().name();
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, algorithm);
            if (signatureProvider == null) {
                throw new VerificationException("Invalid token algorithm: " + algorithm);
            }

            SignatureVerifierContext signatureVerifier = signatureProvider.verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(signatureVerifier);
            token = verifier.verify().getToken();
        } catch (VerificationException e) {
            logger.debugf("Verification failed: %s", e.getMessage());
            event.detail(Details.REASON, e.getMessage());
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, e.getMessage(), Response.Status.BAD_REQUEST);
        }
        // Validate the user session associated with the IDToken.
        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionId());
        if (userSession == null || !AuthenticationManager.isSessionValid(realm, userSession)) {
            event.detail(Details.REASON, "Session not found or invalid");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Session not found or invalid", Response.Status.BAD_REQUEST);
        }
        

        UserModel user = userSession.getUser();
        if (user == null || !user.isEnabled()) {
            event.detail(Details.REASON, "Invalid user");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid user", Response.Status.BAD_REQUEST);
        }

        // Validate the client session associated with the IDToken.
        //
        // Per OIDC specifications, if an ID token has multiple audiences, the 'azp' claim MUST be present.
        // Therefore, if 'azp' (token.getIssuedFor()) is missing, the token is expected to have only a single audience.
        // Thus, it is perfectly safe to extract the first element at index [0] as the fallback client ID.
        // We also ensure the array is not empty to prevent any IndexOutOfBoundsException.
        String audienceClientId = (token.getAudience() != null && token.getAudience().length > 0) ? token.getAudience()[0] : null;
        String clientIdForSession = token.getIssuedFor() != null ? token.getIssuedFor() : audienceClientId;

        if (clientIdForSession == null) {
            event.detail(Details.REASON, "Target client_id not found in token");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.BAD_REQUEST);
        }

        ClientModel tokenHolder = realm.getClientByClientId(clientIdForSession);
        if (tokenHolder == null) {
            event.detail(Details.REASON, "Token client not found");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Token client not found", Response.Status.BAD_REQUEST);
        }

        try {
            TokenVerifier.createWithoutSignature(token)
                .withChecks(
                    TokenManager.NotBeforeCheck.forModel(realm),
                    TokenManager.NotBeforeCheck.forModel(tokenHolder),
                    TokenManager.NotBeforeCheck.forModel(session, realm, user)
                )
                .verify();
        } catch (VerificationException e) {
            event.detail(Details.REASON, "Token invalidated by revocation policy");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN,
                "Token invalidated by revocation policy", Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(tokenHolder.getId());
        if (clientSession == null || !AuthenticationManager.isClientSessionValid(realm, tokenHolder, userSession, clientSession)) {
            event.detail(Details.REASON, "Client session not found or revoked");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Client session not found or revoked", Response.Status.BAD_REQUEST);
        }

        // Guard against token replay attacks by ensuring the ID token was not issued 
        // before the associated client session started.
        if (token.isIssuedBeforeSessionStart(clientSession.getStarted())) {
            event.detail(Details.REASON, "Token issued before the associated client session started");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Token issued before client session started", Response.Status.BAD_REQUEST);
        }

        
        KeycloakContext context = session.getContext();
        
        context.setClient(client);
        context.setBearerToken(token);
        context.setUserSession(userSession);
        
        event.user(user);
        event.detail(Details.USERNAME, user.getUsername());
        if (token.getSessionId() != null) {
            event.session(userSession);
        }
        event.detail(Details.SUBJECT_TOKEN_CLIENT_ID, token.getIssuedFor());

        return exchangeClientToClient(user, userSession, token, true);

    }

    @Override
    protected String getRequestedScope(JsonWebToken token, List<ClientModel> targetAudienceClients) {
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        return scope;

    }

    @Override
    protected Response buildTokenExchangeResponse(ClientSessionContext clientSessionCtx, String requestedTokenType, List<ClientModel> targetAudienceClients) {
                
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();
        
        JsonWebToken idjag = new JsonWebToken();
        idjag.id(SecretGenerator.getInstance().generateSecureID());
        idjag.type(TokenUtil.TOKEN_TYPE_IDJAG); // IDJAG
        idjag.subject(user.getId());
        idjag.issuedNow();

        String issuer = clientSession.getNote(OIDCLoginProtocol.ISSUER);
        if (issuer == null) {
            issuer = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
        }
        idjag.issuer(issuer);

        // use the first audience parameter as the audience of the ID-JAG token.
        if (params.getAudience() != null) {
            idjag.audience(new String[]{ params.getAudience().get(0) });
        }

        if (params.getScope() != null) {
            idjag.setOtherClaims("scope", params.getScope());
        }

        int lifespan = realm.getAccessTokenLifespan(); 
        String clientLifespanStr = client.getAttribute(org.keycloak.protocol.oidc.OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN);
        if (clientLifespanStr != null && !clientLifespanStr.trim().isEmpty()) {
            try {
                int clientLifespan = Integer.parseInt(clientLifespanStr);
                if (clientLifespan > 0) {
                    lifespan = clientLifespan;
                }
            } catch (NumberFormatException e) {
                // Ignore parse failure and fallback to realm default
            }
        }
        idjag.exp((long) (Time.currentTimeSeconds() + lifespan));

        // Execute HardcodedClaimMapper for client_id
        Stream<Map.Entry<ProtocolMapperModel, ProtocolMapper>> sortedMappersStream = 
            ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx);

        sortedMappersStream.forEach(entry -> {
            ProtocolMapperModel mapperModel = entry.getKey();
            
            if ("oidc-hardcoded-claim-mapper".equals(mapperModel.getProtocolMapper())) {
                String claimName = mapperModel.getConfig().get("claim.name");
                String claimValue = mapperModel.getConfig().get("claim.value");
        
                if ("client_id".equals(claimName)) {
                    idjag.getOtherClaims().put(claimName, claimValue);
                }
            }
        });
        
        Object targetClientId = idjag.getOtherClaims().get("client_id");
        if (targetClientId == null || targetClientId.toString().isBlank()) {
            String errorMessage = "Claim 'client_id' is missing in ID-JAG. Ensure a Hardcoded Claim protocol mapper is configured in the Client Scope for target: "
                + (params.getAudience() != null ? params.getAudience().get(0) : "");
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CONFIG);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SERVER_ERROR, errorMessage, Response.Status.BAD_REQUEST);
        }

        // use the same signature algorithm as the ID token for ID-JAG 
        //String signatureAlgorithm = session.tokens().signatureAlgorithm(org.keycloak.TokenCategory.ID);
        String signatureAlgorithm = session.tokens().signatureAlgorithm(org.keycloak.TokenCategory.ACCESS);
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
        SignatureSignerContext signer = signatureProvider.signer();

        String encodedIdJag = new JWSBuilder()
                .type(OAuth2Constants.IDENTITY_ASSERTION_JWT_HEADER_TYPE) // "oauth-id-jag+jwt"
                .jsonContent(idjag)
                .sign(signer);

        AccessTokenResponse res = new AccessTokenResponse();
        res.setToken(encodedIdJag);
        res.setTokenType(TokenUtil.TOKEN_TYPE_NA); 
        res.setExpiresIn((long) realm.getAccessTokenLifespan());
        res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, requestedTokenType);
        res.setScope(params.getScope());

        if (params.getAudience() != null) {
            event.detail(Details.AUDIENCE, params.getAudience());
        }
        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }
    
    @Override
    protected Response exchangeClientToSAML2Client(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, List<ClientModel> targetAudienceClients) {
        event.detail(Details.REASON, "requested_token_type unsupported");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }
    
    @Override
    protected List<String> getSupportedOAuthResponseTokenTypes() {
        return Arrays.asList(OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE);
    }

    @Override
    protected String getRequestedTokenType() {
        String requestedTokenType = params.getRequestedTokenType();
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
            return requestedTokenType;
        }
        if (requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.ID_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.SAML2_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE)) {
            return requestedTokenType;
        }
        OIDCAdvancedConfigWrapper oidcClient = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                && oidcClient.isUseRefreshToken()
                && oidcClient.getStandardTokenExchangeRefreshEnabled() != OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.NO) {
            return requestedTokenType;
        }

        event.detail(Details.REASON, "requested_token_type unsupported in JWTTokenExchangeProvider");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported in JWTTokenExchangeProvider", Response.Status.BAD_REQUEST);
    }

    // Using the value of "audience" parameter to find the target audience clients. 
    // The value of "audience" parameter is expected to be the same as the value of clientId of the target audience client. 
    // The "audience" parameter is required for this token exchange provider.
    protected List<ClientModel> getTargetAudienceClients() {
        List<String> audienceParams = params.getAudience();
        List<ClientModel> targetAudienceClients = new ArrayList<>();
        if (audienceParams != null) {
            if (audienceParams.size() == 0) {
                event.detail(Details.REASON, "audience required");
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "audience required", Response.Status.BAD_REQUEST);
            } else if (audienceParams.size() == 1) {
                // only the first one is used
                String audienceParameterString = params.getAudience().get(0);
                
                ClientModel matchingClient = realm.getClientByClientId(audienceParameterString);
                if (matchingClient == null) {
                    event.detail(Details.AUDIENCE, audienceParameterString);
                    event.detail(Details.REASON, "Client not found for audience parameter: " + audienceParameterString);
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client not found for audience parameter: " + audienceParameterString, Response.Status.BAD_REQUEST);
                } else {
                    if (matchingClient.getRegistrationToken() != null) {
                        event.detail(Details.REASON, "Target client cannot be a dynamically registered client for ID-JAG exchange.");
                        event.error(Errors.NOT_ALLOWED);
                        throw new CorsErrorResponseException(
                            cors,
                            OAuthErrorException.INVALID_TARGET,
                            "ID-JAG token exchange is disabled for dynamically registered target clients",
                            Response.Status.BAD_REQUEST
                        );
                    }
                    targetAudienceClients.add(matchingClient);
                }
            
            } else if (audienceParams.size() > 1) {
                event.detail(Details.REASON, "Multiple audiences are not supported");
                event.error(Errors.INVALID_REQUEST);

                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, 
                    "The ID-JAG token exchange provider does not support multiple audiences. Please request one audience at a time.", 
                    Response.Status.BAD_REQUEST);
            }
        } else {
            event.detail(Details.REASON, "audience required");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "audience required", Response.Status.BAD_REQUEST);
        }
        return targetAudienceClients;
    }
}
