package org.keycloak.protocol.oidc.tokenexchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
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

    // client attribute used to find the resource authorization server client by the audience parameter
    private static final String RESOURCE_AUTHORIZATION_SERVER_IDENTIFIER = "idjag.resource.authorization.server.identifier";

    // client attribute prefix for the client's client_id in resource authorization server
    private static final String CLIENTID_IN_RESOURCE_AUTHORIZATION_SERVER = "idjag.clientid.at.";

    // client attribute prefix for the permitted scopes in the resource authorization server
    private static final String PERMITTED_SCOPES_IN_RESOURCE_AUTHORIZATION_SERVER = "idjag.permitted.scopes.at.";

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

        event.detail(Details.REQUESTED_TOKEN_TYPE, context.getParams().getRequestedTokenType());

        IDToken token= null;
        try{
            String realmUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());

            TokenVerifier<IDToken> verifier = TokenVerifier.create(subjectToken, IDToken.class)
                    .withChecks(
                        new TokenVerifier.RealmUrlCheck(realmUrl),          
                        new TokenVerifier.TokenTypeCheck(List.of(TokenUtil.TOKEN_TYPE_ID)),   
                        TokenVerifier.IS_ACTIVE
                    );
                    
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, algorithm);
            if (signatureProvider == null) {
                logger.debugf("Invalid algorithm '%s' in the IDToken", algorithm);
                event.detail(Details.REASON, "Invalid token");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.BAD_REQUEST); 
            }
            SignatureVerifierContext signatureVerifier = signatureProvider.verifier(kid);
            verifier.verifierContext(signatureVerifier);
            token = verifier.verify().getToken();
        } catch (VerificationException e) {
            logger.debugf("Verification failed: %s", e.getMessage());
            event.detail(Details.REASON, e.getMessage());
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, e.getMessage(), Response.Status.BAD_REQUEST);
        }
        
        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionId());
        if (userSession == null || !AuthenticationManager.isSessionValid(realm, userSession)) {
            event.detail(Details.REASON, "Session not found or invalid");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Session not found or invalid", Response.Status.BAD_REQUEST);
        }
        
        int notBefore = realm.getNotBefore();
        if (token.getIat() < notBefore) {
            event.detail(Details.REASON, "Token issued before Not-Before policy");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Token invalidated by revocation policy", Response.Status.BAD_REQUEST);
        }

        KeycloakContext context = session.getContext();


        UserModel user = userSession.getUser();
        if (user == null || !user.isEnabled()) {
            event.detail(Details.REASON, "Invalid user");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid user", Response.Status.BAD_REQUEST);
        }
        
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

        ClientModel resourceAuthzClient = targetAudienceClients.get(0);
        String clientId = resourceAuthzClient.getClientId();
       
        String attrKey = PERMITTED_SCOPES_IN_RESOURCE_AUTHORIZATION_SERVER + clientId;
        String attrValue = client.getAttribute(attrKey);
            
        java.util.List<String> permittedScopeList;
        java.util.Set<String> permittedScopes;
        if (attrValue != null && !attrValue.isBlank()) {
            // attrValue is expected to be a space separated list of scopes, e.g. "scope1 scope2 scope3"
            permittedScopeList = java.util.Arrays.asList(attrValue.trim().split("\\s+"));
            permittedScopes = new java.util.HashSet<>(permittedScopeList);
        } else {
            String errorMessage = "Permitted scopes not configured for the audience client";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SERVER_ERROR, errorMessage, Response.Status.INTERNAL_SERVER_ERROR);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);
        if (scope == null || scope.isBlank()) {
            return String.join(" ", permittedScopeList);
        }
        String[] requestedScopes = scope.split("\\s+");

        java.util.List<String> filteredScopes = new java.util.ArrayList<>();
        for (String requested : requestedScopes) {
            if (permittedScopes.contains(requested)) {
                filteredScopes.add(requested);
            } else {
                logger.warn("Requested scope [" + requested + "] is not permitted and was filtered out.");
            }
        } 
        
        if (filteredScopes.isEmpty()) {
            String errorMessage = "Invalid scopes: " + scope;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, errorMessage, Response.Status.BAD_REQUEST);
        }

        return String.join(" ", filteredScopes);

    }

    @Override
    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  List<ClientModel> targetAudienceClients, String scope, JsonWebToken subjectToken) {
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = createSessionModel(targetUserSession, rootAuthSession, targetUser, client, scope);
        boolean isOfflineSession = targetUserSession.isOffline();

        // create a transient session now for the token exchange
        if (isOfflineSession) {
            targetUserSession = UserSessionUtil.createTransientUserSession(session, targetUserSession);
        }

        final boolean newClientSessionCreated = targetUserSession.getPersistenceState() != UserSessionModel.SessionPersistenceState.TRANSIENT
                && targetUserSession.getAuthenticatedClientSessionByClient(client.getId()) == null;

        try {
            // Bypass token scope restrictions and use a transient client session for better performance
            ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession,
                    null, true);

            if (params.getAudience() != null && !targetAudienceClients.isEmpty()) {
                clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, targetAudienceClients.toArray(ClientModel[]::new));
            }

            clientSessionCtx.setAttribute(Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

            TokenContextEncoderProvider encoder = session.getProvider(TokenContextEncoderProvider.class);

            TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session,
                            clientSessionCtx.getClientSession().getUserSession(), clientSessionCtx).generateIDJag();

            ClientModel resourceAuthzClient = targetAudienceClients.get(0);
            String clientId = resourceAuthzClient.getClientId();
            
            String attrKey = CLIENTID_IN_RESOURCE_AUTHORIZATION_SERVER + clientId;
            String attrValue = client.getAttribute(attrKey);
            if (attrValue == null || attrValue.isBlank()) {
                 String errorMessage = "Client ID of requested client in resource authorization server is not configured as the client attribute: " + attrKey;
                 event.detail(Details.REASON, errorMessage);
                 event.error(Errors.NOT_ALLOWED);
                 throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, errorMessage, Response.Status.FORBIDDEN);
            }
            responseBuilder.getIdjag().setClientId(attrValue);
            responseBuilder.getIdjag().setScope(scope);

            if (encoder.getTokenContextFromTokenId(responseBuilder.getAccessToken().getId()).getSessionType() == AccessTokenContext.SessionType.TRANSIENT) {
                responseBuilder.getAccessToken().setSessionId(null);
                event.session((String) null);
            }

            responseBuilder.getAccessToken().addAudience(params.getAudience().get(0));

            AccessTokenResponse res;     
            res = responseBuilder.build();
            res.setTokenType(TokenUtil.TOKEN_TYPE_NA);
            res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, requestedTokenType);
            res.setScope(scope);

            if (responseBuilder.getAccessToken().getAudience() != null) {
                event.detail(Details.AUDIENCE, CollectionUtil.join(List.of(responseBuilder.getAccessToken().getAudience()), " "));
            }
            event.success();

            return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
        } catch (RuntimeException e) {
            // Cleanup client-session if created in this request
            if (newClientSessionCreated) {
                targetUserSession.removeAuthenticatedClientSessions(Set.of(client.getId()));
            }

            throw e;
        }
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
    // The value of "audience" parameter is expected to be the same as the value of client attribute
    // "idjag.resource.authorization.server.identifier" in the target audience client.
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
                
                ClientModel targetClient = session.clients().getClientsStream(realm)
                    .filter(c -> audienceParameterString.equals(c.getAttribute(RESOURCE_AUTHORIZATION_SERVER_IDENTIFIER)))
                    .findFirst()
                    .orElseThrow(() -> {
                        event.detail(Details.REASON, "Client not found for audience identifier: " + audienceParameterString);
                        event.error(Errors.NOT_ALLOWED);
                        return new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client not found for audience identifier: " + audienceParameterString, Response.Status.BAD_REQUEST);
                    });

                targetAudienceClients.add(targetClient);
            
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
