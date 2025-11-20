package org.keycloak.it.cli.dist;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.ShowConfig;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@DistributionTest
public class ShowConfigCommandDistTest {

    @DryRun
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testShowConfigPicksUpRightConfigDependingOnCurrentMode(KeycloakDistribution distribution) {
        CLIResult initialResult = distribution.run("show-config");
        initialResult.assertMessage("Current Mode: production");
        initialResult.assertNoMessage("kc.db =  dev-file");

        distribution.run("start-dev");

        CLIResult devModeResult = distribution.run("show-config");
        devModeResult.assertMessage("Current Mode: development");
        devModeResult.assertMessage("kc.db =  dev-file");

        distribution.run("build", "--db=dev-file");

        CLIResult resetResult = distribution.run("show-config");
        resetResult.assertMessage("Current Mode: production");
        resetResult.assertMessage("kc.db =  dev-file");
    }

    @Test
    @Launch({ ShowConfig.NAME })
    void testShowConfigCommandShowsRuntimeConfig(LaunchResult result) {
        Assertions.assertTrue(result.getOutput()
                .contains("Current Configuration"));
    }

    @Test
    @Launch({ ShowConfig.NAME, "all" })
    void testShowConfigCommandWithAllShowsAllProfiles(LaunchResult result) {
        Assertions.assertTrue(result.getOutput()
                .contains("Current Configuration"));
        Assertions.assertTrue(result.getOutput()
                .contains("Quarkus Configuration"));
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testShowConfigCommandHidesCredentialsInProfiles(KeycloakDistribution distribution) {
        CLIResult result = distribution.run(String.format("%s=%s", CONFIG_FILE_LONG_NAME, Paths.get("src/test/resources/ShowConfigCommandTest/keycloak.conf").toAbsolutePath().normalize()), ShowConfig.NAME, "all");
        String output = result.getOutput();
        Assertions.assertFalse(output.contains("testpw1"));
        Assertions.assertFalse(output.contains("testpw2"));
        Assertions.assertFalse(output.contains("testpw3"));
        Assertions.assertTrue(output.contains("kc.db-password =  " + PropertyMappers.VALUE_MASK));
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testSmallRyeKeyStoreConfigSource(KeycloakDistribution distribution) {
        // keystore is shared with QuarkusPropertiesDistTest#testSmallRyeKeyStoreConfigSource
        CLIResult result = distribution.run(String.format("%s=%s", CONFIG_FILE_LONG_NAME, Paths.get("src/test/resources/ShowConfigCommandTest/keycloak-keystore.conf").toAbsolutePath().normalize()), ShowConfig.NAME, "all");
        String output = result.getOutput();
        assertThat(output, containsString("kc.config-keystore-password =  " + PropertyMappers.VALUE_MASK));
        assertThat(output, containsString("kc.log-level =  " + PropertyMappers.VALUE_MASK));

        assertThat(output, not(containsString("secret")));
        assertThat(output, not(containsString("debug")));
    }

    @Test
    @Launch({ ShowConfig.NAME })
    @WithEnvVars({"KC_DB_PASSWORD", "secret-pass", "KC_LOG_LEVEL_FOO_BAR", "trace"})
    void testNoDuplicitEnvVarEntries(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, containsString("kc.db-password =  " + PropertyMappers.VALUE_MASK));
        assertThat(output, containsString("kc.log-level-foo.bar"));
        assertThat(output, not(containsString("kc.log.")));
        assertThat(output, not(containsString("kc.db.password")));
        assertThat(output, not(containsString("secret-pass")));
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testConfigSourceNames(KeycloakDistribution distribution) {
        CLIResult result = distribution.run("build");
        result.assertBuild();

        distribution.setEnvVar("KC_LOG", "file");
        distribution.copyOrReplaceFile(Paths.get("src/test/resources/ShowConfigCommandTest/quarkus.properties"), Path.of("conf", "quarkus.properties"));

        result = distribution.run(String.format("%s=%s", CONFIG_FILE_LONG_NAME, Paths.get("src/test/resources/ShowConfigCommandTest/keycloak-keystore.conf").toAbsolutePath().normalize()), ShowConfig.NAME, "all");

        result.assertMessage("(CLI)");
        result.assertMessage("(ENV)");
        result.assertMessage("(quarkus.properties)");
        result.assertMessage("(Persisted)");
        result.assertMessage("(config-keystore)");
        result.assertMessage("(classpath application.properties)");
        result.assertMessage("(keycloak-keystore.conf)");
    }
}
