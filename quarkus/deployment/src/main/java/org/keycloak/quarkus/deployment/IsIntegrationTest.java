package org.keycloak.quarkus.deployment;

import io.quarkus.deployment.IsTest;
import io.quarkus.runtime.LaunchMode;

import org.keycloak.quarkus.runtime.Environment;

public class IsIntegrationTest extends IsTest {

    public IsIntegrationTest(LaunchMode launchMode) {
        super(launchMode);
    }

    @Override
    public boolean getAsBoolean() {
        return super.getAsBoolean() && Environment.isTestLaunchMode();
    }

}
