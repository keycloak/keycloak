package org.keycloak.test.framework.server;

public interface KeycloakTestServer {

    void start(KeycloakTestServerSmallryeConfig smallryeConfig);

    void stop();

    String getBaseUrl();

}
