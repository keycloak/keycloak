package org.keycloak.protocol.oidc.tokenexchange;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenCategory;
import org.keycloak.TokenVerifier;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.util.TokenUtil;

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

    @Override
    public boolean supports(TokenExchangeContext context) {

        String requestedTokenType = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (!OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE.equals(requestedTokenType)) {
            context.setUnsupportedReason("Parameter 'requested_token_type' should be 'urn:ietf:params:oauth:token-type:id-jag' for IDJWT token exchange");
            return false;
        }

        if (!OIDCAdvancedConfigWrapper.fromClientModel(context.getClient()).isStandardTokenExchangeEnabled()) {
            context.setUnsupportedReason("Standard token exchange is not enabled for the requested client");
            return false;
        }

        return true;
    }

    @Override
    protected Response tokenExchange() {
        validateRequestParams();
        return super.tokenExchange();
    }

    /**
     * Validates the ID-JAG specific constraints on the request parameters.
     * Everything that requires parsing the subject token is done in processSubjectToken().
     */
    private void validateRequestParams() {
        String subjectToken = params.getSubjectToken();
        String subjectTokenType = params.getSubjectTokenType();
        if (subjectToken == null || subjectTokenType == null) {
            event.detail(Details.REASON, "Missing required exchange parameters");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                "Parameters 'subject_token' and 'subject_token_type' are required for ID-JAG token exchange",
                Response.Status.BAD_REQUEST);
        }


        if (params.getActorToken() != null || params.getActorTokenType() != null) {
            // For now, actor tokens are not supported. It will be supported in the future.
            event.detail(Details.REASON, "Actor tokens are not supported for ID-JAG token exchange");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                "Actor tokens are not supported for ID-JAG token exchange", Response.Status.BAD_REQUEST);
        }

        if (params.getResource() != null && !params.getResource().isEmpty()) {
             event.detail(Details.REASON, "Resource parameters are not supported for ID-JAG token exchange");
             event.error(Errors.INVALID_REQUEST);
             throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                 "Resource parameters are not supported for ID-JAG token exchange", Response.Status.BAD_REQUEST);
         }

        // Exactly one audience. RFC 8693 allows multiple audiences, but the draft defines the
        // audience as the issuer of *the* Resource Authorization Server, and the client_id claim
        // is specific to that AS. Rejecting multiple audiences now keeps the option to relax it later.
        List<String> audienceParams = params.getAudience();
        if (audienceParams == null || audienceParams.size() != 1) {
            event.detail(Details.REASON, "Exactly one audience is required");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                "The ID-JAG token exchange requires exactly one audience", Response.Status.BAD_REQUEST);
        }
    }

    private void validateUserSession(AuthenticationManager.AuthResult authResult) {
        UserSessionModel tokenSession = authResult.session();
        AccessToken token = authResult.token();
        ClientModel tokenHolder = authResult.client();

        // An ID token without sid was issued from a transient user session (e.g. a service account
        // via client_credentials), which does not represent a user login.
        if (token.getSessionId() == null) {
            String errorMessage = "ID-JAG requires an ID token bound to a user session";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, errorMessage, Response.Status.BAD_REQUEST);
        }

        // The ID-JAG is only issued for an online user session. The random jti of an ID token resolves to an
        // UNKNOWN session type, so the lookup can fall back to an offline session with the same id.
        // This also covers an offline_access login without an existing SSO session, where Keycloak keeps only the offline session.
        if (tokenSession != null && tokenSession.isOffline()) {
            String errorMessage = "ID-JAG requires an online user session; an ID token bound only to an offline session cannot be used";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, errorMessage, Response.Status.BAD_REQUEST);
        }

        // verifyIdentityToken() checks that the client session exists, but not that it is still within its lifespan,
        // and none of the session checks look at the logout state. Reject sessions that are being logged out
        // (e.g. during frontchannel logout), so that a still-unexpired ID token cannot mint an ID-JAG.
        AuthenticatedClientSessionModel clientSession = tokenSession == null || tokenHolder == null
            ? null : tokenSession.getAuthenticatedClientSessionByClient(tokenHolder.getId());
        if (clientSession == null || isLoggingOut(tokenSession, clientSession)
                || !AuthenticationManager.isClientSessionValid(realm, tokenHolder, tokenSession, clientSession)) {
            event.detail(Details.REASON, "Client session not found or revoked");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN,
                "Client session not found or revoked", Response.Status.BAD_REQUEST);
        }
    }

    // For now, only IDToken is supported. SAML 2.0 assertion and refresh token may be supported in the future.
    @Override
    protected String getSupportedSubjectTokenType() {
        return OAuth2Constants.ID_TOKEN_TYPE;
    }

    /**
     * The subject token is an ID token: the Bearer typ check is replaced by the ID typ check, and the ID-JAG
     * is only issued for an online user session that is not being logged out.
     */
    @Override
    protected AuthenticationManager.AuthResult verifySubjectToken(String subjectToken) {
        // Keep access-token-specific revocation checks disabled; ID token type is checked explicitly.
        AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, realm, session.getContext().getUri(), clientConnection,
             true, false, null, false, subjectToken, context.getHeaders(), verifier -> verifier.withChecks(new TokenVerifier.TokenTypeCheck(List.of(TokenUtil.TOKEN_TYPE_ID))));
        if (authResult == null) {
            return null; // the error is reported by processSubjectToken()
        }
        validateUserSession(authResult);
        return authResult;
    }

    private static boolean isLoggingOut(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        UserSessionModel.State state = userSession.getState();
        if (state == UserSessionModel.State.LOGGING_OUT || state == UserSessionModel.State.LOGGED_OUT
                || state == UserSessionModel.State.LOGGED_OUT_UNCONFIRMED) {
            return true;
        }
        String action = clientSession.getAction();
        return CommonClientSessionModel.Action.LOGGING_OUT.name().equals(action)
            || CommonClientSessionModel.Action.LOGGED_OUT.name().equals(action);
    }

    /**
     * Non-registered claims that may be copied from the evaluated token into the ID-JAG.
     * Only claims that Keycloak protocol mappers actually produce are listed;
     * other optional claims of the draft can be added when there is a use case.
     */
    private static final Set<String> ALLOWED_ID_JAG_CLAIMS = Set.of(
        OAuth2Constants.CLIENT_ID,
        OAuth2Constants.AUTHENTICATOR_METHOD_REFERENCE
    );

    // Required claims come from protocol mappers. When the requesting client uses lightweight access tokens,
    // only mappers with "Add to lightweight access token" enabled contribute claims.
    private static final String LIGHTWEIGHT_HINT = " If the requesting client uses lightweight access tokens, enable 'Add to lightweight access token' on the mapper.";

    @Override
    protected Response buildTokenExchangeResponse(TokenManager.AccessTokenResponseBuilder responseBuilder, String requestedTokenType, List<ClientModel> targetAudienceClients) {

        AccessToken evaluated = responseBuilder.getAccessToken();

        // Required claims that come from protocol mappers. Reject instead of issuing an incomplete ID-JAG.
        String subject = evaluated.getSubject();
        if (subject == null || subject.isBlank()) {
            // Not derived from the user here, so that pairwise subject identifiers are preserved.
            String errorMessage = "Claim 'sub' is missing in ID-JAG. Ensure the 'basic' client scope (or a subject protocol mapper) is assigned to the requesting client"
                    + LIGHTWEIGHT_HINT;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, errorMessage, Response.Status.BAD_REQUEST);
        }

        Object targetClientId = evaluated.getOtherClaims().get(OAuth2Constants.CLIENT_ID);
        // client_id is a string in OAuth (RFC 6749). A hardcoded claim mapper may produce another JSON type,
        // which would be copied unchanged into the signed ID-JAG and break the receiving authorization server.
        if (!(targetClientId instanceof String clientId) || clientId.isBlank()) {
            String errorMessage =
                "Claim 'client_id' must be a nonblank string in ID-JAG. Ensure a Hardcoded Claim protocol mapper with JSON type String is configured in the Client Scope for target: "
                + params.getAudience().get(0) + "." + LIGHTWEIGHT_HINT;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, errorMessage, Response.Status.BAD_REQUEST);
        }

        JsonWebToken idjag = new JsonWebToken();
        idjag.id(SecretGenerator.getInstance().generateSecureID());
        idjag.type(TokenUtil.TOKEN_TYPE_IDJAG);
        idjag.issuer(evaluated.getIssuer());
        idjag.subject(evaluated.getSubject());
        idjag.audience(evaluated.getAudience());
        idjag.iat(evaluated.getIat());
        idjag.exp(evaluated.getExp());

        if (evaluated.getAuth_time() != null) idjag.setOtherClaims(IDToken.AUTH_TIME, evaluated.getAuth_time());
        if (evaluated.getAcr() != null) idjag.setOtherClaims(IDToken.ACR, evaluated.getAcr());

        String grantedScope = evaluated.getScope();
        if (grantedScope != null) idjag.setOtherClaims(OAuth2Constants.SCOPE, grantedScope);

        evaluated.getOtherClaims().forEach((name, value) -> {
             if (ALLOWED_ID_JAG_CLAIMS.contains(name)) {
                idjag.setOtherClaims(name, value);
            }
        });

        // Sign the ID-JAG with the signature algorithm configured for ID tokens
        // (the requesting client's ID token signature algorithm, falling back to the realm default).
        // The ID-JAG is verified by an external authorization server with the realm's published keys (JWKS),
        // so a symmetric (HMAC) algorithm, whose key is not published, cannot be used.
        String signatureAlgorithm = session.tokens().signatureAlgorithm(TokenCategory.ID);
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
        if (signatureProvider == null || !signatureProvider.isAsymmetricAlgorithm()) {
            String errorMessage = "ID-JAG requires an asymmetric signing algorithm, but '" + signatureAlgorithm + "' is configured";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, errorMessage, Response.Status.BAD_REQUEST);
        }
        SignatureSignerContext signer = signatureProvider.signer();

        String encodedIdJag = new JWSBuilder()
                .type(OAuth2Constants.IDENTITY_ASSERTION_JWT_HEADER_TYPE) // "oauth-id-jag+jwt"
                .jsonContent(idjag)
                .sign(signer);

        AccessTokenResponse res = new AccessTokenResponse();
        res.setToken(encodedIdJag);
        res.setTokenType(TokenUtil.TOKEN_TYPE_NA);
        res.setExpiresIn(idjag.getExp() - Time.currentTimeSeconds());
        res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, requestedTokenType);
        res.setScope(grantedScope);

        if (idjag.getAudience() != null) {
            event.detail(Details.AUDIENCE,  CollectionUtil.join(List.of(idjag.getAudience()), " "));
        }
        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    protected List<String> getSupportedOAuthResponseTokenTypes() {
        return Arrays.asList(OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE);
    }

    /**
     * {@link #supports(TokenExchangeContext)} only accepts requests whose requested_token_type is id-jag,
     * so the value is fixed here. The inherited implementation cannot be used because it rejects id-jag.
     */
    @Override
    protected String getRequestedTokenType() {
        return OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE;
    }
}
