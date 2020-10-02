package org.keycloak.testsuite.broker;

import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class KcOidcBrokerClientUserInfoTest extends AbstractBrokerTest {

    protected static final String ATTRIBUTE_TO_MAP_USER_INFO = "user-attribute-ufo";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationUserInfoOnlyMappers();
    }

    private class KcOidcBrokerConfigurationUserInfoOnlyMappers extends KcOidcBrokerConfiguration {

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientsRepList = super.createProviderClients();
            log.info("Update provider clients to disable attributes in Access & ID token");

            ProtocolMapperRepresentation userAttrMapper = new ProtocolMapperRepresentation();
            userAttrMapper.setName("attribute - name");
            userAttrMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            userAttrMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

            Map<String, String> userAttrMapperConfig = userAttrMapper.getConfig();
            userAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, ATTRIBUTE_TO_MAP_USER_INFO);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, ATTRIBUTE_TO_MAP_USER_INFO);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");

            for (ClientRepresentation client: clientsRepList) {
                client.setProtocolMappers(Arrays.asList(userAttrMapper));
            }

            return clientsRepList;

        }

        @Override
        protected void applyDefaultConfiguration(final Map<String, String> config, IdentityProviderSyncMode syncMode) {
            super.applyDefaultConfiguration(config, syncMode);
            config.put("disableUserInfo", "false");
        }
    }
}
