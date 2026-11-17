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

package org.keycloak.testframework.conformance;

import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.testframework.conformance.runner.ConformanceApiClient;
import org.keycloak.testframework.logging.JBossContainerLogConsumer;
import org.keycloak.testframework.util.ContainerImages;

import org.jboss.logging.Logger;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class OpenIdConformanceServer implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(OpenIdConformanceServer.class);

    public static final String MONGODB_CONTAINER = "mongodb";
    public static final String CONFORMANCE_CONTAINER = "conformance";
    public static final String NGINX_CONTAINER = "nginx";

    // The suite URL within the container network, to be used by Keycloak redirect URIs and web origins
    public static final URI INTERNAL_BASE_URI = URI.create("https://nginx:8443");

    // The URL at which the suite containers reach Keycloak, which must be set as the Keycloak 'hostname' option
    public static final URI KEYCLOAK_BASE_URI = URI.create("https://host.testcontainers.internal:8443");

    private static final String NGINX_CERTIFICATE_PATH = "/etc/ssl/certs/nginx-selfsigned.crt";

    private static OpenIdConformanceServer instance;

    private final Network network;
    private final GenericContainer<?> mongo;
    private final GenericContainer<?> server;
    private final GenericContainer<?> nginx;
    private final ConformanceApiClient client;
    private final URI baseUri;
    private final X509Certificate nginxCertificate;

    private OpenIdConformanceServer(Network network, GenericContainer<?> mongo, GenericContainer<?> server,
                                    GenericContainer<?> nginx, URI baseUri, SSLContext sslContext, X509Certificate nginxCertificate) {
        this.network = network;
        this.mongo = mongo;
        this.server = server;
        this.nginx = nginx;
        this.baseUri = baseUri;
        this.nginxCertificate = nginxCertificate;
        this.client = new ConformanceApiClient(baseUri, sslContext);
    }

    /**
     * The suite is a singleton as it is also used to discover module variants while tests are collected, before
     * the test framework injects it.
     */
    public static synchronized OpenIdConformanceServer instance() {
        if (instance == null) {
            // Must be exposed before the containers start so they can reach Keycloak on the Docker host
            Testcontainers.exposeHostPorts(KEYCLOAK_BASE_URI.getPort());
            instance = start();
        }
        return instance;
    }

    private static OpenIdConformanceServer start() {
        LOGGER.trace("Starting Conformance Suite with MongoDB (storage) and Nginx (access point)");

        Network network = Network.newNetwork();

        GenericContainer<?> mongo = new GenericContainer<>(DockerImageName.parse(ContainerImages.getContainerImageName(MONGODB_CONTAINER)))
                .withNetwork(network)
                .withNetworkAliases("mongodb")
                .withExposedPorts(27017)
                .withLogConsumer(new JBossContainerLogConsumer(Logger.getLogger("managed.conformance.mongodb")))
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        GenericContainer<?> server = new GenericContainer<>(DockerImageName.parse(ContainerImages.getContainerImageName(CONFORMANCE_CONTAINER)))
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
                .withLogConsumer(new JBossContainerLogConsumer(Logger.getLogger("managed.conformance.suite")))
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(4)));

        GenericContainer<?> nginx = new GenericContainer<>(
                DockerImageName.parse(ContainerImages.getContainerImageName(NGINX_CONTAINER)))
                .withExposedPorts(8443)
                .withNetwork(network)
                .withNetworkAliases("nginx")
                .dependsOn(server)
                .withLogConsumer(new JBossContainerLogConsumer(Logger.getLogger("managed.conformance.nginx")))
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        try {
            mongo.start();
            server.start();
            nginx.start();

            URI baseUri = URI.create("https://" + nginx.getHost() + ":" + nginx.getMappedPort(8443));
            X509Certificate nginxCertificate = nginxCertificate(nginx);
            SSLContext sslContext = sslContextTrusting(nginxCertificate);
            OpenIdConformanceServer suite =
                    new OpenIdConformanceServer(network, mongo, server, nginx, baseUri, sslContext, nginxCertificate);
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

    // The suite's nginx TLS certificate, so a client can trust it without disabling verification.
    public X509Certificate nginxCertificate() {
        return nginxCertificate;
    }

    // Rewrites a suite internal URI to one reachable from the test JVM.
    public URI externalUri(URI internalUri) {
        if (!INTERNAL_BASE_URI.getHost().equals(internalUri.getHost())) {
            return internalUri;
        }
        String query = internalUri.getRawQuery() != null ? "?" + internalUri.getRawQuery() : "";
        return URI.create(baseUri + internalUri.getRawPath() + query);
    }

    @Override
    public void close() {
        List.of(nginx, server, mongo).forEach(GenericContainer::stop);
        network.close();
        synchronized (OpenIdConformanceServer.class) {
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
