package org.keycloak.testframework.oauth;

import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;

public class DefaultOAuthClientConfiguration implements ClientConfig {

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client.clientId("test-app")
                .serviceAccount()
                .directAccessGrants()
                .secret("test-secret");
    }

}
