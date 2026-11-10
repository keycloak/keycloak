package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.urls.UrlType;

public class AdminApiAudienceMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, TokenIntrospectionTokenMapper {

    public static final String PROVIDER_ID = "oidc-admin-api-audience-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AdminApiAudienceMapper.class);

        configProperties.removeIf(prop -> OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN.equals(prop.getName()));

        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Admin API Audience";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds the Admin REST API audience (the realm Admin API base URL) to the token.";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        token.addAudience(getAdminApiAudience(keycloakSession, keycloakSession.getContext().getRealm()));
    }

    public static String getAdminApiAudience(KeycloakSession session, RealmModel realm) {
        return AdminRoot.realmsUrl(session.getContext().getUri(UrlType.FRONTEND).getBaseUriBuilder())
                .path(realm.getName())
                .build()
                .toString();
    }

    public static ProtocolMapperModel createMapper(String name, boolean accessToken, boolean introspectionEndpoint, boolean lightweightAccessToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, String.valueOf(accessToken));
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, String.valueOf(introspectionEndpoint));
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN, String.valueOf(lightweightAccessToken));
        mapper.setConfig(config);

        return mapper;
    }
}
