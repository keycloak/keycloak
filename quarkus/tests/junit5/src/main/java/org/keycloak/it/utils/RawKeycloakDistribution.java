/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.keycloak.common.Version;
import org.keycloak.it.TestProvider;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.KeycloakMain;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.fs.util.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import static org.keycloak.quarkus.runtime.Environment.KC_CONFIG_BUILT;
import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE;
import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE_EXIT_BEFORE_BOOTSTRAP;
import static org.keycloak.quarkus.runtime.Environment.isWindows;

public final class RawKeycloakDistribution implements KeycloakDistribution {

    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;

    private static final Logger LOG = Logger.getLogger(RawKeycloakDistribution.class);
    private Process keycloak;
    private int exitCode = -1;
    private Path distPath;
    private ExecutorService outputExecutor;
    private final Map<String, String> envVars = new HashMap<>();
    private final DefaultOutputConsumer outputConsumer;

    public RawKeycloakDistribution(boolean reCreate) {
        this.distPath = prepareDistribution(reCreate);
        if (reCreate) {
            try {
                preInitH2(distPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        this.outputConsumer = new DefaultOutputConsumer();
    }
    
    @Override
    public int getMappedPort(int port) {
        return port;
    }
    
    @Override
    public synchronized void waitFor(boolean ready, long timeoutMillis) {
        if (!isRunning()) {
            return;
        }
        try {
            if (ready) {
                this.outputConsumer.running.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } else {
                this.keycloak.onExit().get(timeoutMillis, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException|TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsDebug() {
        return true;
    }

    public CLIResult kc(String... arguments) throws IOException {
        return kc(Arrays.asList(arguments));
    }

    public CLIResult kc(List<String> arguments) throws IOException {
        return invoke(SCRIPT_CMD, arguments);
    }

    public CLIResult kcadm(String... arguments) throws IOException {
    	return kcadm(Arrays.asList(arguments));
    }

    public CLIResult kcadm(List<String> arguments) throws IOException {
        return invoke(SCRIPT_KCADM_CMD, arguments);
    }

    private CLIResult invoke(String script, List<String> arguments) throws IOException {
        List<String> allArgs = new ArrayList<>();

        addPlatformSpecificCommand(allArgs, script);

        allArgs.addAll(arguments);

        ProcessBuilder pb = new ProcessBuilder(allArgs);
        ProcessBuilder builder = pb.directory(distPath.resolve("bin").toFile());

        addAOTEnvVars();
        
        builder.environment().putAll(envVars);

        Process proc = builder.start();

        DefaultOutputConsumer outputConsumer = new DefaultOutputConsumer();
        readOutput(proc, outputConsumer);

        int exitValue = proc.exitValue();

        return CLIResult.create(outputConsumer.getStdOut(), outputConsumer.getErrOut(), exitValue);
    }

	private void addPlatformSpecificCommand(List<String> allArgs, String cmd) {
		if (isWindows()) {
            allArgs.add(distPath.resolve("bin") + File.separator + cmd);
        } else {
            allArgs.add("./" + cmd);
        }
	}
	
	@Override
	public void runKc(List<String> arguments) {
        if (isRunning()) {
            throw new IllegalStateException("Stop has not been called");
        }
        resetForNextRun();
        try {
            startServer(arguments);
            asyncReadOutput();
        } catch (Exception cause) {
            try {
                stop();
            } catch (Exception stopException) {
                cause.addSuppressed(stopException);
            }
            throw new RuntimeException("Failed to start the server", cause);
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            try {
                // On Windows, we need to make sure sub-processes are terminated first
                destroyDescendantsOnWindows(keycloak, false);

                keycloak.destroy();
                keycloak.waitFor(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception cause) {
                destroyDescendantsOnWindows(keycloak, true);
                keycloak.destroyForcibly();
                threadDump();
                throw new RuntimeException("Failed to stop the server", cause);
            }
        }

        if (keycloak != null) {
            exitCode = keycloak.exitValue();
        }

        shutdownOutputExecutor();
    }

    private void destroyDescendantsOnWindows(Process parent, boolean force) {
        if (!isWindows()) {
            return;
        }

        List<ProcessHandle> descendants = parent.descendants().toList();
        if (descendants.isEmpty()) {
            return;
        }

        LOG.debugf("Found %d descendant processes to terminate", descendants.size());
        CompletableFuture<?> allProcesses = CompletableFuture.completedFuture(null);

        // Terminate all processes
        for (ProcessHandle process : descendants) {
            if (force) {
                LOG.warn("Using forcible termination of descendant processes after normal termination failed");
                LOG.debugf("Forcibly terminating process %s", process.pid());
                process.destroyForcibly();
            } else {
                process.destroy();
            }

            allProcesses = CompletableFuture.allOf(allProcesses, process.onExit());
        }

        // Wait for all processes to exit according to Java
        try {
            allProcesses.get(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            LOG.debugf("All descendant processes terminated according to Java");
        } catch (Exception cause) {
            throw new RuntimeException("Failed to terminate descendants processes", cause);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<String> getOutputStream() {
        return outputConsumer.getStdOut();
    }

    @Override
    public List<String> getErrorStream() {
        return outputConsumer.getErrOut();
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void threadDump() {
        if (Environment.isWindows()) {
            return;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder("kill", "-3", String.valueOf(keycloak.pid()));
            Process p = builder.start();
            p.onExit().get(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.warn("A thread dump may not have been successfully triggered", e);
            return;
        }
        Awaitility.await().atMost(1, TimeUnit.MINUTES)
                .until(() -> getOutputStream().stream().anyMatch(s -> s.contains("JNI global refs")));
    }

    public boolean isRunning() {
        return keycloak != null && keycloak.isAlive();
    }

    private void asyncReadOutput() {
        shutdownOutputExecutor();
        outputExecutor = Executors.newSingleThreadExecutor();
        outputExecutor.execute(this::readOutput);
    }

    private void shutdownOutputExecutor() {
        if (outputExecutor != null) {
            outputExecutor.shutdown();
            try {
                outputExecutor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException cause) {
                throw new RuntimeException("Failed to terminate output executor", cause);
            } finally {
                outputExecutor = null;
            }
        }
    }

    private void resetForNextRun() {
        outputConsumer.reset();
        exitCode = -1;
        shutdownOutputExecutor();
        keycloak = null;
    }

    private Path inDistZipDirectory(File distFile) throws Exception{

        try (ZipFile zipFile = new ZipFile(distFile)) {
            Optional<? extends ZipEntry> e = zipFile.stream().filter(ZipEntry::isDirectory).findFirst();
            if (e.isPresent()) {
                String dirName = e.get().getName();
                if (dirName.contains("..")) {
                    throw new RuntimeException("inside zip distribution directory cannot contain relative paths: " + dirName);
                }
                return Path.of(dirName);
            }
        };
        throw new RuntimeException(String.format("ZIP file '%s' doesn't contain any directories", distPath));
    }

    public Path prepareDistribution(boolean reCreate) {
        try {
            Path distRootPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve("kc-tests");
            distRootPath.toFile().mkdirs();

            File distFile;
            if (System.getProperty("product.dist.zip") != null) {
                distFile = new File(System.getProperty("product.dist.zip"));
            } else {
                distFile = new File("../../dist/" + File.separator + "target" + File.separator + "keycloak-" + Version.VERSION + ".zip");
            }

            if (!distFile.exists()) {
                distFile = Maven.resolveArtifact("org.keycloak", "keycloak-quarkus-dist").toFile();
            }

            Path dPath = distRootPath.resolve(inDistZipDirectory(distFile));

            if (reCreate || !dPath.toFile().exists()) {
                FileUtil.deleteDirectory(dPath);
                ZipUtils.unzip(distFile.toPath(), distRootPath);
                FileUtils.copyDirectory(dPath.resolve("conf").toFile(), dPath.resolve("conf-bak").toFile());
                FileUtils.copyDirectory(dPath.resolve("lib").resolve("quarkus").toFile(), dPath.resolve("quarkus-bak").toFile());
                postInit(dPath);
            }

            // make sure script is executable
            if (!dPath.resolve("bin").resolve(SCRIPT_CMD).toFile().setExecutable(true)) {
                throw new RuntimeException("Cannot set " + SCRIPT_CMD + " executable");
            }
            if (!dPath.resolve("bin").resolve(SCRIPT_KCADM_CMD).toFile().setExecutable(true)) {
                throw new RuntimeException("Cannot set " + SCRIPT_KCADM_CMD + " executable");
            }

            return dPath;
        } catch (Exception cause) {
            throw new RuntimeException("Failed to prepare distribution", cause);
        }
    }

    private void preInitH2(Path dPath) throws IOException {
        ProcessHandle.current().info().command().ifPresent(command -> {
            boolean useAot = false;
            if (Boolean.getBoolean("kc.quarkus.tests.aot")) {
                if (Runtime.version().feature() < 25) {
                    throw new AssertionError("AOT requested, but the java version is less than 25");
                }
                useAot = true;
            
                if (envVars.containsKey("JAVA_OPTS_APPEND")) {
                    throw new AssertionError("the raw dist marked as recreate should not have JAVA_OPTS_APPEND set");
                }
                File aotFile = getAotFile(dPath);
                // TODO: we know for certain that the test java is 25+, so we'll set it here 
                envVars.put("JAVA", command);
                envVars.put("JAVA_OPTS_APPEND", "-Xlog:aot -XX:AOTCacheOutput=\"%s\"".formatted(aotFile.getAbsolutePath()));
            }
            
            LOG.infof("Creating pre-initialized database %sfor reuse", useAot ? "and aot cache ":"");
            
            try {
                CLIResult result = invoke(SCRIPT_CMD, List.of("start-dev", "-D%s=%s".formatted(LAUNCH_MODE, LAUNCH_MODE_EXIT_BEFORE_BOOTSTRAP), "-D%s=true".formatted(KC_CONFIG_BUILT)));
                if (result.exitCode() != 0) {
                    throw new RuntimeException("Could not create pre-initialized db, exit code %s: %s".formatted(result.exitCode(), new String(result.getErrorOutput())));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        FileUtils.copyDirectory(dPath.resolve("data").resolve("h2").toFile(), dPath.resolve("h2-bak").toFile());
    }

    private File getAotFile(Path dPath) {
        return dPath.resolve("app.aot").toFile();
    }

    private void postInit(Path dPath) {
        if (System.getProperty("product") != null) {
            // JDBC drivers might be excluded if running as a product build
            copyProvider(dPath, "com.microsoft.sqlserver", "mssql-jdbc");
        }
    }

    private void readOutput() {
        readOutput(keycloak, outputConsumer);
    }

    private void readOutput(Process process, OutputConsumer outputConsumer) {
        try (
                BufferedReader outStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errStream = new BufferedReader(new InputStreamReader(process.getErrorStream()))
        ) {
            while (process.isAlive()) {
                readStream(outStream, outputConsumer, false);
                readStream(errStream, outputConsumer, true);
                // a hint to temporarily disable the current thread in favor of the process where the distribution is running
                // after some tests it shows effective to help starting the server faster
                LockSupport.parkNanos(1L);
            }
        } catch (Throwable cause) {
            throw new RuntimeException("Failed to read server output", cause);
        }
    }

    private void readStream(BufferedReader reader, OutputConsumer outputConsumer, boolean error) throws IOException {
        String line;

        while (reader.ready() && (line = reader.readLine()) != null) {
            if (error) {
                outputConsumer.onErrOut(line);
            } else {
                outputConsumer.onStdOut(line);
            }
        }
    }

    /**
     * The server is configured to redirect errors to output stream. This adds a limitation when checking whether a
     * message arrived via error stream.
     *
     * @param arguments the list of arguments to run the server
     * @throws Exception if something bad happens
     */
    private void startServer(List<String> arguments) throws Exception {
        List<String> allArgs = new ArrayList<>();

        addPlatformSpecificCommand(allArgs, SCRIPT_CMD);

        // used to detect readiness rather than http(s) probing
        allArgs.add("-D%s=true".formatted(KeycloakMain.KC_SERVER_PRINT_RUNNING));
        
        allArgs.addAll(arguments);

        ProcessBuilder pb = new ProcessBuilder(allArgs);
        ProcessBuilder builder = pb.directory(distPath.resolve("bin").toFile());

        addAOTEnvVars();

        builder.environment().putAll(envVars);

        keycloak = builder.start();
        var future = outputConsumer.running;
        keycloak.onExit().whenComplete((p, t) -> future.complete(null));
    }

    private void addAOTEnvVars() {
        File aotFile = getAotFile(getDistPath());
        if (aotFile.exists() && !envVars.containsKey("JAVA_OPTS_APPEND")) {
            // TODO: we know for certain that the test java is 25+, so we'll set it here 
            envVars.put("JAVA", ProcessHandle.current().info().command().orElseThrow());
            envVars.put("JAVA_OPTS_APPEND", "-XX:AOTCache=\"%s\"".formatted(aotFile.getAbsolutePath()));
        }
    }

    public void setProperty(String key, String value) {
        updateProperties(properties -> properties.put(key, value), distPath.resolve("conf").resolve("keycloak.conf").toFile());
    }

    @Override
    public void setEnvVar(String name, String value) {
        this.envVars.put(name, value);
    }

    public void removeProperty(String name) {
        updateProperties(properties -> properties.remove(name), distPath.resolve("conf").resolve("keycloak.conf").toFile());
    }

    public void setQuarkusProperty(String key, String value) {
        updateProperties(properties -> properties.put(key, value), getQuarkusPropertiesFile());
    }

    public void deleteQuarkusProperties() {
        File file = getQuarkusPropertiesFile();

        if (file.exists()) {
            file.delete();
        }
    }

    public void copyOrReplaceFileFromClasspath(String file, Path targetFile) {
        Path path = distPath.resolve(targetFile);

        path.getParent().toFile().mkdirs();

        try {
            Files.copy(getClass().getResourceAsStream(file), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to copy file", cause);
        }
    }

    public void copyOrReplaceFile(Path file, Path targetFile) {
        if (!file.toFile().exists()) {
            return;
        }

        Path path = distPath.resolve(targetFile);

        path.getParent().toFile().mkdirs();

        try {
            Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to copy file", cause);
        }
    }

    @Override
    public void copyProvider(String groupId, String artifactId) {
        copyProvider(getDistPath(), groupId, artifactId);
    }

    private static void copyProvider(Path distPath, String groupId, String artifactId) {
        try {
            Path providerPath = Maven.resolveArtifact(groupId, artifactId);
            if (!Files.isRegularFile(providerPath)) {
                throw new RuntimeException("Failed to copy JAR file to 'providers' directory; " + providerPath + " is not a file");
            }

            Files.copy(providerPath, distPath.resolve("providers").resolve(artifactId + ".jar"));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to copy JAR file to 'providers' directory", cause);
        }
    }

    @Override
    public void copyConfigFile(Path configFilePath) {
        try {
            Files.copy(configFilePath, distPath.resolve("conf").resolve(configFilePath.getFileName()));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to copy config file [" + configFilePath + "] to 'conf' directory", cause);
        }
    }

    private void updateProperties(Consumer<Properties> propertiesConsumer, File propertiesFile) {
        Properties properties = new Properties();

        if (propertiesFile.exists()) {
            try (
                FileInputStream in = new FileInputStream(propertiesFile)
            ) {

                properties.load(in);
            } catch (Exception e) {
                throw new RuntimeException("Failed to update " + propertiesFile, e);
            }
        }

        try (
            FileOutputStream out = new FileOutputStream(propertiesFile)
        ) {
            propertiesConsumer.accept(properties);
            properties.store(out, "");
        } catch (Exception e) {
            throw new RuntimeException("Failed to update " + propertiesFile, e);
        }
    }

    private File getQuarkusPropertiesFile() {
        return distPath.resolve("conf").resolve("quarkus.properties").toFile();
    }

    public Path getDistPath() {
        return distPath;
    }

    public void copyProvider(TestProvider provider) {
        URL pathUrl = provider.getClass().getResource(".");
        File fileUri;
        try {
            fileUri = new File(pathUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid package provider path", e);
        }
        Path providerPackagePath = Paths.get(fileUri.getPath());
        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, provider.getName() + ".jar")
                .addClasses(provider.getClasses())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        Map<String, String> manifestResources = provider.getManifestResources();

        for (Map.Entry<String, String> resource : manifestResources.entrySet()) {
            try {
                providerJar.addAsManifestResource(providerPackagePath.resolve(resource.getKey()).toFile(), resource.getValue());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to add manifest resource: " + resource.getKey(), cause);
            }
        }

        copyOrReplaceFile(providerPackagePath.resolve("quarkus.properties"), Path.of("conf", "quarkus.properties"));

        providerJar.as(ZipExporter.class).exportTo(getDistPath().resolve("providers").resolve(providerJar.getName()).toFile());
    }

    @Override
    public void clearEnv() {
        this.envVars.clear();
    }

    private static final class DefaultOutputConsumer implements OutputConsumer {

        private final List<String> stdOut = Collections.synchronizedList(new ArrayList<>());
        private final List<String> errOut = Collections.synchronizedList(new ArrayList<>());
        private CompletableFuture<Void> running = new CompletableFuture<Void>();
        
        @Override
        public void onStdOut(String line) {
            if (line.equals(KeycloakMain.RUNNING_MESSAGE)) {
                running.complete(null);
            }
            System.out.println(line);
            stdOut.add(line);
        }

        @Override
        public void onErrOut(String line) {
            System.err.println(line);
            errOut.add(line);
        }

        @Override
        public void reset() {
            stdOut.clear();
            errOut.clear();
            this.running = new CompletableFuture<>();
        }

        @Override
        public List<String> getErrOut() {
            return errOut;
        }

        @Override
        public List<String> getStdOut() {
            return stdOut;
        }
        
    }
    
    public void resetH2Dir() throws IOException {
        FileUtil.deleteDirectory(getDistPath().resolve("data").resolve("h2"));
        FileUtils.copyDirectory(getDistPath().resolve("h2-bak").toFile(), getDistPath().resolve("data").resolve("h2").toFile());
    }
    
    /**
     * Reset the distribution back to its install state.
     * @param resetAugmentation if true the lib/quarkus directory will be reset
     */
    public void reset(boolean resetAugmentation) throws IOException {
        LOG.infof("Resetting the distribution for the next test%s %s", resetAugmentation ? " including augmentation" : "", distPath);
        FileUtil.deleteDirectory(getDistPath().resolve("conf"));
        FileUtils.copyDirectory(getDistPath().resolve("conf-bak").toFile(), getDistPath().resolve("conf").toFile());
        FileUtil.deleteDirectory(getDistPath().resolve("providers"));
        getDistPath().resolve("providers").toFile().mkdirs();
        FileUtil.deleteDirectory(getDistPath().resolve("data"));
        getDistPath().resolve("data").toFile().mkdirs();
        resetH2Dir();
        if (resetAugmentation) {
            FileUtil.deleteDirectory(getDistPath().resolve("lib").resolve("quarkus"));
            FileUtils.copyDirectory(getDistPath().resolve("quarkus-bak").toFile(), getDistPath().resolve("lib").resolve("quarkus").toFile());
        }
        postInit(getDistPath());
    }

    public void setDistPath(Path newPath) throws IOException {
        FileUtils.moveDirectory(distPath.toFile(), newPath.toFile());
        this.distPath = newPath;
    }
}
