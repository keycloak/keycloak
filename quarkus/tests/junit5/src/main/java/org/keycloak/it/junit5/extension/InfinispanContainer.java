/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.junit5.extension;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;

import java.time.Duration;
import java.util.stream.Stream;

public class InfinispanContainer extends GenericContainer<InfinispanContainer> {

    private final Logger LOG = Logger.getLogger(getClass());
    public static final String PORT = System.getProperty("keycloak.externalInfinispan.port", "11222");
    public static final String USERNAME = System.getProperty("keycloak.externalInfinispan.username", "keycloak");
    public static final String PASSWORD = System.getProperty("keycloak.externalInfinispan.password", DatabaseContainer.DEFAULT_PASSWORD);

    public static RemoteCacheManager remoteCacheManager;

    public InfinispanContainer() {
        super(getImageName());
        withEnv("USER", USERNAME);
        withEnv("PASS", PASSWORD);
        withNetworkMode("host");

        // the images in the 'infinispan-test' repository point to tags that are frequently refreshed, therefore, always pull them
        if (getImageName().startsWith("quay.io/infinispan-test")) {
            withImagePullPolicy(PullPolicy.alwaysPull());
        }


        //order of waitingFor and withStartupTimeout matters as the latter sets the timeout for WaitStrategy set by waitingFor
        waitingFor(Wait.forLogMessage(".*Infinispan Server.*started in.*", 1));
        withStartupTimeout(Duration.ofMinutes(5));
    }

    private static String getImageName() {
        String INFINISPAN_IMAGE = System.getProperty("kc.infinispan.container.image");
        if (INFINISPAN_IMAGE.matches("quay.io/infinispan/.*-SNAPSHOT")) {
            // If the image name ends with SNAPSHOT, someone is trying to use a snapshot release of Infinispan.
            // Then switch to the closest match of the Infinispan test container
            INFINISPAN_IMAGE = INFINISPAN_IMAGE.replaceAll("quay.io/infinispan/", "quay.io/infinispan-test/");
            INFINISPAN_IMAGE = INFINISPAN_IMAGE.replaceAll("[0-9]*-SNAPSHOT$", "x");
        }

        return INFINISPAN_IMAGE;
    }

    public static void removeCache(String cache) {
        // first stop the cache to avoid leaking MBeans for the HotRodClient
        // see: https://issues.redhat.com/browse/ISPN-15606
        remoteCacheManager.getCache(cache).stop();
        remoteCacheManager.administration().removeCache(cache);
    }

    private void establishHotRodConnection() {
        ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                .addServers(getContainerIpAddress() + ":11222")
                .security()
                .authentication()
                .username(getUsername())
                .password(getPassword())
                .clientIntelligence(ClientIntelligence.BASIC);

        configBuilder.statistics().enable()
                .statistics().jmxEnable();

        remoteCacheManager = new RemoteCacheManager(configBuilder.build());
    }

    @Override
    public void start() {
        logger().info("Starting ISPN container");

        super.start();

        establishHotRodConnection();

        Stream.of("sessions", "actionTokens", "authenticationSessions", "clientSessions", "offlineSessions", "offlineClientSessions", "loginFailures", "work")
                .forEach(cacheName -> {
                    LOG.infof("Creating cache '%s'", cacheName);
                    createCache(remoteCacheManager, cacheName);
                });
    }

    @Override
    public void stop() {
        logger().info("Stopping ISPN container");

        if (remoteCacheManager != null) {
            remoteCacheManager.stop();
        }

        super.stop();
    }

    public void createCache(RemoteCacheManager remoteCacheManager, String cacheName) {
        String xml = String.format("<distributed-cache name=\"%s\" mode=\"SYNC\" owners=\"2\"></distributed-cache>" , cacheName);
        remoteCacheManager.administration().getOrCreateCache(cacheName, new XMLStringConfiguration(xml));
    }

    public String getPort() {
        return PORT;
    }

    public String getUsername() {
        return USERNAME;
    }

    public String getPassword() {
        return PASSWORD;
    }
}
