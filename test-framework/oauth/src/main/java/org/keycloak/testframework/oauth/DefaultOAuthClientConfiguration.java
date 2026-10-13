package org.keycloak.testframework.oauth;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;

public class DefaultOAuthClientConfiguration implements ClientConfig {

    @Override
    public ClientBuilder configure(ClientBuilder client) {
        ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
        audienceMapper.setName("audience-test-app");
        audienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

        Map<String, String> audienceConfig = new HashMap<>();
        audienceConfig.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, "test-app");
        audienceConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        audienceMapper.setConfig(audienceConfig);

        return client.clientId("test-app")
                .serviceAccountsEnabled(true)
                .directAccessGrantsEnabled(true)
                .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, "authorization-grant-idp-alias")
                .secret("test-secret")
                .protocolMappers(audienceMapper);
    }

}
