package org.keycloak.testsuite.arquillian.containers;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.exec.StreamPumper;
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
import org.keycloak.testsuite.arquillian.SuiteContext;

/**
 * @author mhajas
 */
public class KeycloakQuarkusServerDeployableContainer implements DeployableContainer<KeycloakQuarkusConfiguration> {

    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final String AUTH_SERVER_QUARKUS_MAP_STORAGE_PROFILE = "auth.server.quarkus.mapStorage.profile.config";

    private static final Logger log = Logger.getLogger(KeycloakQuarkusServerDeployableContainer.class);

    private KeycloakQuarkusConfiguration configuration;
    private Process container;
    private static AtomicBoolean restart = new AtomicBoolean();
    private Thread stdoutForwarderThread;

    @Inject
    private Instance<SuiteContext> suiteContext;

    private List<String> additionalBuildArgs = Collections.emptyList();

    @Override
    public Class<KeycloakQuarkusConfiguration> getConfigurationClass() {
        return KeycloakQuarkusConfiguration.class;
    }

    @Override
    public void setup(KeycloakQuarkusConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        try {
            importRealm();
            container = startContainer();
            stdoutForwarderThread = new Thread(new StreamPumper(container.getInputStream(), System.out));
            stdoutForwarderThread.start();
            waitForReadiness();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        if (container.isAlive()) {
            try {
                destroyDescendantsOnWindows(container, false);
                container.destroy();
                container.waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                destroyDescendantsOnWindows(container, true);
                container.destroyForcibly();
            }
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return null;
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        log.infof("Trying to deploy: " + archive.getName());

        try {
            deployArchiveToServer(archive);
            restartServer();
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(),e);
        }

        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        File wrkDir = configuration.getProvidersPath().resolve("providers").toFile();
        try {
            if (isWindows()) {
                // stop before updating providers to avoid file locking issues on Windows
                stop();
            }
            Files.deleteIfExists(wrkDir.toPath().resolve(archive.getName()));
            restartServer();
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(),e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {

    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {

    }

    private void importRealm() throws IOException, URISyntaxException {
        if (suiteContext.get().isAuthServerMigrationEnabled() && configuration.getImportFile() != null) {
            final String importFileName = configuration.getImportFile();

            log.infof("Importing realm from file '%s'", importFileName);

            final URL url = getClass().getResource("/migration-test/" + importFileName);
            if (url == null) throw new IllegalArgumentException("Cannot find migration import file");

            final Path path = Paths.get(url.toURI());
            final File wrkDir = configuration.getProvidersPath().resolve("bin").toFile();
            final List<String> commands = new ArrayList<>();

            commands.add(getCommand());
            commands.add("import");
            commands.add("--file=" + wrkDir.toPath().relativize(path));

            final ProcessBuilder pb = new ProcessBuilder(commands);
            pb.directory(wrkDir).inheritIO().start();
        }
    }

    private Process startContainer() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(getProcessCommands());
        File wrkDir = configuration.getProvidersPath().resolve("bin").toFile();
        ProcessBuilder builder = pb.directory(wrkDir).redirectErrorStream(true);

        String javaOpts = configuration.getJavaOpts();

        if (javaOpts != null) {
            builder.environment().put("JAVA_OPTS", javaOpts);
        }

        builder.environment().put("KEYCLOAK_ADMIN", "admin");
        builder.environment().put("KEYCLOAK_ADMIN_PASSWORD", "admin");

        if (restart.compareAndSet(false, true)) {
            deleteDirectory(configuration.getProvidersPath().resolve("data"));
        }

        return builder.start();
    }

    private String[] getProcessCommands() {
        List<String> commands = new ArrayList<>();
        commands.add(getCommand());
        commands.add("-v");
        commands.add("start");
        commands.add("--optimized");
        commands.add("--http-enabled=true");

        if (Boolean.parseBoolean(System.getProperty("auth.server.debug", "false"))) {
            commands.add("--debug");
            if (configuration.getDebugPort() > 0) {
                commands.add(Integer.toString(configuration.getDebugPort()));
            } else {
                commands.add(System.getProperty("auth.server.debug.port", "5005"));
            }
        }

        commands.add("--http-port=" + configuration.getBindHttpPort());
        commands.add("--https-port=" + configuration.getBindHttpsPort());

        if (configuration.getRoute() != null) {
            commands.add("-Djboss.node.name=" + configuration.getRoute());
        }

        String mapStorageProfile = System.getProperty(AUTH_SERVER_QUARKUS_MAP_STORAGE_PROFILE);
        // only run build during restarts or when running cluster tests

        if (restart.get() || "ha".equals(System.getProperty("auth.server.quarkus.cluster.config"))) {
            commands.removeIf("--optimized"::equals);
            commands.add("--http-relative-path=/auth");

            if (mapStorageProfile == null) {
                String cacheMode = System.getProperty("auth.server.quarkus.cluster.config", "local");

                if ("local".equals(cacheMode)) {
                    commands.add("--cache=local");
                } else {
                    commands.add("--cache-config-file=cluster-" + cacheMode + ".xml");
                }
            }
        }

        addStorageOptions(commands);

        commands.addAll(getAdditionalBuildArgs());

        log.debugf("Quarkus parameters: %s", commands);

        return commands.toArray(new String[0]);
    }

    private void addStorageOptions(List<String> commands) {
        String mapStorageProfile = System.getProperty(AUTH_SERVER_QUARKUS_MAP_STORAGE_PROFILE);

        if (mapStorageProfile != null) {
            // We need to drop optimized flag because --storage is build option therefore startup requires re-augmentation
            commands.removeIf("--optimized"::equals);

            // As config is re-augmented on startup we need to also add --http-relative-path as ant build from
            // integration-arquillian/servers/auth-server/quarkus/ant/configure.xml is replaced by build invoked on
            // startup when we add new build option below
            commands.add("--http-relative-path=/auth");

            switch (mapStorageProfile) {
                case "chm":
                    commands.add("--storage=" + mapStorageProfile);
                    break;
                case "jpa":
                    commands.add("--storage=" + mapStorageProfile);
                    commands.add("--db-username=" + System.getProperty("keycloak.map.storage.connectionsJpa.url"));
                    commands.add("--db-password=" + System.getProperty("keycloak.map.storage.connectionsJpa.user"));
                    commands.add("--db-url=" + System.getProperty("keycloak.map.storage.connectionsJpa.password"));
                    break;
                case "hotrod":
                    commands.add("--storage=" + mapStorageProfile);
                    // TODO: URL / username / password
                    break;
            }
        }
    }

    private void waitForReadiness() throws MalformedURLException, LifecycleException {
        SuiteContext suiteContext = this.suiteContext.get();
        //TODO: not sure if the best endpoint but it makes sure that everything is properly initialized. Once we have
        // support for MP Health this should change
        URL contextRoot = new URL(getBaseUrl(suiteContext) + "/auth/realms/master/");
        HttpURLConnection connection;
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > getStartTimeout()) {
                stop();
                throw new IllegalStateException("Timeout [" + getStartTimeout() + "] while waiting for Quarkus server");
            }

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
            } catch (Exception ignore) {
            }
        }

        log.infof("Keycloak is ready at %s", contextRoot);
    }

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
        return new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
    }

    private SSLSocketFactory createInsecureSslSocketFactory() throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
            }

            public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
            }

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

    public void resetConfiguration() {
        additionalBuildArgs = Collections.emptyList();
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

    public void restartServer() throws Exception {
        stop();
        start();
    }

    private String getCommand() {
        if (isWindows()) {
            return configuration.getProvidersPath().resolve("bin").resolve("kc.bat").toString();
        }
        return "./kc.sh";
    }

    public List<String> getAdditionalBuildArgs() {
        return additionalBuildArgs;
    }

    public void setAdditionalBuildArgs(List<String> newArgs) {
        additionalBuildArgs = newArgs;
    }

    private void destroyDescendantsOnWindows(Process parent, boolean force) {
        if (!isWindows()) {
            return;
        }

        CompletableFuture allProcesses = CompletableFuture.completedFuture(null);

        for (ProcessHandle process : parent.descendants().collect(Collectors.toList())) {
            if (force) {
                process.destroyForcibly();
            } else {
                process.destroy();
            }

            allProcesses = CompletableFuture.allOf(allProcesses, process.onExit());
        }

        try {
            allProcesses.get(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to terminate descendants processes", cause);
        }

        try {
            // TODO: remove this. do not ask why, but on Windows we are here even though the process was previously terminated
            // without this pause, tests re-installing dist before tests should fail
            // looks like pausing the current thread let windows to cleanup processes?
            // more likely it is env dependent
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    private static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public static void deleteDirectory(final Path directory) throws IOException {
        if (Files.isDirectory(directory, new LinkOption[0])) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(file);
                    } catch (IOException var4) {
                    }

                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    try {
                        Files.delete(dir);
                    } catch (IOException var4) {
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
