package org.keycloak.testsuite.broker;

import static org.keycloak.testsuite.broker.BrokerTestConstants.CLIENT_SECRET;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.util.KeyUtils;

public class KcOidcBrokerClientSecretJwtTest extends KcOidcBrokerTest {

	@Override
    protected BrokerConfiguration getBrokerConfiguration() {
		return new KcOidcBrokerConfigurationWithJWTAuthentication();
    }

    private class KcOidcBrokerConfigurationWithJWTAuthentication extends KcOidcBrokerConfiguration {

    	@Override
    	public List<ClientRepresentation> createProviderClients(SuiteContext suiteContext) {
    		List<ClientRepresentation> clientsRepList = super.createProviderClients(suiteContext);
    		log.info("Update provider clients to accept JWT authentication");
    		KeyMetadataRepresentation keyRep = KeyUtils.getActiveKey(adminClient.realm(consumerRealmName()).keys().getKeyMetadata(), Algorithm.RS256);
    		for (ClientRepresentation client: clientsRepList) {
    			client.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
    			client.setSecret(CLIENT_SECRET);
    		}
    		return clientsRepList;
    	}

    	@Override
        public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);
            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(suiteContext, config);
            config.put("jwtAuthentication", "true");
            return idp;
        }
        

    }
}
