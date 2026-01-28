/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature.Type;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.model.StoreProvider;
import org.keycloak.utils.StringUtil;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public abstract class AbstractQuarkusDeployableContainer implements DeployableContainer<KeycloakQuarkusConfiguration> {

    private static final Logger log = Logger.getLogger(AbstractQuarkusDeployableContainer.class);

    protected static AtomicBoolean restart = new AtomicBoolean();

    @Inject
    protected Instance<SuiteContext> suiteContext;

    protected KeycloakQuarkusConfiguration configuration;
    protected List<String> additionalBuildArgs = Collections.emptyList();
    protected Map<String, List<String>> spis = new HashMap<>();

    @Override
    public Class<KeycloakQuarkusConfiguration> getConfigurationClass() {
        return KeycloakQuarkusConfiguration.class;
    }

    @Override
    public void setup(KeycloakQuarkusConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        try {
            log.infof("Deploying archive %s to quarkus container", archive.getName());
            deployArchiveToServer(archive);
            restartServer();
            log.infof("Deployed archive %s and restarted quarkus container", archive.getName());
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }

        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        log.infof("Undeploying archive %s from quarkus container", archive.getName());
        File wrkDir = configuration.getProvidersPath().resolve("providers").toFile();
        try {
            if (isWindows()) {
                // stop before updating providers to avoid file locking issues on Windows
                stop();
            }
            Files.deleteIfExists(wrkDir.toPath().resolve(archive.getName()));
            restartServer();
            log.infof("Undeployed archive %s and restarted quarkus container", archive.getName());
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return null;
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {

    }

    @Override
    public void undeploy(Descriptor descriptor) {

    }

    public void restartServer() throws Exception {
        stop();
        start();
    }

    protected List<String> getArgs() {
        return getArgs(new HashMap<>());
    }

    protected List<String> getArgs(Map<String, String> env) {
        List<String> commands = new ArrayList<>();

        commands.add("-v");
        commands.add("start");
        commands.add("--http-enabled=true");

        if (Boolean.parseBoolean(System.getProperty("auth.server.debug", "false"))) {
            commands.add("--debug");

            String debugPort = configuration.getDebugPort() > 0 ? Integer.toString(configuration.getDebugPort()) : System.getProperty("auth.server.debug.port", "5005");
            env.put("DEBUG_PORT", debugPort);

            String debugSuspend = System.getProperty("auth.server.debug.suspend");
            if (debugSuspend != null) {
                env.put("DEBUG_SUSPEND", debugSuspend);
            }
        }

        commands.add("--http-port=" + configuration.getBindHttpPort());
        commands.add("--https-port=" + configuration.getBindHttpsPort());

        commands.add("--http-relative-path=/auth");
        commands.add("--health-enabled=true"); // expose something to management interface to turn it on

        if (suiteContext.get().isAuthServerMigrationEnabled()) {
            commands.add("--hostname-strict=false");
        } else { // Do not set management port for older versions of Keycloak for migration tests - available since Keycloak 25
            commands.add("--http-management-port=" + configuration.getManagementPort());
        }

        if (suiteContext.get().getMigrationContext().isRunningMigrationTest()) {
            commands.add("--spi-datastore-legacy-allow-migrate-existing-database-to-snapshot=true");
        }

        if (configuration.getRoute() != null) {
            commands.add("--spi-cache-embedded-default-node-name=" + configuration.getRoute());
        }

        if (System.getProperty("auth.server.quarkus.log-level") != null) {
            commands.add("--log-level=" + System.getProperty("auth.server.quarkus.log-level"));
        }

        if (System.getProperty("auth.server.host") != null) {
            commands.add("-Dauth.server.host=" + System.getProperty("auth.server.host"));
        }

        commands.addAll(getAdditionalBuildArgs());

        commands = configureArgs(commands);

        final StoreProvider storeProvider = StoreProvider.getCurrentProvider();
        final String cacheMode = System.getProperty("auth.server.quarkus.cluster.config", "local");

        if ("local".equals(cacheMode)) {
            commands.add("--cache=local");
            // Save ~2s for each Quarkus startup, when we know ISPN cluster is empty. See https://github.com/keycloak/keycloak/issues/21033
            commands.add("-Djgroups.join_timeout=10");
        } else {
            commands.add("--cache=ispn");
            commands.add("--cache-config-file=cluster-" + cacheMode + ".xml");

            var stack = System.getProperty("auth.server.quarkus.cluster.stack");
            if (stack != null) {
                commands.add("--cache-stack=" + stack);
            }
        }

        log.debugf("FIPS Mode: %s", configuration.getFipsMode());

        if (configuration.getFipsMode() != FipsMode.DISABLED) {
            addFipsOptions(commands);
        }

        addStorageOptions(storeProvider, commands);
        addFeaturesOption(commands);

        spis.values().forEach(commands::addAll);

        var features = getDefaultFeatures();
        if (features.contains("clusterless") || features.contains("multi-site")) {
            commands.add("--cache-remote-host=127.0.0.1");
            commands.add("--cache-remote-username=keycloak");
            commands.add("--cache-remote-password=Password1!");
            commands.add("--cache-remote-tls-enabled=false");
            commands.add("--spi-cache-embedded-default-site-name=test");
        }

        if (!suiteContext.get().isAuthServerMigrationEnabled()) {
            commands.add("--bootstrap-admin-username=" + AuthRealm.ADMIN);
            commands.add("--bootstrap-admin-password=" + AuthRealm.ADMIN);
        }

        return commands;
    }

    protected void addFeaturesOption(List<String> commands) {
        String enabledFeatures = Optional.ofNullable(configuration.getEnabledFeatures()).orElse("");
        String disabledFeatures = Optional.ofNullable(configuration.getDisabledFeatures()).orElse("");

        var disabled = ProfileAssume.getDisabledFeatures();
        // TODO: this is not ideal, we're trying to infer what should be enabled / disabled from what was captured
        // as the disabled features. This at least does not understand the profile and may not age well.
        // We should consider a direct mechanism - that is part of the persisted configuration - for toggling each
        // feature
        if (disabled != null) {
            enabledFeatures = "";
            disabledFeatures = "";
            for (Profile.Feature f : Profile.Feature.values()) {
                if (disabled.contains(f)) {
                    if (f.getType() == Type.DEFAULT) {
                        disabledFeatures = f.getUnversionedKey() + (disabledFeatures.isEmpty() ? "" : ("," + disabledFeatures));
                    }
                } else {
                    if (f.getType() != Type.DEFAULT) {
                        enabledFeatures = f.getVersionedKey() + (enabledFeatures.isEmpty() ? "" : ("," + enabledFeatures));
                    }
                }
            }
        } else if (configuration.getFipsMode() != FipsMode.DISABLED) {
            enabledFeatures = "fips" + (enabledFeatures.isEmpty() ? "" : ("," + enabledFeatures));
        }

        if (!StringUtil.isBlank(enabledFeatures)) {
            appendOrAddCommand(commands, "--features=", enabledFeatures);
        }

        if (!StringUtil.isBlank(disabledFeatures)) {
            appendOrAddCommand(commands, "--features-disabled=", disabledFeatures);
        }
    }

    private void appendOrAddCommand(List<String> commands, String command, String addition) {
        Iterator<String> iterator = commands.iterator();

        while (iterator.hasNext()) {
            String existingCommand = iterator.next();

            if (existingCommand.startsWith(command)) {
                iterator.remove();
                commands.add(existingCommand + "," + addition);
                return;
            }
        }

        commands.add(command + addition);
    }

    protected List<String> configureArgs(List<String> commands) {
        return commands;
    }

    private void deployArchiveToServer(Archive<?> archive) throws IOException, LifecycleException {
        if (isWindows()) {
            // stop before updating providers to avoid file locking issues on Windows
            stop();
        }
        File providersDir = configuration.getProvidersPath().resolve("providers").toFile();
        InputStream zipStream = archive.as(ZipExporter.class).exportAsInputStream();
        Files.copy(zipStream, providersDir.toPath().resolve(archive.getName()), StandardCopyOption.REPLACE_EXISTING);
    }

    protected static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public List<String> getAdditionalBuildArgs() {
        return additionalBuildArgs;
    }

    public void setAdditionalBuildArgs(List<String> newArgs) {
        additionalBuildArgs = newArgs;
    }

    public void resetConfiguration() {
        additionalBuildArgs = Collections.emptyList();
        this.spis.clear();
    }

    protected void waitForReadiness() throws Exception {
        SuiteContext suiteContext = this.suiteContext.get();
        //TODO: not sure if the best endpoint but it makes sure that everything is properly initialized. Once we have
        // support for MP Health this should change
        URL contextRoot = new URL(getBaseUrl(suiteContext) + "/auth/realms/master/");
        HttpURLConnection connection;
        long startTime = System.currentTimeMillis();
        Exception e = null;

        while (true) {
            if (System.currentTimeMillis() - startTime > getStartTimeout()) {
                stop();
                throw new IllegalStateException("Timeout [" + getStartTimeout() + "] while waiting for Quarkus server", e);
            }

            checkLiveness();

            try {
                // wait before checking for opening a new connection
                Thread.sleep(1000);
                if ("https".equals(contextRoot.getProtocol())) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) (connection = (HttpURLConnection) contextRoot.openConnection());
                    httpsConnection.setSSLSocketFactory(createInsecureSslSocketFactory());
                    httpsConnection.setHostnameVerifier(createInsecureHostnameVerifier());
                } else {
                    connection = (HttpURLConnection) contextRoot.openConnection();
                }

                connection.setReadTimeout((int) getStartTimeout());
                connection.setConnectTimeout((int) getStartTimeout());
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    break;
                }

                connection.disconnect();
            } catch (Exception exception) {
                e = exception;
            }
        }

        log.infof("Keycloak is ready at %s", contextRoot);
    }

    protected abstract void checkLiveness() throws Exception;

    private URL getBaseUrl(SuiteContext suiteContext) throws MalformedURLException {
        URL baseUrl = suiteContext.getAuthServerInfo().getContextRoot();

        // might be running behind a load balancer
        if ("https".equals(baseUrl.getProtocol())) {
            baseUrl = new URL(baseUrl.toString().replace(String.valueOf(baseUrl.getPort()), String.valueOf(configuration.getBindHttpsPort())));
        } else {
            baseUrl = new URL(baseUrl.toString().replace(String.valueOf(baseUrl.getPort()), String.valueOf(configuration.getBindHttpPort())));
        }
        return baseUrl;
    }

    private HostnameVerifier createInsecureHostnameVerifier() {
        return (s, sslSession) -> true;
    }

    private SSLSocketFactory createInsecureSslSocketFactory() throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};

        SSLContext sslContext;
        SSLSocketFactory socketFactory;

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            socketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Can't create unsecure trust manager");
        }
        return socketFactory;
    }

    private long getStartTimeout() {
        return TimeUnit.SECONDS.toMillis(configuration.getStartupTimeoutInSeconds());
    }

    private void addStorageOptions(StoreProvider storeProvider, List<String> commands) {
        log.debugf("Store '%s' is used.", storeProvider.name());
        storeProvider.addStoreOptions(commands);
    }

    private void addFipsOptions(List<String> commands) {
        commands.add("--fips-mode=" + configuration.getFipsMode().toString());

        log.debugf("Keystore file: %s, truststore file: %s",
                configuration.getKeystoreFile(),
                configuration.getTruststoreFile());
        commands.add("--https-key-store-file=" + configuration.getKeystoreFile());
        commands.add("--https-key-store-password=" + configuration.getKeystorePassword());
        commands.add("--https-trust-store-file=" + configuration.getTruststoreFile());
        commands.add("--https-trust-store-password=" + configuration.getTruststorePassword());
        commands.add("--spi-truststore-file-file=" + configuration.getTruststoreFile());
        commands.add("--spi-truststore-file-password=" + configuration.getTruststorePassword());

        // BCFIPS approved mode requires passwords of at least 112 bits (14 characters) to be used. To bypass this, we use this by default
        // as testsuite uses shorter passwords everywhere
        if (FipsMode.STRICT == configuration.getFipsMode()) {
            commands.add("--spi-password-hashing-pbkdf2-max-padding-length=14");
            commands.add("--spi-password-hashing-pbkdf2-sha256-max-padding-length=14");
            commands.add("--spi-password-hashing-pbkdf2-sha512-max-padding-length=14");
        }

        commands.add("--log-level=INFO,org.keycloak.common.crypto:TRACE,org.keycloak.crypto:TRACE,org.keycloak.truststore:TRACE");

        configuration.appendJavaOpts("-Djava.security.properties=" + System.getProperty("auth.server.java.security.file"));
    }

    private Collection<String> getDefaultFeatures() {
        var features = configuration.getEnabledFeatures();
        if (features == null || features.isBlank()) {
            return List.of();
        }
        return Arrays.stream(features.split(",")).collect(Collectors.toSet());
    }

    public void setSpiConfig(String spi, List<String> args) {
        this.spis.put(spi, args);
    }

    public void removeSpiConfig(String spi) {
        this.spis.remove(spi);
    }
}
