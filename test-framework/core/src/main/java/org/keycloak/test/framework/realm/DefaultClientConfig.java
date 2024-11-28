package org.keycloak.test.framework.realm;

public class DefaultClientConfig implements ClientConfig {

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client;
    }

}
