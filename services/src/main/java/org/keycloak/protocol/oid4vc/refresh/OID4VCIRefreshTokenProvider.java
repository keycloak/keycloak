package org.keycloak.protocol.oid4vc.refresh;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeUtils;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.refresh.AbstractRefreshTokenProvider;
import org.keycloak.protocol.oidc.refresh.InitialRefreshTokenContext;
import org.keycloak.protocol.oidc.refresh.RefreshTokenContext;
import org.keycloak.protocol.oidc.refresh.RefreshTokenException;
import org.keycloak.protocol.oidc.refresh.RefreshTokenProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_CODE;
import static org.keycloak.OAuth2Constants.REFRESH_TOKEN;
import static org.keycloak.OAuthErrorException.INVALID_REQUEST;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;
import static org.keycloak.models.UserSessionModel.SessionPersistenceState.TRANSIENT;

public class OID4VCIRefreshTokenProvider extends AbstractRefreshTokenProvider implements RefreshTokenProvider {

    private static final Logger logger = Logger.getLogger(OID4VCIRefreshTokenProvider.class);
    private static final String ROTATION_KEY_PREFIX = OID4VCIRefreshTokenProvider.class.getName().toLowerCase(Locale.ROOT) + ".rotation.";
    private static final String NOTE_ACCEPTED_TOKEN_ID = "acceptedTokenId";
    private static final String NOTE_LATEST_GENERATED_TOKEN_ID = "latestGeneratedTokenId";
    private static final String NOTE_USE_COUNT = "useCount";
    private static final String NOTE_LAST_REFRESH = "lastRefresh";
    private static final int ROTATION_RECORD_CLOCK_SKEW_SECONDS = 10;
    private String pendingRotationKey;
    private Map<String, String> pendingRotationRecord;

    public OID4VCIRefreshTokenProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public boolean supports(InitialRefreshTokenContext initialRefreshTokenCtx) {
        ClientSessionContext clientSessionCtx = initialRefreshTokenCtx.clientSessionCtx();

        // Supported only for authorization_code grant type and refresh-token grant
        String grantType = clientSessionCtx.getAttribute(Constants.GRANT_TYPE, String.class);
        if (!AUTHORIZATION_CODE.equals(grantType) && !REFRESH_TOKEN.equals(grantType)) {
            return false;
        }

        // Check any 'oid4vci' client scope is present
        return clientSessionCtx.getClientScopesStream()
                .anyMatch(it -> OID4VC_PROTOCOL.equals(it.getProtocol()));
    }

    @Override
    public RefreshToken generateRefreshToken(InitialRefreshTokenContext initialRefreshTokenCtx) throws RefreshTokenException {
        ClientSessionContext clientSessionCtx = initialRefreshTokenCtx.clientSessionCtx();
        TokenManager.AccessTokenResponseBuilder responseBuilder = initialRefreshTokenCtx.responseBuilder();
        AccessToken accessToken = responseBuilder.getAccessToken();
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        UserModel user = clientSession.getUserSession().getUser();

        logger.tracev("Generating refresh token for oid4vci. Realm: {0}, user: {1}, client: {2}", session.getContext().getRealm().getName(),
                user.getUsername(), session.getContext().getClient().getClientId());

        RefreshToken refreshToken = createRefreshToken(accessToken, initialRefreshTokenCtx.confirmation(), OID4VCIRefreshTokenProviderFactory.PROVIDER_ID);

        if (refreshToken.getSubject() == null) {
            refreshToken.subject(user.getId());
        }

        if (initialRefreshTokenCtx.offlineTokenRequested()) {
            throw new RefreshTokenException(INVALID_REQUEST, "Unsupported to request offline access together with oid4vci credential");
        } else {
            refreshToken.exp(getExpiration(clientSessionCtx, user));
        }

        // Likely should not need to support this for OID4VCI refresh tokens
        final ClientModel[] requestedAudienceClients = clientSessionCtx.getAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, ClientModel[].class);
        if (requestedAudienceClients != null) {
            throw new RefreshTokenException(INVALID_REQUEST, "Unsupported to request audience clients together with oid4vci");
        }

