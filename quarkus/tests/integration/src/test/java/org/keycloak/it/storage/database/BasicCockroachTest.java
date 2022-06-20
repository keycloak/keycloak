package org.keycloak.it.storage.database;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BasicCockroachTest {

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false" })
    void testSuccessful(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--db-username=wrong" })
    void testWrongUsername(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("ERROR: Failed to obtain JDBC connection");
        assertWrongUsername(cliResult);
    }

    protected abstract void assertWrongUsername(CLIResult cliResult);

    @Order(1)
    @Test
    @Launch({ "export", "--dir=./target/export"})
    public void testExportSucceeds(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Full model export requested");
        cliResult.assertMessage("Export finished successfully");
    }

    @Order(2)
    @Test
    @Launch({ "import", "--dir=./target/export" })
    void testImportSucceeds(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("target/export");
        cliResult.assertMessage("Realm 'master' imported");
        cliResult.assertMessage("Import finished successfully");
    }
}
