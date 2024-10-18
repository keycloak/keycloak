package org.keycloak.test.framework.server;

import org.keycloak.it.TestProvider;

import java.util.List;

public interface KeycloakTestServer {

    void start(List<String> rawOptions, List<? extends TestProvider> customProviders);

    void stop();

    String getBaseUrl();

}
