package org.keycloak.it.storage.database.dist;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.STORAGE)
public class DatasourcesDistTest {

    @Test
    @Launch({"start-dev", "--db-kind-users=postgres", "--db-kind-clients=postgres", "--transaction-xa-enabled-users=false"})
    public void multipleNonXaDatasources(CLIResult result) {
        result.assertNoMessage("Multiple datasources are specified:"); // log handlers are not initialized yet, so only the error should be visible
        result.assertExitCode(CommandLine.ExitCode.SOFTWARE);
        result.assertError("Multiple datasources are configured but more than 1 (<default>, users) is using non-XA transactions.");
    }

    @Test
    @Launch({"build", "--db-kind-users=postgres", "--db-kind-clients=mariadb"})
    public void multipleDatasourcesPrint(CLIResult result) {
        result.assertMessage("Multiple datasources are specified: clients, <default>, users");
        result.assertBuild();
    }

    @Test
    @WithEnvVars({"KC_DB_KIND_USERS", "postgres", "KC_DB_KIND_MY_AWESOME_CLIENTS", "mariadb"})
    @Launch({"build"})
    public void specifiedViaEnvVars(CLIResult result) {
        result.assertMessage("Multiple datasources are specified: <default>, my-awesome-clients, users");
        result.assertBuild();
    }

}
