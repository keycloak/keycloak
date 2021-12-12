package org.keycloak.it.utils;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public final class DockerKeycloakDistribution implements KeycloakDistribution {

    private static final Logger LOGGER = Logger.getLogger(DockerKeycloakDistribution.class);

    private boolean debug;
    private boolean manualStop;
    private int exitCode = -1;

    private String stdout = "";
    private String stderr = "";
    private ToStringConsumer backupConsumer = new ToStringConsumer();

    private File distributionFile = new File("../../../distribution/server-x-dist/target/keycloak.x-" + Version.VERSION_KEYCLOAK + ".tar.gz");
    private File cachedDockerfile = createDockerCacheFile();
    private boolean dockerfileFetched = false;

    private GenericContainer keycloakContainer = null;

    private File createDockerCacheFile() {
        try {
            File tmp = File.createTempFile("Dockerfile", "keycloak.x");
            tmp.deleteOnExit();
            return tmp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fetchDockerfile() {
        if (!dockerfileFetched) {
            try {
                FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/keycloak/keycloak-containers/main/server-x/Dockerfile"), cachedDockerfile);
                dockerfileFetched = true;
            } catch (Exception cause) {
                throw new RuntimeException("Cannot download upstream Dockerfile", cause);
            }
        }
    }

    private GenericContainer getKeycloakContainer() {
        if (!distributionFile.exists()) {
            throw new RuntimeException("Distribution archive " + distributionFile.getAbsolutePath() +" doesn't exists");
        }
        fetchDockerfile();
        return new GenericContainer(
                new ImageFromDockerfile()
                        .withFileFromFile("keycloakx.tar.gz", distributionFile)
                        .withFileFromFile("Dockerfile", cachedDockerfile)
                        .withBuildArg("KEYCLOAK_DIST", "keycloakx.tar.gz")
        )
                .withExposedPorts(8080);
    }

    public <T> DockerKeycloakDistribution(boolean debug, boolean manualStop, boolean reCreate) {
        this.debug = debug;
        this.manualStop = manualStop;
    }

    @Override
    public void start(List<String> arguments) {
        try {
            this.exitCode = -1;
            this.stdout = "";
            this.stderr = "";
            this.backupConsumer = new ToStringConsumer();


            keycloakContainer = getKeycloakContainer();

            keycloakContainer
                    .withLogConsumer(backupConsumer)
                    .withCommand(arguments.toArray(new String[0]))
                    .start();

            // TODO: this is based on a lot of assumptions
            io.restassured.RestAssured.port = keycloakContainer.getMappedPort(8080);
        } catch (Exception cause) {
            this.exitCode = -1;
            this.stdout = backupConsumer.toUtf8String();
            this.stderr = backupConsumer.toUtf8String();
            keycloakContainer = null;
            LOGGER.warn("Failed to start Keycloak container", cause);
        }
    }

    @Override
    public void stop() {
        try {
            if (keycloakContainer != null) {
                this.stdout = fetchOutputStream();
                this.stderr = fetchErrorStream();

                keycloakContainer.stop();
                this.exitCode = 0;
            }
        } catch (Exception cause) {
            this.exitCode = -1;
            throw new RuntimeException("Failed to stop the server", cause);
        } finally {
            keycloakContainer = null;
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
