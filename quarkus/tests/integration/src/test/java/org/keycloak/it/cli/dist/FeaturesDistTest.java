package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FeaturesDistTest {

    @Test
    @Launch({ Build.NAME, "--features=preview", "--cache=local"})
    @Order(1)
    public void testEnableOnBuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
        assertPreviewFeaturesEnabled(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--http-enabled=true", "--hostname-strict=false"})
    @Order(2)
    public void testFeatureEnabledOnStart(LaunchResult result) {
        assertPreviewFeaturesEnabled((CLIResult) result);
    }

    @Test
    @Launch({StartDev.NAME, "--features=preview"})
    public void testEnablePreviewFeatures(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertPreviewFeaturesEnabled((CLIResult) result);
    }

    @Test
    @Launch({StartDev.NAME, "--features=preview", "--features-disabled=token-exchange"})
    public void testEnablePrecedenceOverDisable(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertPreviewFeaturesEnabled((CLIResult) result);
    }

    @Test
    @Launch({StartDev.NAME, "--features=token-exchange,admin-fine-grained-authz"})
    public void testEnableMultipleFeatures(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertThat(cliResult.getOutput(), CoreMatchers.allOf(
                containsString("Preview feature enabled: admin_fine_grained_authz"),
                containsString("Preview feature enabled: token_exchange")));
        assertFalse(cliResult.getOutput().contains("declarative-user-profile"));
    }

    private void assertPreviewFeaturesEnabled(CLIResult result) {
        assertThat(result.getOutput(), CoreMatchers.allOf(
                containsString("Preview feature enabled: admin_fine_grained_authz"),
                containsString("Preview feature enabled: openshift_integration"),
                containsString("Preview feature enabled: scripts"),
                containsString("Preview feature enabled: token_exchange"),
                containsString("Preview feature enabled: declarative_user_profile")));
    }
}
