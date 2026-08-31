package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

public class ParameterizedScopeClientSubMapper extends ParameterizedScopeMapper {

    public static final String PROVIDER_ID = "oidc-parameterized-scope-client-sub-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ParameterizedScopeClientSubMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Parameterized Scope Client Sub";
    }

    @Override
    public String getHelpText() {
        return "Resolves a client from the parameterized scope parameter (client ID), obtains its service account user, and maps the user ID to the token claim.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, List<String> parameterValues) {
        List<Object> resolvedValues = new ArrayList<>();
        for (String parameterValue : parameterValues) {
            ClientModel client = userSession.getRealm().getClientByClientId(parameterValue);
            if (client == null) {
                continue;
            }
            UserModel serviceAccount = keycloakSession.users().getServiceAccount(client);
            if (serviceAccount == null) {
                continue;
            }
            resolvedValues.add(serviceAccount.getId());
        }

        if (!resolvedValues.isEmpty()) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, resolvedValues);
        }
    }

    public static ProtocolMapperModel create(String name, String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConfig(new HashMap<>());
        mapper.getConfig().put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        mapper.getConfig().put(OIDCAttributeMapperHelper.JSON_TYPE, claimType);
        if (accessToken) mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        if (introspectionEndpoint) mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        return mapper;
    }
}
