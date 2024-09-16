package org.keycloak.test.framework.server;

import java.util.List;
import java.util.Set;

public interface KeycloakTestServer {

    void start(List<String> rawOptions, Set<Class<? extends ProviderModule>> providerModules);

    void stop();

    String getBaseUrl();

}
