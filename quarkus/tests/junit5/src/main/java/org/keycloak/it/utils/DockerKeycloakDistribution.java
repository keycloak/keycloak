package org.keycloak.it.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.keycloak.common.Version;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LazyFuture;
import org.testcontainers.utility.MountableFile;

public final class DockerKeycloakDistribution implements KeycloakDistribution {

    private static class BackupConsumer implements Consumer<OutputFrame> {

        final ToStringConsumer stdOut = new ToStringConsumer();
        final ToStringConsumer stdErr = new ToStringConsumer();
        final Consumer<OutputFrame> customLogConsumer;
        public BackupConsumer(Consumer<OutputFrame> customLogConsumer) {
            this.customLogConsumer = customLogConsumer;
        }

        @Override
        public void accept(OutputFrame t) {
            if (customLogConsumer != null) {
                customLogConsumer.accept(t);
            }
            if (t.getType() == OutputType.STDERR) {
                stdErr.accept(t);
            } else if (t.getType() == OutputType.STDOUT) {
                stdOut.accept(t);
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(DockerKeycloakDistribution.class);

    public static final int STARTUP_TIMEOUT_SECONDS = 120;

    private final Integer[] exposedPorts;

    private int exitCode = -1;

    private String stdout = "";
    private String stderr = "";
    private BackupConsumer backupConsumer;
    private Consumer<OutputFrame> customLogConsumer;
    private GenericContainer<?> keycloakContainer = null;
    private String containerId = null;

    private final Executor parallelReaperExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, String> envVars = new HashMap<>();
    private final LazyFuture<String> image;

    private final Map<MountableFile, String> copyToContainer = new HashMap<>();

    public DockerKeycloakDistribution(int[] exposedPorts) {
        this(exposedPorts, null);
    }

    public DockerKeycloakDistribution(int[] exposedPorts, LazyFuture<String> image) {
        this.exposedPorts = IntStream.of(exposedPorts).boxed().toArray(Integer[]::new);
        this.image = image == null ? createImage(false) : image;
    }
    
    @Override
    public boolean supportsDebug() {
        return false;
    }

    @Override
    public void setEnvVar(String name, String value) {
        this.envVars.put(name, value);
    }

    public void setCustomLogConsumer(Consumer<OutputFrame> customLogConsumer) {
        this.customLogConsumer = customLogConsumer;
    }

    private GenericContainer<?> getKeycloakContainer() {
        return new GenericContainer<>(image)
                .withEnv(envVars)
                .withExposedPorts(exposedPorts)
                .withStartupAttempts(1)
                .waitingFor(new WaitAllStrategy()
                        .withStrategy(Wait.forLogMessage(".*Bootstrap completed.*", 1))
                        .withStrategy(Wait.forListeningPorts(8080))
                        .withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT_SECONDS))
                );
    }

    public static LazyFuture<String> createImage(boolean failIfDockerFileMissing) {
        Path quarkusModule = Maven.getKeycloakQuarkusModulePath();
        var distributionFile = quarkusModule.resolve(Path.of("dist", "target", "keycloak-" + Version.VERSION + ".tar.gz"))
                .toFile();

//        In current Dockerfile we support only tar.gz keycloak distribution, this module, however. does not have this
//        dependency. Adding the dependency breaks our CI as tar.gz files are not part of CI build archive.
//        Adding tar.gz files to archive would double the size of each build archive.
//        Therefore, for now, we support only building the image from the target folder of this module.
//        if (!distributionFile.exists()) {
//            distributionFile = Maven.resolveArtifact("org.keycloak", "keycloak-quarkus-dist").toFile();
//        }

        if (!distributionFile.exists()) {
            throw new RuntimeException("Distribution archive " + distributionFile.getAbsolutePath() +" doesn't exist");
        }
        LOGGER.infof("Building a new docker image from distribution: %s", distributionFile.getAbsoluteFile());

        var dockerFile = quarkusModule.resolve(Path.of("container", "Dockerfile"))
                .toFile();
        var ubiNullScript = quarkusModule.resolve(Path.of("container", "ubi-null.sh"))
                .toFile();

        if (dockerFile.exists()) {
            return new ImageFromDockerfile("keycloak-under-test", false)
                    .withFileFromFile("keycloak.tar.gz", distributionFile)
                    .withFileFromFile("ubi-null.sh", ubiNullScript)
                    .withFileFromFile("Dockerfile", dockerFile)
                    .withBuildArg("KEYCLOAK_DIST", "keycloak.tar.gz");
        } else {
            if (failIfDockerFileMissing) {
                throw new RuntimeException("Docker file %s not found".formatted(dockerFile.getAbsolutePath()));
            }
            return new RemoteDockerImage(DockerImageName.parse("quay.io/keycloak/keycloak"));
        }
    }

    @Override
    public void runKc(List<String> arguments) {
        if (keycloakContainer != null) {
            throw new IllegalStateException("Stop has not been called");
        }
        try {
            this.exitCode = -1;
            this.stdout = "";
            this.stderr = "";
            this.containerId = null;
            this.backupConsumer = new BackupConsumer(customLogConsumer);

            keycloakContainer = getKeycloakContainer();

            copyToContainer.forEach(keycloakContainer::withCopyFileToContainer);

            keycloakContainer
                    .withLogConsumer(backupConsumer)
                    .withCommand(arguments.toArray(new String[0]))
                    .start();
            containerId = keycloakContainer.getContainerId();
        } catch (Exception cause) {
            this.exitCode = -1;
            this.stdout = backupConsumer.stdOut.toUtf8String();
            this.stderr = backupConsumer.stdErr.toUtf8String();
            try {
                cleanupContainer();
            } catch (Exception stopException) {
                cause.addSuppressed(stopException);
            }
            keycloakContainer = null;
            throw new RuntimeException("Failed to start the server", cause);
        }
    }

    public void copyProvider(String groupId, String artifactId) {
        Path providerPath = Maven.resolveArtifact(groupId, artifactId);
        if (!Files.isRegularFile(providerPath)) {
            throw new RuntimeException("Failed to copy JAR file to 'providers' directory; " + providerPath + " is not a file");
        }

        copyToContainer.put(MountableFile.forHostPath(providerPath), "/opt/keycloak/providers/" + providerPath.getFileName());
    }

    public void copyConfigFile(Path configFilePath) {
        copyToContainer.put(MountableFile.forHostPath(configFilePath), "/opt/keycloak/conf/" + configFilePath.getFileName());
    }
    
    // After the web server is responding we are still producing some logs that got checked in the tests
    @Override
    public void waitFor(boolean ready, long timeoutMillis) {
        // TODO: doesn't differentiate ready, nor implements the timeout
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
                stableOutput = lastLine.equals(newLastLine) || (retry <= 0);
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
                            if (containerId == null) {
                                return;
                            }
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
                throw new RuntimeException("Failed to schedule the removal of the container", cause);
            }
        }
    }

    private String fetchOutputStream() {
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            return keycloakContainer.getLogs(OutputFrame.OutputType.STDOUT);
        } else if (this.stdout.isEmpty()) {
            return backupConsumer.stdOut.toUtf8String();
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
            return backupConsumer.stdErr.toUtf8String();
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
    public void clearEnv() {
        this.envVars.clear();
    }

    @Override
    public int getMappedPort(int port) {
        if (keycloakContainer == null || !keycloakContainer.isRunning()) {
            throw new IllegalStateException("KeycloakContainer is not running.");
        }

        return keycloakContainer.getMappedPort(port);
    }

}
