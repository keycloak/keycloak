package org.keycloak.tests.broker;

import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;

/**
 * Configures the injected consumer {@code OAuthClient} as the migrated {@code broker-app} client
 * (confidential, direct-access-grants enabled) that the legacy broker suite used to drive the login flows,
 * instead of the framework default {@code test-app}. The redirect URI is supplied by the OAuthClient
 * supplier, so it is not set here.
 */
public class BrokerAppClientConfig implements ClientConfig {

    @Override
    public ClientBuilder configure(ClientBuilder client) {
        return client
                .clientId(AbstractBrokerTest.CONSUMER_BROKER_APP_CLIENT_ID)
                .secret(AbstractBrokerTest.CONSUMER_BROKER_APP_SECRET)
                .directAccessGrantsEnabled();
    }
}
