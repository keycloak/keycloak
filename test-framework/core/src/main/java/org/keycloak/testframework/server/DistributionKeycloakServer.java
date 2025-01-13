package org.keycloak.testframework.server;

import io.quarkus.maven.dependency.Dependency;
import org.jboss.logging.Logger;
import org.keycloak.it.utils.OutputConsumer;
import org.keycloak.it.utils.RawKeycloakDistribution;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistributionKeycloakServer implements KeycloakServer {

    private static final boolean DEBUG = false;
    private static final boolean MANUAL_STOP = true;
    private static final boolean ENABLE_TLS = false;
    private static final boolean RE_CREATE = false;
    private static final boolean REMOVE_BUILD_OPTIONS_AFTER_BUILD = false;
    private static final int REQUEST_PORT = 8080;

    private RawKeycloakDistribution keycloak;

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        keycloak = new RawKeycloakDistribution(DEBUG, MANUAL_STOP, ENABLE_TLS, RE_CREATE, REMOVE_BUILD_OPTIONS_AFTER_BUILD, REQUEST_PORT, new LoggingOutputConsumer());

        for (Dependency dependency : keycloakServerConfigBuilder.toDependencies()) {
            keycloak.copyProvider(dependency.getGroupId(), dependency.getArtifactId());
        }

        keycloak.run(keycloakServerConfigBuilder.toArgs());
    }

    @Override
    public void stop() {
        keycloak.stop();
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

    @Override
    public String getManagementBaseUrl() {
        return "http://localhost:9000";
    }

    private static final class LoggingOutputConsumer implements OutputConsumer {

        private static final Pattern LOG_PATTERN = Pattern.compile("([^ ]*) ([^ ]*) ([A-Z]*)([ ]*)(.*)");
        private static final Logger LOGGER = Logger.getLogger("managed.keycloak");

        @Override
        public void onStdOut(String line) {
            onLine(line, Logger.Level.INFO);
        }

        @Override
        public void onErrOut(String line) {
            onLine(line, Logger.Level.ERROR);
        }

        @Override
        public List<String> getStdOut() {
            return Collections.emptyList();
        }

        @Override
        public List<String> getErrOut() {
            return Collections.emptyList();
        }

        private void onLine(String line, Logger.Level defaultLevel) {
            Matcher matcher = LOG_PATTERN.matcher(line);
            if (matcher.matches()) {
                String levelString = matcher.group(3);
                String message = matcher.group(5);
                if (levelString != null && message != null) {
                    for (Logger.Level l : Logger.Level.values()) {
                        if (l.name().equals(levelString)) {
                            LOGGER.log(l, message);
                            return;
                        }
                    }
                }
            }

            LOGGER.log(defaultLevel, line);
        }

        @Override
        public void reset() {
        }
    }

}
