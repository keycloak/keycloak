package org.keycloak.test.framework.oauth;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.realm.ClientConfig;

public class DefaultOAuthClientConfiguration implements ClientConfig {

    @Override
    public ClientRepresentation getRepresentation() {
        return builder()
                .clientId("test-oauth-client")
                .serviceAccount()
                .redirectUris("http://127.0.0.1/callback/oauth")
                .secret("test-secret")
                .build();
    }

}
