package org.keycloak.it.cli.dist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.it.TestProvider;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Runs test with distribution placed in a directory that has special characters in name.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/45971">Issue #45971</a> for motivation of this test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractPathDistTest {

    protected abstract String getSubPath();

    @BeforeStartDistribution(AddCustomScriptsProvider.class)
    @RawDistOnly(reason = "Testing installation path handling")
    @Test
    void testApplicationBuildAndStart(KeycloakDistribution dist) throws IOException {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        Path distPath = rawDist.getDistPath();
        Path newPath = distPath.getParent().resolve(getSubPath()).resolve(distPath.getFileName());
        rawDist.setDistPath(newPath);
        try {
            CLIResult result = dist.run(StartDev.NAME);
            result.assertStartedDevMode();
            assertThat(result.getOutput(), containsString("Updating the configuration and installing your custom providers, if any. Please wait."));
        } finally {
            rawDist.setDistPath(distPath);
        }
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
