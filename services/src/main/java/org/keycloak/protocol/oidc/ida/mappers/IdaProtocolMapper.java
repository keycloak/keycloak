package org.keycloak.protocol.oidc.ida.mappers;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.ida.mappers.connector.IdaConnector;
import org.keycloak.protocol.oidc.ida.mappers.extractor.VerifiedClaimExtractor;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_REQUEST_CLAIMS_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_REQUEST_CLAIMS_JSON_SYNTAX_ERROR_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.IDA_DISPLAY_TYPE;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.IDA_HELP_TEXT;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.USERINFO;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.VERIFIED_CLAIMS;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME;
import static org.keycloak.protocol.oidc.utils.OIDCResponseType.ID_TOKEN;
import static org.keycloak.util.TokenUtil.TOKEN_TYPE_BEARER;
import static org.keycloak.util.TokenUtil.TOKEN_TYPE_ID;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.IDA_PROVIDER_ID;

/**
 * Support an extension of OpenID Connect for providing Replying Parties with Verified Claims about End-Users
 * https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html
 */
public class IdaProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper,
        UserInfoTokenMapper, EnvironmentDependentProviderFactory {
    private static final Logger logger = Logger.getLogger(IdaProtocolMapper.class);

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, IdaProtocolMapper.class);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        IdaConnector idaConnector = factory.create().getProvider(IdaConnector.class);
        idaConnector.addIdaExternalStore(configProperties);
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return IDA_DISPLAY_TYPE;
    }

    @Override
    public String getHelpText() {
        return IDA_HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return IDA_PROVIDER_ID;
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client,
                               ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        IdaConnector idaConnector = session.getProvider(IdaConnector.class);
        idaConnector.validateIdaExternalStore(mapperModel.getConfig());
    }

    @Override
    protected void setClaim(final IDToken token,
                            final ProtocolMapperModel mappingModel,
                            final UserSessionModel userSession,
                            final KeycloakSession keycloakSession,
                            final ClientSessionContext clientSessionCtx) {
        // Obtaining request claims
        AuthenticatedClientSessionModel acs = clientSessionCtx.getClientSession();
        String requestString = acs.getNote(OIDCLoginProtocol.CLAIMS_PARAM);

        List<Map<String, Object>> requestClaims = getRequestClaims(requestString, token.getType());

        if (!requestClaims.isEmpty()) {
            // Retrieving Verified Claims for a user from an external store
            IdaConnector idaConnector = keycloakSession.getProvider(IdaConnector.class);
            Map<String, Object> userAllClaims = idaConnector.getVerifiedClaims(mappingModel.getConfig(), userSession.getUser().getUsername());
            // Filtering request claims from validated claims in an external store
            List<Map<String, Object>> extractedClaims = new ArrayList<>();
            for (Map<String, Object> requestClaim : requestClaims) {
                Map<String, Object> extractedClaim = new VerifiedClaimExtractor(OffsetDateTime.now()).getFilteredClaims(requestClaim, userAllClaims);
                extractedClaims.add(extractedClaim);
            }
            // Mapping filtering results to output
            mappingModel.getConfig().put(TOKEN_CLAIM_NAME, VERIFIED_CLAIMS);

            if (isArray(extractedClaims)) {
                // verified Claims is array.
                mappingModel.getConfig().put(ProtocolMapperUtils.MULTIVALUED, "true");
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, extractedClaims);
            } else {
                // verified Claims is single value.
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, extractedClaims.get(0));
            }
        }
    }

    private boolean isArray(List<Map<String, Object>> verifiedClaims) {
        return !(verifiedClaims.size() == 1);
    }

    private List<Map<String, Object>> getRequestClaims(String requestString, String tokenType) {
        Map<String, Object> requestMap = getRequestMap(requestString);
        List<Map<String, Object>> requestClaims = new ArrayList<>();
        String endpointKey = getEndPointKey(tokenType);
        if(endpointKey != null) {
            if (requestMap.get(endpointKey) instanceof Map) {
                Map<String, Object> idTokenValue = (Map) requestMap.get(endpointKey);
                if (idTokenValue.get(VERIFIED_CLAIMS) instanceof Map) {
                    requestClaims.add((Map) idTokenValue.get(VERIFIED_CLAIMS));
                } else if (idTokenValue.get(VERIFIED_CLAIMS) instanceof List) {
                    requestClaims = (List) idTokenValue.get(VERIFIED_CLAIMS);
                }
            }
        }
        return requestClaims;
    }

    private Map<String, Object> getRequestMap(String requestString) {
        if (requestString.isEmpty()) {
            logger.errorf(ERROR_MESSAGE_REQUEST_CLAIMS_ERROR);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, ERROR_MESSAGE_REQUEST_CLAIMS_ERROR,
                    Response.Status.BAD_REQUEST);
        }
        try {
            return JsonSerialization.readValue(requestString, Map.class);
        } catch (IOException e) {
            logger.errorf(ERROR_MESSAGE_REQUEST_CLAIMS_JSON_SYNTAX_ERROR_ERROR);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, ERROR_MESSAGE_REQUEST_CLAIMS_JSON_SYNTAX_ERROR_ERROR,
                    Response.Status.BAD_REQUEST);
        }
    }

    private String getEndPointKey(String tokenType) {
        if (tokenType == null) {
            // transformUserInfoToken
            return USERINFO;
        }
        switch (tokenType) {
            // transformIDToken
            case TOKEN_TYPE_ID:
            // transformAccessToken
            case TOKEN_TYPE_BEARER:
                return ID_TOKEN;
            default:
                return null;
        }
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.IDA);
    }
}