        return refreshToken;
    }

    @Override
    public boolean supports(RefreshTokenContext ctx) {
        RefreshToken oldRefreshToken = ctx.oldRefreshToken();
        return (TokenUtil.TOKEN_TYPE_REFRESH.equals(oldRefreshToken.getType())  && OID4VCIRefreshTokenProviderFactory.PROVIDER_ID.equals(oldRefreshToken.getProvider()));
    }


    @Override
    protected TokenManager.TokenValidation validateToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm,
                                                         RefreshToken oldToken, HttpHeaders headers, String scope, ClientModel client,
                                                         TokenManager tokenManager, EventBuilder event) throws OAuthErrorException {
        List<AuthorizationDetailsJSONRepresentation> authzDetails = oldToken.getAuthorizationDetails();
        if (authzDetails == null || authzDetails.isEmpty()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_TOKEN, "Authorization details not found in the old refresh token");
        }
        OID4VCAuthorizationDetail oid4vcAuthzDetail = getOid4vcAuthzDetail(authzDetails);

        // Find user
        UserModel user = getUser(realm, oldToken);
        if (user == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", "Unknown user");
        }
        if (!user.isEnabled()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "User disabled", "User disabled");
        }

        // Create transient sessions
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAuthenticatedUser(user);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(authSession.getParentSession().getId(), realm, user, user.getUsername(),
                connection.getRemoteHost(), "oid4vci-refresh-token", false, null, null, TRANSIENT);

        event.session(userSession);

        AuthenticationManager.setClientScopesInSession(session, authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);
        clientSessionCtx.setAttribute(Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);

        CredentialScopeModel credentialScopeModel = CredentialScopeUtils.findCredentialScopeModelByConfigurationId(session.getContext().getRealm(), clientSessionCtx::getClientScopesStream, oid4vcAuthzDetail.getCredentialConfigurationId());
        if (credentialScopeModel == null) {
            throw new OAuthErrorException(INVALID_REQUEST, "Not found credential scope model in current clientSessionCtx with credential configuration id: " + oid4vcAuthzDetail.getCredentialConfigurationId());
        }

        event.detail(Details.CREDENTIAL_TYPE, oid4vcAuthzDetail.getCredentialConfigurationId());
        checkIssuedVerifiableCredential(session, user, oid4vcAuthzDetail.getIssuedCredentialId(), credentialScopeModel, clientSessionCtx.getClientSession().getClient());

        return new TokenManager.TokenValidation(user, userSession, clientSessionCtx);
    }

    @Override
    protected void afterRefreshTokenGenerated(RefreshTokenContext ctx, TokenManager.AccessTokenResponseBuilder responseBuilder) {
        // Run before the authorization_details early return below, otherwise the rotation record is never written
        flushRotationRecord(ctx, responseBuilder.getRefreshToken());

        ClientSessionContext clientSessionCtx = responseBuilder.getClientSessionCtx();
        List<AuthorizationDetailsJSONRepresentation> authzDetails = clientSessionCtx.getAttribute(AUTHORIZATION_DETAILS_RESPONSE, List.class);

        if (authzDetails == null) {
            return;
        }

        List<AuthorizationDetailsJSONRepresentation> clearedDetails = new ArrayList<>(authzDetails.size());
        for (AuthorizationDetailsJSONRepresentation d : authzDetails) {
            if (OPENID_CREDENTIAL.equals(d.getType())) {
                OID4VCAuthorizationDetail typed = d.asSubtype(OID4VCAuthorizationDetail.class);
                typed.setCredentialsOfferId(null);
                clearedDetails.add(typed);
            } else {
                clearedDetails.add(d);
            }
        }

        responseBuilder.getAccessToken().setAuthorizationDetails(clearedDetails);
        if (responseBuilder.getRefreshToken() != null) {
            responseBuilder.getRefreshToken().setAuthorizationDetails(clearedDetails);
        }

        clientSessionCtx.setAttribute(AUTHORIZATION_DETAILS_RESPONSE, clearedDetails);
    }


    private void flushRotationRecord(RefreshTokenContext ctx, RefreshToken newRefreshToken) {
        //Retrieve the staged token from in-memory fields and immediately clear them
        String key = pendingRotationKey;
        Map<String, String> record = pendingRotationRecord;

        pendingRotationKey = null;
        pendingRotationRecord = null;

        if (key == null || record == null) {
            return;
        }

        // Complete the record with newly generated child token's metadata
        RefreshToken lifespanSource = ctx.oldRefreshToken();
        if (newRefreshToken != null) {
            // Record new Refresh token as the most recent valid child in the family
            record.put(NOTE_LATEST_GENERATED_TOKEN_ID, newRefreshToken.getId());
            record.put(NOTE_LAST_REFRESH, String.valueOf(newRefreshToken.getIat()));
            lifespanSource = newRefreshToken;
        }
        // Completes the read-modify-write started in validateTokenReuseForRefresh. It is atomic because
        // getRefreshTokenLockId() keys the refresh lock on the same family key as the record, and that lock is only
        // released once this transaction has committed.
        storeRotationRecord(session.singleUseObjects(), key, lifespanSource, record);
    }

    @Override
    public void revokeToken(AccessToken token, UserModel user, ClientModel client, EventBuilder event) {
        // Revoke the issued verifiable credential associated with this refresh token
        List<AuthorizationDetailsJSONRepresentation> authorizationDetails = token.getAuthorizationDetails();
        if (authorizationDetails == null || authorizationDetails.isEmpty()) {
            logger.warnf("OID4VCI refresh token revoked but authorization_details is missing. " +
                            "Realm: %s, client: %s, user: %s",
                    session.getContext().getRealm().getName(), client.getClientId(), user.getUsername());
            return;
        }

        for (AuthorizationDetailsJSONRepresentation detail : authorizationDetails) {
            if (!OPENID_CREDENTIAL.equals(detail.getType())) {
                continue;
            }
            OID4VCAuthorizationDetail oid4VCDetail = detail.asSubtype(OID4VCAuthorizationDetail.class);
            String issuedCredentialId = oid4VCDetail.getIssuedCredentialId();
            if (issuedCredentialId == null) {
                continue;
            }
            boolean ownedByUserAndClient = session.users().getIssuedVerifiableCredentialsStreamByUser(user.getId())
                    .anyMatch(issued -> issuedCredentialId.equals(issued.getId()) && client.getId().equals(issued.getClientId()));

            if (!ownedByUserAndClient) {
                logger.warnf("Issued verifiable credential '%s' referenced by revoked refresh token is not associated with current user/client. Realm: %s, client: %s, user: %s",
                        issuedCredentialId, session.getContext().getRealm().getName(), client.getClientId(), user.getUsername());
                continue;
            }

            boolean removed = session.users().removeIssuedVerifiableCredential(issuedCredentialId);

            if (!removed) {
                logger.warnf("Failed to remove issued verifiable credential '%s' on refresh token revocation. Realm: %s, client: %s, user: %s",
                        issuedCredentialId, session.getContext().getRealm().getName(), client.getClientId(), user.getUsername());
                continue;
            }

            logger.debugf("Removed issued verifiable credential '%s' on refresh token revocation. " +
                            "Realm: %s, client: %s, user: %s",
                    issuedCredentialId, session.getContext().getRealm().getName(),
                    client.getClientId(), user.getUsername());
        }
    }

    @Override
    public String getProviderId() {
        return OID4VCIRefreshTokenProviderFactory.PROVIDER_ID;
    }

    /**
     * Every OID4VCI refresh mints a fresh transient session, and {@link OID4VCITokenPostProcessor} clears the session
     * id on the tokens produced from it. The inherited session scoped lock id would therefore differ between a token
     * and its rotated child, even though both contend on the same rotation record. Key the lock on the family instead,
     * exactly as the record itself is keyed.
     */
    @Override
    protected String getRefreshTokenLockId(RealmModel realm, RefreshToken refreshToken, TokenManager tokenManager) {
        return "refreshLock:" + getRotationKey(realm, refreshToken);
    }

    /**
     * The client session created in {@link #validateToken} is TRANSIENT, so it is discarded at the end of the request
     * and the rotation state the default implementation keeps on it is always empty. Keep the equivalent state in the
     * single-use object store instead, keyed by the refresh token family.
     * <p>
     * Mirrors the logic of {@link TokenManager#validateTokenReuse}.
     * The updated record is only staged here and written once in {@link #flushRotationRecord}, which also knows the id of the newly generated token.
     */
    @Override
    protected void validateTokenReuseForRefresh(KeycloakSession session, RealmModel realm, RefreshToken refreshToken, TokenManager.TokenValidation validation, TokenManager tokenManager) throws OAuthErrorException {
        pendingRotationKey = null;
        pendingRotationRecord = null;

        if (!realm.isRevokeRefreshToken()) {
            return;
        }

        if (refreshToken.getExp() == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", "Refresh token has no expiration");
        }

        String key = getRotationKey(realm, refreshToken);
        Map<String, String> record = session.singleUseObjects().get(key);

        String acceptedTokenId = getNote(record, NOTE_ACCEPTED_TOKEN_ID);
        String latestGeneratedTokenId = getNote(record, NOTE_LATEST_GENERATED_TOKEN_ID);
        int useCount = getIntNote(record, NOTE_USE_COUNT);
        int lastRefresh = getIntNote(record, NOTE_LAST_REFRESH);

        if (acceptedTokenId != null && !refreshToken.getId().equals(acceptedTokenId)) {
            // A different token of this family was already accepted. Anything issued no later than the most recently
            // generated token is a replay of an already rotated token.
            if (latestGeneratedTokenId != null && !refreshToken.getId().equals(latestGeneratedTokenId) && refreshToken.getIat() <= lastRefresh) {
                logger.debugf("Rejecting replayed oid4vci refresh token %s. Realm: %s, client: %s",
                        refreshToken.getId(), realm.getName(), session.getContext().getClient().getClientId());
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
            }
            // Strictly newer token: it becomes the accepted one and its reuse counter starts over
            useCount = 0;
        }

        if (useCount > realm.getRefreshTokenMaxReuse()) {
            logger.debugf("Rejecting oid4vci refresh token %s due to exceeding max reuse count. Realm: %s, client: %s",
                    refreshToken.getId(), realm.getName(), session.getContext().getClient().getClientId());
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Maximum allowed refresh token reuse exceeded",
                    "Maximum allowed refresh token reuse exceeded");
        }

        Map<String, String> updated = record == null ? new HashMap<>() : new HashMap<>(record);
        updated.put(NOTE_ACCEPTED_TOKEN_ID, refreshToken.getId());
        updated.put(NOTE_USE_COUNT, String.valueOf(useCount + 1));
        pendingRotationKey = key;
        pendingRotationRecord = updated;
    }

    private void storeRotationRecord(SingleUseObjectProvider singleUseStore, String key, RefreshToken refreshToken, Map<String, String> record) {
        Long expiration = refreshToken.getExp();
        long lifeSpan = (expiration == null ? 0 : expiration - Time.currentTimeSeconds()) + ROTATION_RECORD_CLOCK_SKEW_SECONDS;
        if (lifeSpan <= 0) {
            return; // already expired, it can never be successfully replayed anyway
        }
        singleUseStore.put(key, lifeSpan, record);
    }

    private int getIntNote(Map<String, String> record, String name) {
        String value = getNote(record, name);
        return value != null ? Integer.parseInt(value) : 0;
    }

    private String getNote(Map<String, String> record, String name) {
        return record == null ? null : record.get(name);
    }

    /**
     * The reuse_id claim is generated once per refresh token family and copied onto every rotated token, so it
     * identifies the whole chain. Tokens issued before revoke-refresh-token was enabled have none, in which case this
     * degrades to strict single-use of that one token.
     */
    private String getRotationKey(RealmModel realm, RefreshToken refreshToken) {
        Object reuseId = refreshToken.getOtherClaims().get(Constants.REUSE_ID);
        String familyId = reuseId != null ? String.valueOf(reuseId) : refreshToken.getId();
        return ROTATION_KEY_PREFIX + HashUtils.sha256UrlEncodedHash(realm.getId() + "." + familyId, StandardCharsets.UTF_8);
    }

    // Might eventually be overridden for scenarios where a user is not available in the Keycloak DB
    protected UserModel getUser(RealmModel realm, RefreshToken oldToken) {
        String userId = oldToken.getSubject();
        return session.users().getUserById(realm, userId);
    }

    protected IssuedVerifiableCredentialModel checkIssuedVerifiableCredential(KeycloakSession session, UserModel user, String issuedCredentialId, CredentialScopeModel expectedCredentialScope, ClientModel expectedClient) {
        try {
            return OID4VCUtil.checkIssuedVerifiableCredential(session, user, issuedCredentialId, expectedCredentialScope, expectedClient);
        } catch (IllegalStateException ise) {
            throw new RefreshTokenException(INVALID_REQUEST, ise.getMessage());
        }
    }


    private long getExpiration(ClientSessionContext clientSessionCtx, UserModel user) {
        List<AuthorizationDetailsJSONRepresentation> authzDetails = clientSessionCtx.getAttribute(AUTHORIZATION_DETAILS_RESPONSE, List.class);
        if (authzDetails == null || authzDetails.isEmpty()) {
            throw new RefreshTokenException(INVALID_REQUEST, "Authorization details not found in the client session context");
        }

        OID4VCAuthorizationDetail oid4vcAuthzDetail = getOid4vcAuthzDetail(authzDetails);

        CredentialScopeModel credentialScopeModel = CredentialScopeUtils.findCredentialScopeModelByConfigurationId(session.getContext().getRealm(), clientSessionCtx::getClientScopesStream, oid4vcAuthzDetail.getCredentialConfigurationId());
        if (credentialScopeModel == null) {
            throw new RefreshTokenException(INVALID_REQUEST, "Not found credential scope model in current clientSessionCtx with credential configuration id: " + oid4vcAuthzDetail.getCredentialConfigurationId());
        }

        IssuedVerifiableCredentialModel issuedVerifiableCredentialModel = checkIssuedVerifiableCredential(session, user, oid4vcAuthzDetail.getIssuedCredentialId(), credentialScopeModel, clientSessionCtx.getClientSession().getClient());
        return (issuedVerifiableCredentialModel.getExpiresAt() / 1000); // Expiry saved on credential is in milliseconds
    }

    private OID4VCAuthorizationDetail getOid4vcAuthzDetail(List<AuthorizationDetailsJSONRepresentation> authzDetails) {
        List<OID4VCAuthorizationDetail> oid4vcAuthzDetails = authzDetails.stream()
                .filter(authzDetail -> OPENID_CREDENTIAL.equals(authzDetail.getType()))
                .map(authzDetail -> authzDetail.asSubtype(OID4VCAuthorizationDetail.class))
                .toList();
        // Aligned with other places in Keycloak codebase to support single VC
        if (oid4vcAuthzDetails.size() != 1) {
            throw new RefreshTokenException(INVALID_REQUEST, "Supporting single OID4VCI authorization detail for now");
        }
        return oid4vcAuthzDetails.get(0);
    }
}
