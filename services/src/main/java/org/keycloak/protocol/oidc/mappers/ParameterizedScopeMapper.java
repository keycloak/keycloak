package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
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
        addScopeConditionConfig(configProperties);
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ParameterizedScopeMapper.class);
    }

    protected static void addScopeConditionConfig(List<ProviderConfigProperty> properties) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(OIDCAttributeMapperHelper.SCOPE_CONDITION);
        property.setLabel("Scope Condition");
        property.setHelpText("When this mapper is placed on a client, specify the parameterized scope whose parameter value should be resolved. Leave empty when the mapper is placed on a client scope.");
        property.setType(ProviderConfigProperty.CLIENT_SCOPE_LIST_TYPE);
        property.setOptions(List.of(ClientScopeModel.IS_PARAMETERIZED_SCOPE));
        properties.add(property);
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

        if (isOverriddenByClientMapper(mappingModel, clientScope, clientSessionCtx)) {
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
     * A scope-level mapper (no {@code scope.condition}) is overridden when the client has its own
     * mapper targeting the same scope and claim name via {@code scope.condition}.
     */
    private boolean isOverriddenByClientMapper(ProtocolMapperModel mappingModel, ClientScopeModel clientScope,
                                               ClientSessionContext clientSessionCtx) {
        if (StringUtil.isNotBlank(mappingModel.getConfig().get(OIDCAttributeMapperHelper.SCOPE_CONDITION))) {
            return false;
        }

        String claimName = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        if (StringUtil.isBlank(claimName)) {
            return false;
        }

        String scopeName = clientScope.getName();
        ClientModel client = clientSessionCtx.getClientSession().getClient();

        return client.getProtocolMappersStream()
                .anyMatch(m -> scopeName.equals(m.getConfig().get(OIDCAttributeMapperHelper.SCOPE_CONDITION))
                        && claimName.equals(m.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME)));
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

    /**
     * Resolves the parameterized client scope this mapper should bind to.
     *
     * <p>First tries to find a scope that directly contains this mapper (scope-level placement).
     * If not found, falls back to matching by the {@code scope.condition} config property,
     * which allows client-level mappers to bind to a parameterized scope by name.</p>
     */
    protected Optional<ClientScopeModel> resolveClientScope(ProtocolMapperModel mappingModel, ClientSessionContext clientSessionCtx) {
        AuthorizationRequestContext ctx = clientSessionCtx.getAuthorizationRequestContext();
        if (ctx == null) {
            return Optional.empty();
        }

        // Scope-level placement: the mapper belongs directly to the parameterized scope
        Optional<ClientScopeModel> result = findParameterizedScope(ctx, detail -> detail.getClientScope().getProtocolMapperById(mappingModel.getId()) != null);
        if (result.isPresent()) {
            return result;
        }

        // Client-level placement: resolve scope by the configured scope.condition name
        String scopeCondition = mappingModel.getConfig().get(OIDCAttributeMapperHelper.SCOPE_CONDITION);
        if (StringUtil.isBlank(scopeCondition)) {
            return Optional.empty();
        }

        return findParameterizedScope(ctx, detail -> scopeCondition.equals(detail.getClientScope().getName()));
    }

    /**
     * Finds a granted parameterized scope matching the given condition.
     */
    private Optional<ClientScopeModel> findParameterizedScope(AuthorizationRequestContext ctx, Predicate<AuthorizationDetails> condition) {
        return ctx.getAuthorizationDetailEntries().stream()
                .filter(detail -> detail.getClientScope() != null && detail.isParameterizedScope() && condition.test(detail))
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
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel container, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        String scopeCondition = mapperModel.getConfig() != null
                ? mapperModel.getConfig().get(OIDCAttributeMapperHelper.SCOPE_CONDITION) : null;
        if (StringUtil.isNotBlank(scopeCondition) && !(container instanceof ClientModel)) {
            throw new ProtocolMapperConfigException("Scope condition is only supported on client-level mappers",
                    "scopeConditionClientOnly");
        }
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.PARAMETERIZED_SCOPES);
    }

    public static ProtocolMapperModel create(String name, String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return create(name, tokenClaimName, claimType, accessToken, idToken, introspectionEndpoint, null);
    }

    public static ProtocolMapperModel create(String name, String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint,
                                              String scopeCondition) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(
                name, null, tokenClaimName, claimType,
                accessToken, idToken, false, introspectionEndpoint,
                PROVIDER_ID);
        if (scopeCondition != null) {
            mapper.getConfig().put(OIDCAttributeMapperHelper.SCOPE_CONDITION, scopeCondition);
        }
        return mapper;
    }
}
