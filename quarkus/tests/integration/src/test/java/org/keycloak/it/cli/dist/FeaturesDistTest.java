package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.LegacyStore;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@LegacyStore
public class FeaturesDistTest {

    private static final String PREVIEW_FEATURES_EXPECTED_LOG = "Preview features enabled: admin-fine-grained-authz, client-secret-rotation, declarative-user-profile, openshift-integration, recovery-codes, scripts, token-exchange, update-email";

    @Test
    public void testEnableOnBuild(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run(Build.NAME, "--features=preview");
        cliResult.assertBuild();
        assertPreviewFeaturesEnabled(cliResult);

        cliResult = dist.run(Start.NAME, "--http-enabled=true", "--hostname-strict=false", OPTIMIZED_BUILD_OPTION_LONG);
        assertPreviewFeaturesEnabled(cliResult);

    }

    @Test
    @Launch({StartDev.NAME, "--features=preview"})
    public void testEnablePreviewFeatures(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertPreviewFeaturesEnabled((CLIResult) result);
    }

    // Should enable "fips" together with all other "preview" features
    @Test
    @Launch({StartDev.NAME, "--features=preview,fips"})
    public void testEnablePreviewFeaturesAndFips(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        String previewFeaturesWithFipsIncluded = PREVIEW_FEATURES_EXPECTED_LOG.replace("declarative-user-profile", "declarative-user-profile, fips");
        assertThat(result.getOutput(), CoreMatchers.allOf(
                containsString(previewFeaturesWithFipsIncluded)));
        cliResult.assertError("Failed to configure FIPS.");
    }

    @Test
    @Launch({StartDev.NAME, "--features=preview", "--features-disabled=token-exchange"})
    public void testPreviewFeatureDisabledInPreviewMode(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertFalse(cliResult.getOutput().contains("token-exchange"));
    }

    @Test
    @Launch({StartDev.NAME, "--features=token-exchange", "--features-disabled=token-exchange"})
    public void testEnablePrecedenceOverDisable(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertThat(cliResult.getOutput(), containsString("Preview features enabled: token-exchange"));
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({StartDev.NAME, "--features=token-exchange,admin-fine-grained-authz"})
    public void testEnableMultipleFeatures(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertThat(cliResult.getOutput(), CoreMatchers.allOf(
                containsString("Preview features enabled: admin-fine-grained-authz, token-exchange")));
        assertFalse(cliResult.getOutput().contains("declarative-user-profile"));
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({StartDev.NAME, "--features=\"token-exchange,admin-fine-grained-authz\""})
    public void testWinEnableMultipleFeatures(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertThat(cliResult.getOutput(), CoreMatchers.allOf(
                containsString("Preview features enabled: admin-fine-grained-authz, token-exchange")));
        assertFalse(cliResult.getOutput().contains("declarative-user-profile"));
    }

    private void assertPreviewFeaturesEnabled(CLIResult result) {
        assertThat(result.getOutput(), CoreMatchers.allOf(
                containsString(PREVIEW_FEATURES_EXPECTED_LOG)));
    }
}
