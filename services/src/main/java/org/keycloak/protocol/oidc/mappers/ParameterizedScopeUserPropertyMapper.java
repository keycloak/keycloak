package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.scope.ParameterizedScopeTypeProvider;
import org.keycloak.protocol.oidc.scope.UsernameScopeType;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

public class ParameterizedScopeUserPropertyMapper extends ParameterizedScopeMapper {

    private static final Logger logger = Logger.getLogger(ParameterizedScopeUserPropertyMapper.class);

    public static final String PROVIDER_ID = "oidc-parameterized-scope-user-property-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText("User profile attribute or built-in property (e.g. id, email, firstName) to map to the token claim.");
        property.setType(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE);
        configProperties.add(property);

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ParameterizedScopeUserPropertyMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Parameterized Scope User Property";
    }

    @Override
    public String getHelpText() {
        return "Resolves a user from a parameterized scope parameter (username) and maps a user attribute or property to a token claim.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientScopeModel clientScope, List<String> parameterValues) {
        String attributeName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
        if (StringUtil.isBlank(attributeName)) {
            return;
        }

        ParameterizedScopeTypeProvider scopeType = keycloakSession.getProvider(ParameterizedScopeTypeProvider.class, clientScope.getParameterizedScopeType());
        RealmModel realm = userSession.getRealm();

        List<Object> resolvedValues = new ArrayList<>();
        for (String parameterValue : parameterValues) {
            UserModel targetUser = keycloakSession.users().getUserByUsername(realm, parameterValue);
            if (targetUser == null) {
                continue;
            }

            boolean accessDenied = scopeType instanceof UsernameScopeType usernameType && !usernameType.canAccessTargetUser(clientScope, userSession.getUser(), targetUser);
            if (accessDenied) {
                logger.debugf("Skipping user property claim for target user '%s' — authenticated user '%s' is not authorized to access the target user's data",
                        parameterValue, userSession.getUser().getUsername());
                continue;
            }

            Collection<String> attributeValue = KeycloakModelUtils.resolveAttribute(targetUser, attributeName, false);
            if (CollectionUtil.isNotEmpty(attributeValue)) {
                resolvedValues.addAll(attributeValue);
                continue;
            }

            String propertyValue = ProtocolMapperUtils.getUserModelValue(targetUser, attributeName);
            if (propertyValue != null) {
                resolvedValues.add(propertyValue);
            }
        }

        if (!resolvedValues.isEmpty()) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, resolvedValues);
        }
    }

    public static ProtocolMapperModel create(String name, String userAttribute,
                                              String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return create(name, userAttribute, tokenClaimName, claimType, accessToken, idToken, introspectionEndpoint, false);
    }

    public static ProtocolMapperModel create(String name, String userAttribute,
                                              String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint,
                                              boolean multivalued) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(
                name, userAttribute, tokenClaimName, claimType,
                accessToken, idToken, false, introspectionEndpoint,
                PROVIDER_ID);
        mapper.getConfig().put(ProtocolMapperUtils.MULTIVALUED, Boolean.toString(multivalued));
        return mapper;
    }
}
