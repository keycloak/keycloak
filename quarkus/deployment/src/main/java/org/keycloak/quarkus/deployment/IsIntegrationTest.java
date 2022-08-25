package org.keycloak.quarkus.deployment;

import io.quarkus.deployment.IsTest;
import io.quarkus.runtime.LaunchMode;

import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE;

public class IsIntegrationTest extends IsTest {

    public IsIntegrationTest(LaunchMode launchMode) {
        super(launchMode);
    }

    @Override
    public boolean getAsBoolean() {
        return super.getAsBoolean() && (System.getProperty(LAUNCH_MODE) != null && System.getProperty(LAUNCH_MODE).equals("test"));
    }

}
