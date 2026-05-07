package org.keycloak.organization.protocol.mappers.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.RefreshToken;

public class OrganizationTokenPostProcessor implements TokenPostProcessor{

    private final KeycloakSession session;

    public OrganizationTokenPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        String grantType = context.clientSessionCtx().getAttribute(Constants.GRANT_TYPE, String.class);

        if (OAuth2Constants.REFRESH_TOKEN.equals(grantType)) {
            RefreshToken refreshToken = context.requestRefreshToken();

            if (refreshToken != null) {
                Object orgAlias = refreshToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
                if (orgAlias != null) {
                    addOrganizationRefreshTokenClaim(context, orgAlias.toString());
                }
            }
        } else {
            OrganizationModel organization = session.getContext().getOrganization();

            if (organization != null) {
                addOrganizationRefreshTokenClaim(context, organization.getAlias());
            }
        }
    }

    private void addOrganizationRefreshTokenClaim(TokenPostProcessorContext context, String orgAlias) {
        if (orgAlias == null) {
            return;
        }

        OrganizationProvider provider = Organizations.getProvider(session);
        ClientSessionContext clientSessionContext = context.clientSessionCtx();
        AuthenticatedClientSessionModel clientSession = clientSessionContext.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        OrganizationModel organization = provider.getByAlias(orgAlias);

        if (organization == null || !organization.isEnabled()) {
            throw new TokenInterceptorException(OAuthErrorException.INVALID_REQUEST, OAuthErrorException.INVALID_TOKEN);
        }

        UserModel user = userSession.getUser();

        if (user != null && !organization.isMember(user)) {
            throw new TokenInterceptorException(OAuthErrorException.INVALID_REQUEST, OAuthErrorException.INVALID_TOKEN);
        }

        RefreshToken newRefreshToken = context.refreshToken();

        if (newRefreshToken != null) {
            newRefreshToken.getOtherClaims().put(OAuth2Constants.ORGANIZATION, orgAlias);
        }
    }
}
