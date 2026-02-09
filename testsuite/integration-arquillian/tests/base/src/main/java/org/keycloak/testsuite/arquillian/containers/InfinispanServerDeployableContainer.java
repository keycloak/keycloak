/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian.containers;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 *
 * @author tkyjovsk
 */
public class InfinispanServerDeployableContainer implements DeployableContainer<InfinispanServerConfiguration> {

    protected static final Logger log = Logger.getLogger(InfinispanServerDeployableContainer.class);

    InfinispanServerConfiguration configuration;
    private Process infinispanServerProcess;

    private File pidFile;
    private JMXServiceURL jmxServiceURL;

    public static final Boolean CACHE_SERVER_AUTH = Boolean.parseBoolean(System.getProperty("cache.server.auth", "false"));

    @Override
    public Class<InfinispanServerConfiguration> getConfigurationClass() {
        return InfinispanServerConfiguration.class;
    }

    @Override
    public void setup(InfinispanServerConfiguration configuration) {
        this.configuration = configuration;
        pidFile = new File(configuration.getInfinispanHome(), "bin/server.pid");
    }

    @Override
    public void start() throws LifecycleException {
        List<String> commands = new ArrayList<>();
        commands.add("./server.sh");

        if (configuration.getServerConfig() != null) {
            commands.add("-c");
            commands.add(configuration.getServerConfig());
        }

        if (configuration.getPortOffset() != null && configuration.getPortOffset() > 0) {
            commands.add("-o");
            commands.add(configuration.getPortOffset().toString());
        }

        commands.add(String.format("-Dcom.sun.management.jmxremote.port=%s", configuration.getManagementPort()));
        commands.add("-Dcom.sun.management.jmxremote.authenticate=false");
        commands.add("-Dcom.sun.management.jmxremote.ssl=false");

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb = pb.directory(new File(configuration.getInfinispanHome(), "/bin")).inheritIO().redirectErrorStream(true);
        pb.environment().put("LAUNCH_ISPN_IN_BACKGROUND", "false");
        pb.environment().put("ISPN_PIDFILE", pidFile.getAbsolutePath());
        if (configuration.getJavaVmArguments() != null) {
            pb.environment().put("JAVA_OPTS", configuration.getJavaVmArguments());
        }

        String javaHome = configuration.getJavaHome();
        if (javaHome != null && !javaHome.isEmpty()) {
            pb.environment().put("JAVA_HOME", javaHome);
        }
        try {
            log.info("Starting Infinispan server");
            log.infof("  Home directory: %s", configuration.getInfinispanHome());
            log.infof("  Commands: %s", commands);
            log.infof("  Environment: %s", pb.environment());
            infinispanServerProcess = pb.start();

            trustAllCertificates();

            long startTimeMillis = System.currentTimeMillis();
            long startupTimeoutMillis = 30 * 1000;
            URL consoleURL = new URL(String.format("%s://localhost:%s/console/",
                    CACHE_SERVER_AUTH ? "https" : "http",
                    11222 + configuration.getPortOffset()));

            while (true) {
                Thread.sleep(1000);
                if (System.currentTimeMillis() > startTimeMillis + startupTimeoutMillis) {
                    stop();
                    throw new LifecycleException("Infinispan server startup timed out.");
                }

                HttpURLConnection connection = (HttpURLConnection) consoleURL.openConnection();
                connection.setReadTimeout(1000);
                connection.setConnectTimeout(1000);
                try {
                    connection.connect();
                    if (connection.getResponseCode() == 200) {
                        break;
                    }
                    connection.disconnect();
                } catch (ConnectException ex) {
                    // ignoring
                }
            }

            log.info("Infinispan server started.");

        } catch (IOException ex) {
            throw new LifecycleException("Unable to start Infinispan server.", ex);
        } catch (InterruptedException ex) {
            log.error("Infinispan server startup process interupted.", ex);
            stop();
        }
    }

    private void trustAllCertificates() {

        TrustManager[] trustAllCerts;
        trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to initialize a 'trust-all' trust manager.");
        }
    }

    @Override
    public void stop() throws LifecycleException {
        log.info("Stopping Infinispan server");
        infinispanServerProcess.destroy();
        try {
            infinispanServerProcess.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("Unable to stop Infinispan server within timeout. Stopping forcibly.");
            infinispanServerProcess.destroyForcibly();
        }
        log.info("Infinispan server stopped");
    }

    private long getPID() throws IOException {
        if (pidFile == null) {
            throw new IllegalStateException(String.format("Unable to find PID file '%s'", pidFile));
        }
        return Long.parseLong(Files.readAllLines(pidFile.toPath()).get(0).trim());
    }

    /**
     * Attach to a local Infinispan JVM, launch a management-agent, and return
     * its JMXServiceURL.
     *
     * @return
     */
    public JMXServiceURL getJMXServiceURL() throws IOException {
        if (jmxServiceURL == null) {
            jmxServiceURL = new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://localhost:%s/jmxrmi", configuration.getManagementPort()));
        }
        return jmxServiceURL;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return ProtocolDescription.DEFAULT;
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archv) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(Archive<?> archv) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deploy(Descriptor d) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(Descriptor d) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

}
