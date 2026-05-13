package org.keycloak.it.cli.dist;

import java.io.IOException;

import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.deployment.util.FileUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SLOW)
public class LiquibaseDistTest {

    @Test
    public void dbLockMultipleExecution(KeycloakDistribution distribution) throws IOException {
        // force a full db initialization
        RawKeycloakDistribution rawDist = distribution.unwrap(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());
        var result = distribution.run("start-dev", "--log-level=org.keycloak.connections.jpa.updater.liquibase.lock.CustomLockService:trace");
        result.assertMessage("Initialize Database Lock Table, current locks []");
        result.assertMessage("Initialized record in the database lock table");

        // the code block in the CustomLockService should not be executed for the second time
        result = distribution.run("start-dev", "--log-level=org.keycloak.connections.jpa.updater.liquibase.lock.CustomLockService:trace");
        result.assertNoMessage("Initialize Database Lock Table, current locks");
        result.assertNoMessage("Initialized record in the database lock table");
    }
}
