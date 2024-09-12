package org.keycloak.test.framework.server;

import org.keycloak.it.TestProvider;

import java.util.List;
import java.util.Set;

public interface KeycloakTestServer {

    void start(List<String> rawOptions, Set<TestProvider> customTestProviders);

    void stop();

    String getBaseUrl();

}
