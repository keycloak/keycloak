package org.keycloak.testsuite.broker;

import java.util.List;

import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

public class KcOidcAccessTokenOnlyClaimsUserAttributeMapperTest extends OidcUserAttributeMapperTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> clientsRepList = super.createProviderClients();
                clientsRepList.stream()
                    .flatMap(clientRepresentation -> clientRepresentation.getProtocolMappers().stream())
                    .map(ProtocolMapperRepresentation::getConfig)
                    .forEach(protocolMapperConfig -> {
                        protocolMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
                        protocolMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
                        protocolMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "false");
                    });

                return clientsRepList;
            }
        };
    }
}
