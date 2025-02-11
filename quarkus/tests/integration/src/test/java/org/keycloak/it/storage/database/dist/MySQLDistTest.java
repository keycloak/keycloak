package org.keycloak.it.storage.database.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.it.storage.database.MySQLTest;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand;

@DistributionTest(removeBuildOptionsAfterBuild = true)
@WithDatabase(alias = "mysql")
public class MySQLDistTest extends MySQLTest {

    @Test
    @Launch({"start", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG, "--spi-connections-jpa-quarkus-migration-strategy=manual", "--spi-connections-jpa-quarkus-initialize-empty=false", "--http-enabled=true", "--hostname-strict=false",})
    public void testKeycloakDbUpdateScript(LaunchResult result, RawDistRootPath rawDistRootPath) {
        assertManualDbInitialization((CLIResult) result, rawDistRootPath);
    }
}
