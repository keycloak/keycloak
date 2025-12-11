package org.keycloak.testframework.oauth;

import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;

public class DefaultOAuthClientConfiguration implements ClientConfig {

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client.clientId("test-app")
                .serviceAccountsEnabled(true)
                .directAccessGrantsEnabled(true)
                .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, "authorization-grant-idp-alias")
                .secret("test-secret");
    }

}
