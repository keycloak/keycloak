package org.keycloak.testframework.oauth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;

public class DefaultOAuthClientConfiguration implements ClientConfig {

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
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
                .secret("test-secret")
                .protocolMappers(Collections.singletonList(audienceMapper));
    }

}
