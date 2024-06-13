package org.keycloak.it.storage.database.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;

@DistributionTest
@WithDatabase(alias = "mssql")
public class TransactionConfigurationDistTest {

    @Test
    @Launch({ "start-dev", "--db=mssql", "--transaction-xa-enabled=false" })
    void testXADisabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        cliResult.assertNoMessage("ARJUNA016061: TransactionImple.enlistResource");
    }

}
