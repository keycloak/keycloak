package org.keycloak.protocol.oid4vc.refresh;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
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
