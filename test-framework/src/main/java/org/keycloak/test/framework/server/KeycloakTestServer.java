package org.keycloak.test.framework.server;

import java.util.List;

public interface KeycloakTestServer {

    void start(List<String> rawOptions);

    void stop();

    String getBaseUrl();

}
