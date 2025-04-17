package org.keycloak.it.storage.database.dist;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import picocli.CommandLine;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.STORAGE)
public class DatasourcesDistTest {

    @Test
    @Launch({"start-dev", "--db-kind-users=postgres", "--db-kind-clients=postgres", "--transaction-xa-datasource-users=false"})
    public void multipleNonXaDatasources(CLIResult result) {
        result.assertExitCode(CommandLine.ExitCode.SOFTWARE);
        result.assertError("Multiple datasources are configured but more than 1 (<default>, users) is using non-XA transactions.");
    }
}
