package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@DistributionTest
public class FeaturesDistTest {

    @Test
    @Launch({StartDev.NAME, "--features=preview"})
    public void testPreviewFeaturesGetEnabledWhenCliArgIsSet(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        assertThat(cliResult.getOutput(), CoreMatchers.allOf(
                containsString("Preview feature enabled: admin_fine_grained_authz"),
                containsString("Preview feature enabled: openshift_integration"),
                containsString("Preview feature enabled: scripts"),
                containsString("Preview feature enabled: token_exchange"),
                containsString("Preview feature enabled: declarative_user_profile")));
    }
}
