package org.keycloak.quarkus.deployment;

import org.keycloak.quarkus.runtime.Environment;

import io.quarkus.deployment.IsTest;
import io.quarkus.runtime.LaunchMode;

public class IsIntegrationTest extends IsTest {

    public IsIntegrationTest(LaunchMode launchMode) {
        super(launchMode);
    }

    @Override
    public boolean getAsBoolean() {
        return super.getAsBoolean() && Environment.isTestLaunchMode();
    }

}
