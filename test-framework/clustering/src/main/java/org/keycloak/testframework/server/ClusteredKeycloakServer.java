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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.keycloak.it.utils.DockerKeycloakDistribution;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.infinispan.CacheType;
import org.keycloak.testframework.logging.JBossLogConsumer;

import org.infinispan.server.test.core.CountdownLatchLoggingConsumer;
import org.jboss.logging.Logger;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LazyFuture;

public class ClusteredKeycloakServer implements KeycloakServer {

    private static final String CLUSTER_VIEW_REGEX = ".*ISPN000093.*(?<=\\()(%1$d)(?=\\)).*|.*ISPN000094.*(?<=\\()(%1$d)(?=\\)).*";
    private static final boolean MANUAL_STOP = true;
    private static final int REQUEST_PORT = 8080;
    private static final int MANAGEMENT_PORT = 9000;
    public static final String SNAPSHOT_IMAGE = "-";

    private final DockerKeycloakDistribution[] containers;
    private final String images;

    private static LazyFuture<String> defaultImage() {
        return DockerKeycloakDistribution.createImage(true);
    }

    public ClusteredKeycloakServer(int mumServers, String images) {
        containers = new DockerKeycloakDistribution[mumServers];
        this.images = images;
    }

    @Override
    public void start(KeycloakServerConfigBuilder configBuilder) {
        int numServers = containers.length;
        CountdownLatchLoggingConsumer clusterLatch = new CountdownLatchLoggingConsumer(numServers, String.format(CLUSTER_VIEW_REGEX, numServers));
        String[] imagePeServer = null;

        // Infinispan clustered cache
        configBuilder.cache(CacheType.ISPN);

        if (images == null || images.isEmpty() || (imagePeServer = images.split(",")).length == 1) {
            startContainersWithSameImage(configBuilder, imagePeServer == null ? SNAPSHOT_IMAGE : imagePeServer[0], clusterLatch);
        } else {
            startContainersWithMixedImage(configBuilder, imagePeServer, clusterLatch);
        }

        try {
            clusterLatch.await((long) numServers * DockerKeycloakDistribution.STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Expected %d cluster members".formatted(numServers), e);
        }
    }

    private void startContainersWithMixedImage(KeycloakServerConfigBuilder configBuilder, String[] imagePeServer, CountdownLatchLoggingConsumer clusterLatch) {
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
                    // Required otherwise we will receive an "Incorrect state of migration" error preventing startup
                    configBuilder.option("spi-datastore--legacy--allow-migrate-existing-database-to-snapshot", "true");
                    snapshotImage = defaultImage();
                }
                resolvedImage = snapshotImage;
            } else {
                resolvedImage = new RemoteDockerImage(DockerImageName.parse(imagePeServer[i]));
            }
            var container = new DockerKeycloakDistribution(false, MANUAL_STOP, REQUEST_PORT, exposedPorts, resolvedImage);
            containers[i] = container;

            copyProvidersAndConfigs(container, configBuilder);

            configureLogConsumers(container, i, clusterLatch);
            container.run(configBuilder.toArgs());
        }
    }

    private void startContainersWithSameImage(KeycloakServerConfigBuilder configBuilder, String image, CountdownLatchLoggingConsumer clusterLatch) {
        int[] exposedPorts = new int[]{REQUEST_PORT, MANAGEMENT_PORT};
        LazyFuture<String> imageFuture = image == null || SNAPSHOT_IMAGE.equals(image) ?
                defaultImage() :
                new RemoteDockerImage(DockerImageName.parse(image));
        for (int i = 0; i < containers.length; ++i) {
            var container = new DockerKeycloakDistribution(false, MANUAL_STOP, REQUEST_PORT, exposedPorts, imageFuture);
            containers[i] = container;

            copyProvidersAndConfigs(container, configBuilder);
            configureLogConsumers(container, i, clusterLatch);
            container.run(configBuilder.toArgs());
        }
    }

    private static void configureLogConsumers(DockerKeycloakDistribution container, int index, CountdownLatchLoggingConsumer clusterLatch) {
        var logger = new JBossLogConsumer(Logger.getLogger("managed.keycloak." + index));
        container.setCustomLogConsumer(logger.andThen(clusterLatch));
    }

    private void copyProvidersAndConfigs(DockerKeycloakDistribution container, KeycloakServerConfigBuilder configBuilder) {
        for (var dependency : configBuilder.toDependencies()) {
            container.copyProvider(dependency.getGroupId(), dependency.getArtifactId());
        }

        for(var config : configBuilder.toConfigFiles()) {
            container.copyConfigFile(config);
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
        return LoadBalancer.HOSTNAME;
    }

    @Override
    public String getManagementBaseUrl() {
        return getManagementBaseUrl(0);
    }

    @Override
    public boolean isTlsEnabled() {
        return false;
    }

    public int getBasePort(int index) {
        return containers[index].getMappedPort(REQUEST_PORT);
    }

    public String getBaseUrl(int index) {
        return "http://localhost:%d".formatted(getBasePort(index));
    }

    public String getManagementBaseUrl(int index) {
        return "http://localhost:%d".formatted(containers[index].getMappedPort(MANAGEMENT_PORT));
    }

    public int clusterSize() {
        return containers.length;
    }
}
