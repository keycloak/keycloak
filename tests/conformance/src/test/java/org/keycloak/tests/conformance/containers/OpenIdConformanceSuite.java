/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.conformance.containers;

import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.tests.conformance.runner.ConformanceApiClient;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class OpenIdConformanceSuite implements AutoCloseable {

    public static final String IMAGE_TAG_PROPERTY = "keycloak.conformance.imageTag";
    public static final String MONGO_IMAGE_TAG_PROPERTY = "keycloak.conformance.mongoImageTag";

    // The suite URL within the container network, to be used by Keycloak redirect URIs and web origins
    public static final URI INTERNAL_BASE_URI = URI.create("https://nginx:8443");

    // The URL at which the suite containers reach Keycloak, which must be set as the Keycloak 'hostname' option
    public static final URI KEYCLOAK_BASE_URI = URI.create("https://host.testcontainers.internal:8443");

    // Fallbacks for running outside Maven, where the defaults are set by the pom properties of the same name
    private static final String DEFAULT_IMAGE_TAG = "release-v5.1.44";
    private static final String DEFAULT_MONGO_IMAGE_TAG = "6.0.13";
    private static final String NGINX_CERTIFICATE_PATH = "/etc/ssl/certs/nginx-selfsigned.crt";

    private static OpenIdConformanceSuite instance;

    private final Network network;
    private final GenericContainer<?> mongo;
    private final GenericContainer<?> server;
    private final GenericContainer<?> nginx;
    private final ConformanceApiClient client;

    private OpenIdConformanceSuite(Network network, GenericContainer<?> mongo, GenericContainer<?> server,
            GenericContainer<?> nginx, URI baseUri, SSLContext sslContext) {
        this.network = network;
        this.mongo = mongo;
        this.server = server;
        this.nginx = nginx;
        this.client = new ConformanceApiClient(baseUri, sslContext);
    }

    /**
     * The suite is a singleton as it is also used to discover module variants while tests are collected, before
     * the test framework injects it.
     */
    public static synchronized OpenIdConformanceSuite instance() {
        if (instance == null) {
            // Must be exposed before the containers start so they can reach Keycloak on the Docker host
            Testcontainers.exposeHostPorts(KEYCLOAK_BASE_URI.getPort());
            instance = start();
        }
        return instance;
    }

    private static OpenIdConformanceSuite start() {
        String imageTag = System.getProperty(IMAGE_TAG_PROPERTY, DEFAULT_IMAGE_TAG);
        String mongoImageTag = System.getProperty(MONGO_IMAGE_TAG_PROPERTY, DEFAULT_MONGO_IMAGE_TAG);
        Network network = Network.newNetwork();

        GenericContainer<?> mongo = new GenericContainer<>(DockerImageName.parse("mongo:" + mongoImageTag))
                .withNetwork(network)
                .withNetworkAliases("mongodb")
                .withExposedPorts(27017)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        GenericContainer<?> server = new GenericContainer<>(
                DockerImageName.parse("registry.gitlab.com/openid/conformance-suite:" + imageTag))
                .withNetwork(network)
                .withNetworkAliases("server")
                .withExposedPorts(8080)
                .withEnv("BASE_URL", INTERNAL_BASE_URI.toString())
                .withEnv("MONGODB_HOST", "mongodb")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withEnv("OIDC_GOOGLE_CLIENTID", "google-client")
                .withEnv("OIDC_GOOGLE_SECRET", "google-secret")
                .withEnv("OIDC_GITLAB_CLIENTID", "gitlab-client")
                .withEnv("OIDC_GITLAB_SECRET", "gitlab-secret")
                .dependsOn(mongo)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(4)));

        GenericContainer<?> nginx = new GenericContainer<>(
                DockerImageName.parse("registry.gitlab.com/openid/conformance-suite/nginx:" + imageTag))
                .withExposedPorts(8443)
                .withNetwork(network)
                .withNetworkAliases("nginx")
                .dependsOn(server)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        try {
            mongo.start();
            server.start();
            nginx.start();

            URI baseUri = URI.create("https://" + nginx.getHost() + ":" + nginx.getMappedPort(8443));
            SSLContext sslContext = sslContextTrusting(nginxCertificate(nginx));
            OpenIdConformanceSuite suite = new OpenIdConformanceSuite(network, mongo, server, nginx, baseUri, sslContext);
            suite.client().waitUntilAvailable(Duration.ofMinutes(4));
            return suite;
        } catch (RuntimeException e) {
            List.of(nginx, server, mongo).forEach(GenericContainer::stop);
            network.close();
            throw e;
        }
    }

    public ConformanceApiClient client() {
        return client;
    }

    @Override
    public void close() {
        List.of(nginx, server, mongo).forEach(GenericContainer::stop);
        network.close();
        synchronized (OpenIdConformanceSuite.class) {
            if (instance == this) {
                instance = null;
            }
        }
    }

    private static X509Certificate nginxCertificate(GenericContainer<?> nginx) {
        try {
            return nginx.copyFileFromContainer(NGINX_CERTIFICATE_PATH,
                    input -> (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the conformance suite TLS certificate from " + NGINX_CERTIFICATE_PATH, e);
        }
    }

    private static SSLContext sslContextTrusting(X509Certificate certificate) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("conformance-nginx", certificate);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagerFactory.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL context trusting the conformance suite certificate", e);
        }
    }
}
