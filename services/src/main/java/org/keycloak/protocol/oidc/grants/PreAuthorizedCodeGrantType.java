package org.keycloak.protocol.oidc.grants;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProvider;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.utils.MediaType;

public class PreAuthorizedCodeGrantType extends OAuth2GrantTypeBase implements EnvironmentDependentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(PreAuthorizedCodeGrantType.class);

    public static final String PROVIDER_ID = "pre-authorization_code";
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:pre-authorized_code";

    @Override
    public String getGrantType() {
        return GRANT_TYPE;
    }

    @Override
    public Response process() {
        LOGGER.debug("Process grant request for preauthorized.");
        String code = formParams.getFirst(OAuth2Constants.CODE);

        if (code == null) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    "Missing parameter: " + OAuth2Constants.CODE, Response.Status.BAD_REQUEST);
        }
        OAuth2CodeParser.ParseResult result = OAuth2CodeParser.parseCode(session, code, realm, event);
        if (result.isIllegalCode()) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code not valid",
                    Response.Status.BAD_REQUEST);
        }
        if (result.isExpiredCode()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code is expired",
                    Response.Status.BAD_REQUEST);
        }
        AuthenticatedClientSessionModel clientSession = result.getClientSession();

        var sessionContext = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession,
                OAuth2Constants.SCOPE_OPENID, session);

        AccessToken accessToken = tokenManager.createClientAccessToken(session,
                clientSession.getRealm(),
                clientSession.getClient(),
                clientSession.getUserSession().getUser(),
                clientSession.getUserSession(),
                sessionContext);

        AccessTokenResponse tokenResponse = tokenManager.responseBuilder(
                        clientSession.getRealm(),
                        clientSession.getClient(),
                        event,
                        session,
                        clientSession.getUserSession(),
                        sessionContext)
                .accessToken(accessToken).build();

        event.success();

        return cors.allowAllOrigins().builder(Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE)).build();
    }

    @Override
    public OAuth2GrantType create(KeycloakSession session) {
        LOGGER.debugf("Pre-Authorized supported");
        return new PreAuthorizedCodeGrantType();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.CIBA);
    }
}