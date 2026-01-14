package org.keycloak.testframework.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.common.Version;
import org.keycloak.it.utils.Maven;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testframework.util.FileUtils;
import org.keycloak.testframework.util.ProcessUtils;
import org.keycloak.testframework.util.TmpDir;

import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.Dependency;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class DistributionKeycloakServer implements KeycloakServer {

    private static final Logger log = Logger.getLogger(DistributionKeycloakServer.class);

    private static final File INSTALL_DIR = Path.of(TmpDir.resolveTmpDir().getAbsolutePath(), "kc-test-framework", "keycloak").toFile();
    private static final String CMD = "kc" + (Environment.isWindows() ? ".bat" : ".sh");

    private File keycloakHomeDir;
    private Process keycloakProcess;

    private final boolean debug;
    private final boolean reuse;
    private final long startTimeout;
    private boolean tlsEnabled = false;

    public DistributionKeycloakServer(boolean debug, boolean reuse, long startTimeout) {
        this.debug = debug;
        this.reuse = reuse;
        this.startTimeout = startTimeout;
    }

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder, boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;

        List<String> args = keycloakServerConfigBuilder.toArgs();
        Set<Dependency> dependencies = keycloakServerConfigBuilder.toDependencies();

        if (!reuse) {
            killPreviousProcess();
        }

        try {
            boolean installationCreated = createInstallation();

            File providersDir = new File(keycloakHomeDir, "providers");
            List<File> existingProviders = listExistingProviders(providersDir);

            if (!installationCreated && reuse && ping()) {
                checkRunning();

                File startupArgsFile = getServerArgsFile();
                String startedWithArgs = startupArgsFile.isFile() ? FileUtils.readStringFromFile(startupArgsFile) : null;
                String requestedArgs = String.join(" ", args);

                Set<String> requestedDependencies = dependencies.stream().map(d -> d.getGroupId() + "__" + d.getArtifactId() + ".jar").collect(Collectors.toSet());
                Set<String> startedWithDependencies = existingProviders.stream().map(File::getName).collect(Collectors.toSet());

                if (requestedArgs.equals(startedWithArgs) && setEquals(requestedDependencies, startedWithDependencies)) {
                    log.trace("Re-using already running Keycloak");
                    return;
                } else {
                    if (killPreviousProcess()) {
                        log.trace("Killed existing Keycloak");
                    } else {
                        throw new RuntimeException("Running Keycloak not started with required arguments or providers, and could not kill the current process");
                    }
                }
            }

            updateProviders(existingProviders, dependencies, providersDir);

            OutputHandler outputHandler = startKeycloak(args);

            waitForStart(outputHandler);

            FileUtils.writeToFile(getServerArgsFile(), String.join(" ", args));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkRunning() {
        if (!Environment.isWindows()) {
            ProcessBuilder pb = new ProcessBuilder("fuser", "-n", "tcp", tlsEnabled ? "8443" : "8080");
            try {
                Process process = pb.start();
                process.waitFor(1, TimeUnit.SECONDS);
                String pid = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                String expectedPid = FileUtils.readStringFromFile(getPidFile());
                if (!pid.equals(expectedPid)) {
                    throw new RuntimeException("Process running on port is not a managed Keycloak server");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            ServerInfoRepresentation serverInfo;
            try {
                serverInfo = getServerInfo();
            } catch (Throwable t) {
                throw new RuntimeException("Non-managed Keycloak server or other process running on " + getBaseUrl());
            }
            File userDir = new File(serverInfo.getSystemInfo().getUserDir()).getParentFile();
            if (!userDir.equals(keycloakHomeDir)) {
                throw new RuntimeException("Non-managed Keycloak server running from " + userDir);
            }
        }
    }

    private @NotNull DistributionKeycloakServer.OutputHandler startKeycloak(List<String> args) {
        log.trace("Starting Keycloak");
        List<String> cmd = new LinkedList<>();
        if (Environment.isWindows()) {
            cmd.add(keycloakHomeDir.toPath().resolve("bin").resolve(CMD).toString());
        } else {
            cmd.add("./" + CMD);
        }
        cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(keycloakHomeDir, "bin"));

        if (debug) {
            pb.environment().put("DEBUG", "true");
        }

        OutputHandler outputHandler;
        try {
            keycloakProcess = pb.start();
            outputHandler = new OutputHandler(keycloakProcess);
            new Thread(outputHandler).start();

            ProcessHandle descendent = ProcessUtils.waitForDescendent(keycloakProcess);
            FileUtils.writeToFile(getPidFile(), descendent.pid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputHandler;
    }

    private static void updateProviders(List<File> existingProviders, Set<Dependency> dependencies, File providersDir) throws IOException {
        existingProviders.stream()
                .filter(f -> f.getName().endsWith(".jar"))
                .filter(f -> {
                    String fileName = f.getName();
                    String groupId = fileName.substring(0, fileName.indexOf("__"));
                    String artifactId = fileName.substring(fileName.indexOf("__") + 2, fileName.lastIndexOf(".jar"));
                    return dependencies.stream().noneMatch(d -> d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId));
                }).forEach(f -> {
                    log.trace("Deleted non-requested provider: " + f.getAbsolutePath());
                    FileUtils.delete(f);
                    FileUtils.delete(new File(f.getAbsolutePath() + ".lastModified"));
                });

        Path providersPath = providersDir.toPath();
        for (Dependency d : dependencies) {
            Path dependencyPath = Maven.resolveArtifact(d.getGroupId(), d.getArtifactId());
            File dependencyFile = dependencyPath.toFile();
            Path targetPath = providersPath.resolve(d.getGroupId() + "__" + d.getArtifactId() + ".jar");
            File targetFile = targetPath.toFile();
            File targetLastModified = new File(targetFile.getAbsolutePath() + ".lastModified");
            long lastModified = targetLastModified.isFile() ? FileUtils.readLongFromFile(targetLastModified) : -1;

            if (lastModified != dependencyPath.toFile().lastModified() || !targetFile.isFile()) {
                log.trace("Adding or overriding existing provider: " + targetPath.toFile().getAbsolutePath());
                Files.copy(dependencyPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(targetLastModified.toPath(), Long.toString(dependencyFile.lastModified()));
            }
        }
    }

    @Override
    public void stop() {
        if (!reuse) {
            ProcessUtils.killRunningProcess(keycloakProcess);

            File pidFile = getPidFile();
            if (pidFile.exists()) {
                FileUtils.delete(pidFile);
            }
        }
    }

    private boolean killPreviousProcess() {
        if (!Environment.isWindows()) {
            File pidFile = getPidFile();
            if (pidFile.exists()) {
                try {
                    String previousPid = FileUtils.readStringFromFile(pidFile);
                    if (ProcessUtils.killProcess(previousPid)) {
                        log.trace("Killed running managed Keycloak: " + previousPid);
                        FileUtils.delete(pidFile);
                        return true;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }

    @Override
    public String getBaseUrl() {
        if (tlsEnabled) {
            return "https://localhost:8443";
        } else {
            return "http://localhost:8080";
        }
    }

    @Override
    public String getManagementBaseUrl() {
        if (tlsEnabled) {
            return "https://localhost:9000";
        } else {
            return "http://localhost:9000";
        }
    }

    private boolean createInstallation() throws IOException {
        File dist = resolveKeycloakDist();

        if (INSTALL_DIR.isDirectory()) {
            File[] f = INSTALL_DIR.listFiles();
            if (f != null && f.length == 1) {
                long fromZipLastModified = FileUtils.readLongFromFile(getZipLastModifiedFile(f[0]));
                if (fromZipLastModified != dist.lastModified()) {
                    log.trace("Deleting installation from a previous distribution");
                    FileUtils.delete(INSTALL_DIR);
                } else {
                    log.trace("Re-using previous installation");
                    keycloakHomeDir = f[0];
                    return false;
                }
            }
        }

        if (INSTALL_DIR.isDirectory()) {
            FileUtils.delete(INSTALL_DIR);
        }
        if (!INSTALL_DIR.mkdirs()) {
            throw new IOException("Failed to create directory " + INSTALL_DIR);
        }

        ZipUtils.unzip(dist.toPath(), INSTALL_DIR.toPath());

        File[] files = INSTALL_DIR.listFiles();
        if (files == null || files.length != 1) {
            throw new RuntimeException("Expected " + INSTALL_DIR.getAbsolutePath() + " to contain a single directory");
        }
        keycloakHomeDir = files[0];

        if (!Path.of(keycloakHomeDir.getPath(), "bin", CMD).toFile().setExecutable(true)) {
            throw new RuntimeException("Failed to make startup script executable");
        }

        FileUtils.writeToFile(getZipLastModifiedFile(keycloakHomeDir), dist.lastModified());
        return true;
    }

    private boolean ping() {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(getBaseUrl()).openConnection();
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            if(urlConnection instanceof HttpsURLConnection httpsURLConnection) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] { new NullTrustManager() }, new SecureRandom());
                SSLSocketFactory socketFactory = sslContext.getSocketFactory();
                httpsURLConnection.setSSLSocketFactory(socketFactory);
            }
            urlConnection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForStart(OutputHandler outputHandler) {
        boolean started = outputHandler.waitForStarted();
        if (started && ping()) {
            return;
        }
        keycloakProcess.destroy();
        throw new RuntimeException("Keycloak did not start within timeout: " + getErrorOutput());
    }

    private File getZipLastModifiedFile(File dir) {
        return new File(dir, "zip-last-modified");
    }

    private File getPidFile() {
        return new File(keycloakHomeDir, "pid");
    }

    private File getServerArgsFile() {
        return new File(keycloakHomeDir, "startup-args");
    }

    private ServerInfoRepresentation getServerInfo() {
        KeycloakBuilder kcb = KeycloakBuilder.builder()
                .serverUrl(getBaseUrl())
                .realm("master")
                .clientId("temp-admin")
                .clientSecret("mysecret")
                .grantType("client_credentials");

        Keycloak kc = kcb.build();
        ServerInfoRepresentation info = kc.serverInfo().getInfo();
        kc.close();
        return info;
    }

    private String getErrorOutput() {
        try {
            return new String(keycloakProcess.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static File resolveKeycloakDist() {
        Path p = Path.of(System.getProperty("user.dir"));
        String dist = "quarkus/dist/target/" + "keycloak-" + Version.VERSION + ".zip";
        while (p.resolve("pom.xml").toFile().isFile()) {
            File zip = p.resolve(dist).toFile();
            if (zip.isFile()) {
                return zip;
            }
            p = p.getParent();
        }

        return Maven.resolveArtifact("org.keycloak", "keycloak-quarkus-dist").toFile();
    }

    private boolean setEquals(Set<String> a, Set<String> b) {
        return a.size() == b.size() && a.containsAll(b);
    }

    private List<File> listExistingProviders(File providersDir) {
        if (providersDir.isDirectory()) {
            File[] files = providersDir.listFiles(n -> n.getName().endsWith(".jar"));
            if (files != null) {
                return Arrays.stream(files).toList();
            }
        }
        return List.of();
    }

    private class OutputHandler implements Runnable {

        private static final Pattern LOG_PATTERN = Pattern.compile("([^ ]*) ([^ ]*) ([A-Z]*)([ ]*)(.*)");
        private static final Logger LOGGER = Logger.getLogger("managed.keycloak");

        private boolean startedInPrinted = false;
        private final Process process;

        private CountDownLatch startupLatch = new CountDownLatch(1);

        private OutputHandler(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            InputStream is = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            try {
                for (String line = br.readLine(); process.isAlive() && line != null; line = br.readLine()) {
                    if (!startedInPrinted && line.matches(".*Keycloak.* started in.*")) {
                        startupLatch.countDown();
                    }

                    Matcher matcher = LOG_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String levelString = matcher.group(3);
                        String message = matcher.group(5);
                        if (levelString != null && message != null) {
                            for (Logger.Level l : Logger.Level.values()) {
                                if (l.name().equals(levelString)) {
                                    LOGGER.log(l, message);
                                    break;
                                }
                            }
                        }
                    }
                    LOGGER.info(line);
                }
            } catch (IOException e) {
                // Ignored
            }
        }

        public boolean waitForStarted() {
            try {
                startupLatch.await(startTimeout, TimeUnit.SECONDS);
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

    }

    private static class NullTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
