package org.keycloak.testframework.saml;

import org.keycloak.testframework.realm.ClientConfigBuilder;

public interface SamlClientConfig {

    ClientConfigBuilder configure(ClientConfigBuilder client);

}
