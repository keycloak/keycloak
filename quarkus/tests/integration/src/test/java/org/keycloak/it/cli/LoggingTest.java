package org.keycloak.it.cli;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.CLITest;

import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

@CLITest
public class LoggingTest {

    @Test
    @Launch({ CONFIG_FILE_LONG_NAME+"=src/test/resources/LoggingTest/keycloak.conf", "start-dev" })
    void failUnknownHandlersInConfFile(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Invalid values in list for key: log Values: foo,console. Possible values are a combination of: console,file,gelf");
    }

    @Test
    @Launch({ CONFIG_FILE_LONG_NAME+"=src/test/resources/LoggingTest/emptylog.conf", "start-dev" })
    void failEmptyLogErrorFromConfFileError(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Value for configuration key 'log' is empty.");
    }

    @Test
    @Launch({ "start-dev","--log=foo,bar" })
    void failUnknownHandlersInCliCommand(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Invalid value for option '--log': foo,bar");
    }

    @Test
    @Launch({ "start-dev","--log=" })
    void failEmptyLogValueInCliError(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Invalid value for option '--log': .");
    }
}
