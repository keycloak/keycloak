package org.keycloak.testframework.oauth.nimbus;

import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;

public class DefaultOAuthClientConfiguration implements ClientConfig {

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client.clientId("test-oauth-client")
                .serviceAccount()
                .directAccessGrants()
                .redirectUris("http://127.0.0.1/callback/oauth")
                .secret("test-secret");
    }

}
