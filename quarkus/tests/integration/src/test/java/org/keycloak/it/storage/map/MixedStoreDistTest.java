package org.keycloak.it.storage.map;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.it.utils.RawDistRootPath;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@WithDatabase(alias = "postgres", buildOptions = {"storage=jpa", "storage-area-realm=chm"})
public class MixedStoreDistTest {

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false" })
    void testStartUsingMixedStorage(LaunchResult result, RawDistRootPath distRoot) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
        File chmRealmsFile = Paths.get(distRoot.getDistRootPath().toString(), "data","chm", "map-realms.json").toFile();
        assertTrue(chmRealmsFile.isFile(), "File for realms does not exist!");
    }
}