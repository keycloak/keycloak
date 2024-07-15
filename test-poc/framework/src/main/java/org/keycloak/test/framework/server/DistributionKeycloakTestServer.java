package org.keycloak.test.framework.server;

import org.keycloak.it.utils.RawKeycloakDistribution;

import java.util.LinkedList;
import java.util.List;

public class DistributionKeycloakTestServer implements KeycloakTestServer {

    private boolean debug = false;
    private boolean manualStop = true;
    private boolean enableTls = false;
    private boolean reCreate = false;
    private boolean removeBuildOptionsAfterBuild = false;
    private int requestPort = 8080;
    private RawKeycloakDistribution keycloak;

    @Override
    public void start(KeycloakTestServerConfig serverConfig) {
        keycloak = new RawKeycloakDistribution(debug, manualStop, enableTls, reCreate, removeBuildOptionsAfterBuild, requestPort);

        // Set environment variables user and password for Keycloak Admin used by Keycloak instance.
        keycloak.setEnvVar("KC_BOOTSTRAP_ADMIN_USERNAME", serverConfig.adminUserName().get());
        keycloak.setEnvVar("KC_BOOTSTRAP_ADMIN_PASSWORD", serverConfig.adminUserPassword().get());

        List<String> rawOptions = new LinkedList<>();
        rawOptions.add("start-dev");

        //rawOptions.add("--db=dev-mem"); // TODO With dev-mem there's an issue as the H2 DB isn't stopped when restarting embedded server
        rawOptions.add("--cache=local");

        if (!serverConfig.features().isEmpty()) {
            rawOptions.add("--features=" + String.join(",", serverConfig.features()));
        }

        serverConfig.options().forEach((key, value) -> rawOptions.add("--" + key + "=" + value));

        keycloak.run(rawOptions).assertStartedDevMode();
    }

    @Override
    public void stop() {
        keycloak.stop();
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
