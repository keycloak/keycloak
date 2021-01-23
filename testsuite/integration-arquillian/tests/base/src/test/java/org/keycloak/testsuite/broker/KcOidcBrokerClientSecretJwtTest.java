package org.keycloak.testsuite.broker;

import static org.keycloak.testsuite.broker.BrokerTestConstants.CLIENT_SECRET;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

public class KcOidcBrokerClientSecretJwtTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationWithJWTAuthentication();
    }

    private class KcOidcBrokerConfigurationWithJWTAuthentication extends KcOidcBrokerConfiguration {

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientsRepList = super.createProviderClients();
            log.info("Update provider clients to accept JWT authentication");
            for (ClientRepresentation client: clientsRepList) {
                client.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
                client.setSecret(CLIENT_SECRET);
            }
            return clientsRepList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);
            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            config.put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_JWT);
            return idp;
        }
        

    }
}
