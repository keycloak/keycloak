package org.keycloak.it.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import io.restassured.RestAssured;
import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.keycloak.it.junit5.extension.CLIResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LazyFuture;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public final class DockerKeycloakDistribution implements KeycloakDistribution {

    private static final Logger LOGGER = Logger.getLogger(DockerKeycloakDistribution.class);

    private final boolean debug;
    private final boolean manualStop;
    private final int requestPort;
    private final Integer[] exposedPorts;

    private int exitCode = -1;

    private String stdout = "";
    private String stderr = "";
    private ToStringConsumer backupConsumer = new ToStringConsumer();
    private final File dockerScriptFile = new File("../../container/ubi-null.sh");

    private GenericContainer<?> keycloakContainer = null;
    private String containerId = null;

    private final Executor parallelReaperExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, String> envVars = new HashMap<>();

    public DockerKeycloakDistribution(boolean debug, boolean manualStop, int requestPort, int[] exposedPorts) {
        this.debug = debug;
        this.manualStop = manualStop;
        this.requestPort = requestPort;
        this.exposedPorts = IntStream.of(exposedPorts).boxed().toArray(Integer[]::new);
    }

    @Override
    public void setEnvVar(String name, String value) {
        this.envVars.put(name, value);
    }

    private GenericContainer<?> getKeycloakContainer() {
        File distributionFile = new File("../../dist/" + File.separator + "target" + File.separator + "keycloak-" + Version.VERSION + ".tar.gz");

        if (!distributionFile.exists()) {
            distributionFile = Maven.resolveArtifact("org.keycloak", "keycloak-quarkus-dist").toFile();
        }

        if (!distributionFile.exists()) {
            throw new RuntimeException("Distribution archive " + distributionFile.getAbsolutePath() +" doesn't exist");
        }

        File dockerFile = new File("../../container/Dockerfile");
        LazyFuture<String> image;

        if (dockerFile.exists()) {
            image = new ImageFromDockerfile("keycloak-under-test", false)
                    .withFileFromFile("keycloak.tar.gz", distributionFile)
                    .withFileFromFile("ubi-null.sh", dockerScriptFile)
                    .withFileFromFile("Dockerfile", dockerFile)
                    .withBuildArg("KEYCLOAK_DIST", "keycloak.tar.gz");
            toString();
        } else {
            image = new RemoteDockerImage(DockerImageName.parse("quay.io/keycloak/keycloak"));
        }

        return new GenericContainer<>(image)
                .withEnv(envVars)
                .withExposedPorts(exposedPorts)
                .withStartupAttempts(1)
                .withStartupTimeout(Duration.ofSeconds(120))
                .waitingFor(Wait.forListeningPorts(8080));
    }

    @Override
    public CLIResult run(List<String> arguments) {
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
        } catch (Exception cause) {
            this.exitCode = -1;
            this.stdout = backupConsumer.toUtf8String();
            this.stderr = backupConsumer.toUtf8String();
            cleanupContainer();
            keycloakContainer = null;
            LOGGER.warn("Failed to start Keycloak container", cause);
        } finally {
            if (!manualStop) {
                stop();
                envVars.clear();
            }
        }

        setRequestPort();

        return CLIResult.create(getOutputStream(), getErrorStream(), getExitCode());
    }

    @Override
    public void setRequestPort() {
        setRequestPort(requestPort);
    }

    @Override
    public void setRequestPort(int port) {
        if (keycloakContainer != null) {
            RestAssured.port = keycloakContainer.getMappedPort(port);
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
                Runnable reaper = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (containerId == null) return;
                            DockerClient dockerClient = DockerClientFactory.lazyClient();
                            dockerClient.killContainerCmd(containerId).exec();
                            dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).withForce(true).exec();
                        } catch (NotFoundException notFound) {
                            LOGGER.debug("Container is already cleaned up, no additional cleanup required");
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

    @Override
    public <D extends KeycloakDistribution> D unwrap(Class<D> type) {
        if (!KeycloakDistribution.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Not a " + KeycloakDistribution.class + " type");
        }

        if (type.isInstance(this)) {
            return type.cast(this);
        }

        throw new IllegalArgumentException("Not a " + type + " type");
    }

    @Override
    public void assertStopped() {
        // not implemented
    }
}
