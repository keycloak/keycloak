package org.keycloak.it.cli.dist;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.it.TestProvider;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs test with distribution placed in a directory that has special characters in name.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/45971">Issue #45971</a> for motivation of this test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractPathDistTest {

    /**
     * Switches temporary directory used by the {@link RawKeycloakDistribution}.
     */
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private String originalTmpDir = null;

    protected abstract String getSubPath();

    @BeforeAll
    void changeTmpDir() {
        originalTmpDir = System.getProperty(JAVA_IO_TMPDIR);
        System.setProperty(JAVA_IO_TMPDIR, originalTmpDir + File.separator + getSubPath());
    }

    @AfterAll
    void switchBackToOriginalTmpDir() {
        if (originalTmpDir != null) {
            System.setProperty(JAVA_IO_TMPDIR, originalTmpDir);
            originalTmpDir = null;
        }
    }

    @BeforeStartDistribution(AddCustomScriptsProvider.class)
    @RawDistOnly(reason = "Testing installation path handling")
    @Test
    @Launch(StartDev.NAME)
    void testApplicationBuildAndStart(CLIResult result, KeycloakDistribution dist) {
        assertDistPath(dist);
        result.assertStartedDevMode();
        assertThat(result.getOutput(), containsString("Updating the configuration and installing your custom providers, if any. Please wait."));
    }

    private void assertDistPath(KeycloakDistribution dist) {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        String distPath = rawDist.getDistPath().toString();
        assertTrue(distPath.contains(getSubPath()), "Test must run with '" + getSubPath() + "' in path. Actual path: " + distPath);
    }

    public static final class AddCustomScriptsProvider implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            RawKeycloakDistribution rawDist = distribution.unwrap(RawKeycloakDistribution.class);
            rawDist.copyProvider(new ScriptProviderForTest());
        }
    }

    /**
     * Triggers provider discovery in KeycloakProcessor.
     */
    private static final class ScriptProviderForTest implements TestProvider {
        @Override
        public String getName() {
            return "test-script-provider";
        }

        @Override
        public Class[] getClasses() {
            return new Class[0];
        }

        @Override
        public Map<String, String> getManifestResources() {
            return Map.of("keycloak-scripts.json", "keycloak-scripts.json");
        }
    }
}
