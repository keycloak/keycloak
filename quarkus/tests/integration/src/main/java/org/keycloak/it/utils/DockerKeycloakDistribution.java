package org.keycloak.it.utils;

import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.ResourceReaper;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class DockerKeycloakDistribution implements KeycloakDistribution {

    private static final Logger LOGGER = Logger.getLogger(DockerKeycloakDistribution.class);

    private boolean debug;
    private boolean manualStop;
    private int exitCode = -1;

    private String stdout = "";
    private String stderr = "";
    private ToStringConsumer backupConsumer = new ToStringConsumer();

    private File distributionFile = new File("../../dist/target/keycloak-" + Version.VERSION_KEYCLOAK + ".tar.gz");
    private File dockerFile = new File("../../container/Dockerfile");

    private GenericContainer<?> keycloakContainer = null;
    private String containerId = null;

    private Executor parallelReaperExecutor = Executors.newSingleThreadExecutor();

    public DockerKeycloakDistribution(boolean debug, boolean manualStop, boolean reCreate) {
        this.debug = debug;
        this.manualStop = manualStop;
    }

    private GenericContainer getKeycloakContainer() {
        if (!distributionFile.exists()) {
            throw new RuntimeException("Distribution archive " + distributionFile.getAbsolutePath() +" doesn't exists");
        }
        return new GenericContainer(
                new ImageFromDockerfile("keycloak-under-test", false)
                        .withFileFromFile("keycloak.tar.gz", distributionFile)
                        .withFileFromFile("Dockerfile", dockerFile)
                        .withBuildArg("KEYCLOAK_DIST", "keycloak.tar.gz")
        )
                .withExposedPorts(8080)
                .withStartupAttempts(1)
                .withStartupTimeout(Duration.ofSeconds(120))
                .waitingFor(Wait.forListeningPort());
    }

    @Override
    public void start(List<String> arguments) {
        stop();
        try {
            this.exitCode = -1;
            this.stdout = "";
            this.stderr = "";
            this.containerId = null;
            this.backupConsumer = new ToStringConsumer();

            keycloakContainer = getKeycloakContainer();

            keycloakContainer
                    .withLogConsumer(backupConsumer)
                    .withCommand(arguments.toArray(new String[0]))
                    .start();
            containerId = keycloakContainer.getContainerId();

            waitForStableOutput();

            // TODO: this is based on a lot of assumptions
            io.restassured.RestAssured.port = keycloakContainer.getMappedPort(8080);
        } catch (Exception cause) {
            this.exitCode = -1;
            this.stdout = backupConsumer.toUtf8String();
            this.stderr = backupConsumer.toUtf8String();
            cleanupContainer();
            keycloakContainer = null;
            LOGGER.warn("Failed to start Keycloak container", cause);
        }
    }

    // After the web server is responding we are still producing some logs that got checked in the tests
    private void waitForStableOutput() {
        int retry = 10;
        String lastLine = "";
        boolean stableOutput = false;
        while (!stableOutput) {
            if (keycloakContainer.isRunning()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                String[] splitted = keycloakContainer.getLogs().split(System.lineSeparator());
                String newLastLine = splitted[splitted.length - 1];

                retry -= 1;
                stableOutput = lastLine.equals(newLastLine) | (retry <= 0);
                lastLine = newLastLine;
            } else {
                stableOutput = true;
            }
        }
    }

    @Override
    public void stop() {
        try {
            if (keycloakContainer != null) {
                containerId = keycloakContainer.getContainerId();
                this.stdout = fetchOutputStream();
                this.stderr = fetchErrorStream();

                keycloakContainer.stop();
                this.exitCode = 0;
            }
        } catch (Exception cause) {
            this.exitCode = -1;
            throw new RuntimeException("Failed to stop the server", cause);
        } finally {
            cleanupContainer();
            keycloakContainer = null;
        }
    }

    private void cleanupContainer() {
        if (containerId != null) {
            try {
                final String finalContainerId = containerId;
                Runnable reaper = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ResourceReaper
                                    .instance()
                                    .stopAndRemoveContainer(finalContainerId);
                        } catch (Exception cause) {
                            throw new RuntimeException("Failed to stop and remove container", cause);
                        }
                    }
                };
                parallelReaperExecutor.execute(reaper);
            } catch (Exception cause) {
                throw new RuntimeException("Failed to schecdule the removal of the container", cause);
            }
        }
    }

    private String fetchOutputStream() {
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            return keycloakContainer.getLogs(OutputFrame.OutputType.STDOUT);
        } else if (this.stdout.isEmpty()) {
            return backupConsumer.toUtf8String();
        } else {
            return this.stdout;
        }
    }

    @Override
    public List<String> getOutputStream() {
        return List.of(fetchOutputStream().split("\n"));
    }

    public String fetchErrorStream() {
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            return keycloakContainer.getLogs(OutputFrame.OutputType.STDERR);
        } else if (this.stderr.isEmpty()) {
            return backupConsumer.toUtf8String();
        } else {
            return this.stderr;
        }
    }

    @Override
    public List<String> getErrorStream() {
        return List.of(fetchErrorStream().split("\n"));
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }

    @Override
    public boolean isDebug() {
        return this.debug;
    }

    @Override
    public boolean isManualStop() {
        return this.manualStop;
    }

}
