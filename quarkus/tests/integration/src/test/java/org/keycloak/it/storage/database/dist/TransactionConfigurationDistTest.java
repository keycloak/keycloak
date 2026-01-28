package org.keycloak.it.storage.database.dist;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest
@WithDatabase(alias = "mssql")
@Tag(DistributionTest.STORAGE)
public class TransactionConfigurationDistTest {

    @Test
    @Launch({ "start-dev", "--db=mssql", "--transaction-xa-enabled=false" })
    void testXADisabled(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
        cliResult.assertNoMessage("ARJUNA016061: TransactionImple.enlistResource");
    }

}
