package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

public class ParameterizedScopeMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, TokenIntrospectionTokenMapper, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oidc-parameterized-scope-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ParameterizedScopeMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Parameterized Scope Parameter";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the parameter value from a parameterized scope directly to a token claim.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        ClientScopeModel clientScope = resolveClientScope(mappingModel, clientSessionCtx).orElse(null);
        if (clientScope == null) {
            return;
        }

        List<String> parameterValues = resolveParameterValues(clientScope, clientSessionCtx);
        ProtocolMapperModel model = new ProtocolMapperModel(mappingModel);
        model.getConfig().put(ProtocolMapperUtils.MULTIVALUED, Boolean.toString(TokenManager.isRepeatableScope(keycloakSession, clientScope)));
        if (!parameterValues.isEmpty()) {
            setClaim(token, model, userSession, keycloakSession, clientScope, parameterValues);
        }
    }

    /**
     * Maps resolved parameter values to a token claim. The mapper's {@code multivalued} config
     * controls whether multiple values are mapped as a JSON array or only the first value is used.
     */
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientScopeModel clientScope, List<String> parameterValues) {
        setClaim(token, mappingModel, userSession, keycloakSession, parameterValues);
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, List<String> parameterValues) {
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, parameterValues);
    }

    protected Optional<ClientScopeModel> resolveClientScope(ProtocolMapperModel mappingModel, ClientSessionContext clientSessionCtx) {
        AuthorizationRequestContext ctx = clientSessionCtx.getAuthorizationRequestContext();
        if (ctx == null) {
            return Optional.empty();
        }

        return ctx.getAuthorizationDetailEntries().stream()
                .filter(d -> d.getClientScope() != null && d.isParameterizedScope()
                        && d.getClientScope().getProtocolMapperById(mappingModel.getId()) != null)
                .map(AuthorizationDetails::getClientScope)
                .findAny();
    }

    protected List<String> resolveParameterValues(ClientScopeModel clientScope, ClientSessionContext clientSessionCtx) {
        AuthorizationRequestContext ctx = clientSessionCtx.getAuthorizationRequestContext();
        if (ctx == null) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (AuthorizationDetails detail : ctx.getAuthorizationDetailEntries()) {
            if (detail.getClientScope() != null
                    && detail.getClientScope().getId().equals(clientScope.getId())) {
                String paramValue = detail.getParameterizedScopeParam();
                if (StringUtil.isNotBlank(paramValue)) {
                    values.add(paramValue);
                }
            }
        }
        return values;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.PARAMETERIZED_SCOPES);
    }

    public static ProtocolMapperModel create(String name, String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(
                name, null, tokenClaimName, claimType,
                accessToken, idToken, false, introspectionEndpoint,
                PROVIDER_ID);
        return mapper;
    }
}
