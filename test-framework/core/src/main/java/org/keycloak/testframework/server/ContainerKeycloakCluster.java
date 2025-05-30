/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testframework.server;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import io.quarkus.bootstrap.utils.BuildToolHelper;
import org.keycloak.it.utils.DockerKeycloakDistribution;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LazyFuture;

public class ContainerKeycloakCluster implements KeycloakServer {

    private static final boolean MANUAL_STOP = true;
    private static final int REQUEST_PORT = 8080;
    private static final int MANAGEMENT_PORT = 9000;
    public static final String SNAPSHOT_IMAGE = "-";

    private final DockerKeycloakDistribution[] containers;
    private final String images;
    private final boolean debug;

    private static LazyFuture<String> defaultImage() {
        Path classPathDir;
        try {
            classPathDir = Paths.get(Thread.currentThread().getContextClassLoader().getResource(".").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        var quarkusModule = BuildToolHelper.getProjectDir(classPathDir).getParent().getParent().resolve("quarkus");
        return DockerKeycloakDistribution.createImage(quarkusModule,true);
    }

    public ContainerKeycloakCluster(int mumServers, String images, boolean debug) {
        containers = new DockerKeycloakDistribution[mumServers];
        this.images = images;
        this.debug = debug;
    }

    @Override
    public void start(KeycloakServerConfigBuilder configBuilder) {
        if (!configBuilder.toDependencies().isEmpty()) {
            //throw new UnsupportedOperationException("Adding dependencies not supported yet.");
        }
        if (!configBuilder.toConfigFiles().isEmpty()) {
            throw new UnsupportedOperationException("Adding configuration files not supported yet.");
        }
        String[] imagePeServer = null;
        if (images == null || images.isEmpty() || (imagePeServer = images.split(",")).length == 1) {
            startContainersWithSameImage(configBuilder, imagePeServer == null ? SNAPSHOT_IMAGE : imagePeServer[0]);
        } else {
            startContainersWithMixedImage(configBuilder, imagePeServer);
        }
    }

    private void startContainersWithMixedImage(KeycloakServerConfigBuilder configBuilder, String[] imagePeServer) {
        assert imagePeServer != null;
        if (containers.length != imagePeServer.length) {
            throw new IllegalArgumentException("The number of containers and the number of images must match");
        }
        int[] exposedPorts = new int[]{REQUEST_PORT, MANAGEMENT_PORT};
        LazyFuture<String> snapshotImage = null;
        for (int i = 0; i < containers.length; ++i) {
            LazyFuture<String> resolvedImage;
            if (SNAPSHOT_IMAGE.equals(imagePeServer[i])) {
                if (snapshotImage == null) {
                    snapshotImage = defaultImage();
                }
                resolvedImage = snapshotImage;
            } else {
                resolvedImage = new RemoteDockerImage(DockerImageName.parse(imagePeServer[i]));
            }
            var container = new DockerKeycloakDistribution(debug, MANUAL_STOP, REQUEST_PORT, exposedPorts, resolvedImage);
            containers[i] = container;
            container.run(configBuilder.toArgs());
        }
    }

    private void startContainersWithSameImage(KeycloakServerConfigBuilder configBuilder, String image) {
        int[] exposedPorts = new int[]{REQUEST_PORT, MANAGEMENT_PORT};
        LazyFuture<String> imageFuture = image == null || SNAPSHOT_IMAGE.equals(image) ?
                defaultImage() :
                new RemoteDockerImage(DockerImageName.parse(image));
        for (int i = 0; i < containers.length; ++i) {
            var container = new DockerKeycloakDistribution(debug, MANUAL_STOP, REQUEST_PORT, exposedPorts, imageFuture);
            containers[i] = container;
            container.run(configBuilder.toArgs());
        }
    }

    @Override
    public void stop() {
        Arrays.stream(containers)
                .filter(Objects::nonNull)
                .forEach(DockerKeycloakDistribution::stop);
    }

    @Override
    public String getBaseUrl() {
        return getBaseUrl(0);
    }

    @Override
    public String getManagementBaseUrl() {
        return getManagementBaseUrl(0);
    }

    public String getBaseUrl(int index) {
        return "http://localhost:%d".formatted(containers[index].getMappedPort(REQUEST_PORT));
    }

    public String getManagementBaseUrl(int index) {
        return "http://localhost:%d".formatted(containers[index].getMappedPort(MANAGEMENT_PORT));
    }

    public int clusterSize() {
        return containers.length;
    }
}
