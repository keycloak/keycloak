package org.keycloak.testframework.server;

import io.quarkus.maven.dependency.Dependency;
import org.keycloak.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakServer implements KeycloakServer {

    private Keycloak keycloak;
    private Path homeDir;

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        Keycloak.Builder builder = Keycloak.builder().setVersion(Version.VERSION);

        for(Dependency dependency : keycloakServerConfigBuilder.toDependencies()) {
            builder.addDependency(dependency.getGroupId(), dependency.getArtifactId(), "");
        }

        Set<Path> configFiles = keycloakServerConfigBuilder.toConfigFiles();
        if (!configFiles.isEmpty()) {
            if (homeDir == null) {
                homeDir = Platform.getPlatform().getTmpDirectory().toPath();
            }

            Path conf = homeDir.resolve("conf");

            if (!conf.toFile().exists()) {
                conf.toFile().mkdirs();
            }

            for (Path configFile : configFiles) {
                try {
                    Files.copy(configFile, conf.resolve(configFile.getFileName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        builder.setHomeDir(homeDir);
        keycloak = builder.start(keycloakServerConfigBuilder.toArgs());
    }

    @Override
    public void stop() {
        try {
            keycloak.stop();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

    @Override
    public String getManagementBaseUrl() {
        return "http://localhost:9001";
    }
}
