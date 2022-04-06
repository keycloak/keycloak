package org.keycloak.it.cli;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLITest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

@CLITest
public class HostnameTest {

    @Test
    @Launch({ CONFIG_FILE_LONG_NAME+"=src/test/resources/HostnameTest/keycloak.conf", "start" })
    void failInvalidHostnameMsgFromConfFile(LaunchResult result) {
        assertTrue(result.getOutput().contains("Invalid hostname value. The entered value is not complying to the standardized hostname syntax."),
                () -> "The Output:\n" + result.getOutput() + "doesn't contains the expected string. ");
    }
}
