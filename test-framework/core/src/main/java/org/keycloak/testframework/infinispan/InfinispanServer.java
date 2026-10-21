package org.keycloak.testframework.infinispan;

import java.util.Map;

public interface InfinispanServer {

    void start();

    void stop();

    Map<String, String> serverConfig();
}
