package org.keycloak.testframework.saml;

import org.keycloak.testframework.realm.ClientBuilder;

public interface SamlClientConfig {

    ClientBuilder configure(ClientBuilder client);

}
