package org.keycloak.it.cli.dist;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(DistributionTest.SMOKE)
public class FeaturesDistTest {

    private static final String PREVIEW_FEATURES_EXPECTED_LOG = "Preview features enabled: " + Arrays.stream(Profile.Feature.values())
            .filter(feature -> feature.getType() == Profile.Feature.Type.PREVIEW)
            .filter(feature -> {
                Set<Profile.Feature> versions = Profile.getFeatureVersions(feature.getUnversionedKey());
                if (versions.size() == 1) {
                    return true;
                }
                return versions.iterator().next().getVersion() == feature.getVersion();
            })
            .map(Profile.Feature::getVersionedKey)
            .sorted()
            .collect(Collectors.joining(", "));

    @Test
    public void testEnableOnBuild(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run(Build.NAME, "--db=dev-file", "--features=preview");
        cliResult.assertBuild();
        assertPreviewFeaturesEnabled(cliResult);

        cliResult = dist.run(Start.NAME, "--http-enabled=true", "--hostname-strict=false", OPTIMIZED_BUILD_OPTION_LONG);
        assertPreviewFeaturesEnabled(cliResult);
    }

    @Test
    @Launch({StartDev.NAME, "--features=preview"})
    public void testEnablePreviewFeatures(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
        assertPreviewFeaturesEnabled(cliResult);
    }

    // Should enable "docker" together with all other "preview" features
    @Test
    @Launch({StartDev.NAME, "--features=preview,docker"})
    public void testEnablePreviewFeaturesAndDocker(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
        assertPreviewFeaturesEnabled(cliResult);
    }

    @Test
    @Launch({StartDev.NAME, "--features=preview", "--features-disabled=token-exchange"})
    public void testPreviewFeatureDisabledInPreviewMode(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
        cliResult.assertNoMessage("token-exchange");
    }

    @Test
    @Launch({StartDev.NAME, "--features=token-exchange", "--features-disabled=token-exchange"})
    public void testEnableDisableConflict(CLIResult cliResult) {
        cliResult.assertError("token-exchange is in both the enabled and disabled feature lists");
    }

    @Test
    @Launch({StartDev.NAME, "--features=token-exchange:v1", "--features-disabled=token-exchange"})
    public void testEnableDisableConflictUsingVersioned(CLIResult cliResult) {
        cliResult.assertError("Versioned feature token-exchange:v1 is not expected as token-exchange is already disabled");
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({StartDev.NAME, "--features=token-exchange,admin-fine-grained-authz:v1"})
    public void testEnableMultipleFeatures(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
        cliResult.assertMessage("Preview features enabled: admin-fine-grained-authz:v1, token-exchange:v1");
        cliResult.assertNoMessage("recovery-codes");
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({StartDev.NAME, "--features=\"token-exchange,admin-fine-grained-authz:v1\""})
    public void testWinEnableMultipleFeatures(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
        cliResult.assertMessage("Preview features enabled: admin-fine-grained-authz:v1, token-exchange:v1");
        cliResult.assertNoMessage("recovery-codes");
    }

    private void assertPreviewFeaturesEnabled(CLIResult cliResult) {
        assertThat("expecting at least one preview feature on the list", PREVIEW_FEATURES_EXPECTED_LOG, containsString(":"));
        cliResult.assertMessage(PREVIEW_FEATURES_EXPECTED_LOG);
    }
}
