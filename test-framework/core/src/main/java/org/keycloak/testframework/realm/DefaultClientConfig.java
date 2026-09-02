package org.keycloak.testframework.realm;

public class DefaultClientConfig implements ClientConfig {

    @Override
    public ClientBuilder configure(ClientBuilder client) {
        return client;
    }

}
