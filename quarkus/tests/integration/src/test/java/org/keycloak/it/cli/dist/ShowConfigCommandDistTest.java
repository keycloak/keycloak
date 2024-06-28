package org.keycloak.it.cli.dist;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

@DistributionTest
public class ShowConfigCommandDistTest {

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testShowConfigPicksUpRightConfigDependingOnCurrentMode(KeycloakDistribution distribution) {
        CLIResult initialResult = distribution.run("show-config");
        initialResult.assertMessage("Current Mode: production");
        initialResult.assertMessage("kc.db =  dev-file");

        distribution.run("start-dev");

        CLIResult devModeResult = distribution.run("show-config");
        devModeResult.assertMessage("Current Mode: development");
        devModeResult.assertMessage("kc.db =  dev-file");

        distribution.run("build");

        CLIResult resetResult = distribution.run("show-config");
        resetResult.assertMessage("Current Mode: production");
        resetResult.assertMessage("kc.db =  dev-file");
    }
}
