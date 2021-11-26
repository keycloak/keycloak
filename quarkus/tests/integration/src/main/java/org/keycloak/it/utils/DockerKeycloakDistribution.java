package org.keycloak.it.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;

public final class DockerKeycloakDistribution implements KeycloakDistribution {

    private static final Logger LOGGER = Logger.getLogger(DockerKeycloakDistribution.class);

    private boolean debug;
    private boolean manualStop;
    private int exitCode = -1;

    private List<String> stdout = List.of();
    private List<String> stderr = List.of();
    private ToStringConsumer backupConsumer = new ToStringConsumer();

    private File distributionFile = new File("../../../distribution/server-x-dist/target/keycloak.x-" + Version.VERSION_KEYCLOAK + ".tar.gz");

    private GenericContainer keycloakContainer = null;

    private GenericContainer runKeycloakContainer() {
        if (!distributionFile.exists()) {
            throw new RuntimeException("Distribution archive " + distributionFile.getAbsolutePath() +" doesn't exists");
        }
        File dockerFile = null;
        try {
            dockerFile = File.createTempFile("keycloakx", "Dockerfile");
            FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/keycloak/keycloak-containers/main/server-x/Dockerfile"), dockerFile);
        } catch (Exception cause) {
            throw new RuntimeException("Cannot download upstream Dockerfile", cause);
        }
        return new GenericContainer(
                new ImageFromDockerfile()
                        .withFileFromFile("keycloakx.tar.gz", distributionFile)
                        .withFileFromFile("Dockerfile", dockerFile)
                        .withBuildArg("KEYCLOAK_DIST", "keycloakx.tar.gz")
        )
                .withExposedPorts(8080)
                .withStartupTimeout(Duration.ofSeconds(40))
                .withStartupAttempts(1)
                .waitingFor(Wait.forHttp("/").forStatusCode(200).withReadTimeout(Duration.ofSeconds(2)));
    }

    public <T> DockerKeycloakDistribution(boolean debug, boolean manualStop, boolean reCreate) {
        this.debug = debug;
        this.manualStop = manualStop;
    }

    @Override
    public void start(List<String> arguments) {
        try {
            this.exitCode = -1;
            this.stdout = List.of();
            this.stderr = List.of();
            this.backupConsumer = new ToStringConsumer();

            keycloakContainer = runKeycloakContainer();

            keycloakContainer
                    .withLogConsumer(backupConsumer)
                    .withCommand(arguments.toArray(new String[0]))
                    .start();

            // TODO: this is based on a lot of assumptions
            io.restassured.RestAssured.port = keycloakContainer.getMappedPort(8080);
        } catch (Exception cause) {
            this.exitCode = -1;
            this.stdout = List.of(backupConsumer.toUtf8String());
            this.stderr = List.of(backupConsumer.toUtf8String());
            keycloakContainer = null;
            LOGGER.warn("Failed to start Keycloak container", cause);
        }
    }

    @Override
    public void stop() {
        try {
            if (keycloakContainer != null) {
                this.stdout = getOutputStream();
                this.stderr = getErrorStream();

                keycloakContainer.stop();
                keycloakContainer = null;
                this.exitCode = 0;
            }
        } catch (Exception cause) {
            this.exitCode = -1;
            throw new RuntimeException("Failed to stop the server", cause);
        }
    }

    @Override
    public List<String> getOutputStream() {
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            return List.of(keycloakContainer.getLogs(OutputFrame.OutputType.STDOUT));
        } else if (this.stdout.isEmpty()) {
            return List.of(backupConsumer.toUtf8String());
        } else {
            return this.stdout;
        }
    }

    @Override
    public List<String> getErrorStream() {
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            return List.of(keycloakContainer.getLogs(OutputFrame.OutputType.STDERR));
        } else if (this.stderr.isEmpty()) {
            return List.of(backupConsumer.toUtf8String());
        } else {
            return this.stderr;
        }
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }

    @Override
    public boolean getDebug() {
        return this.debug;
    }

    @Override
    public boolean getManualStop() {
        return this.manualStop;
    }
}
