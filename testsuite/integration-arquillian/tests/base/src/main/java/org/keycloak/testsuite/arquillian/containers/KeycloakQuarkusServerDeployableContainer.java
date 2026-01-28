package org.keycloak.testsuite.arquillian.containers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.keycloak.testsuite.model.StoreProvider;
import org.keycloak.testsuite.util.WaitUtils;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.logging.Logger;

/**
 * @author mhajas
 */
public class KeycloakQuarkusServerDeployableContainer extends AbstractQuarkusDeployableContainer implements RemoteContainer {

    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;

    private static final Logger log = Logger.getLogger(KeycloakQuarkusServerDeployableContainer.class);

    private Process container;
    private Thread stdoutForwarderThread;
    private LogProcessor logProcessor;

    @Override
    public void start() throws LifecycleException {
        try {
            importRealm();
            container = startContainer();
            logProcessor = new LogProcessor(new BufferedReader(new InputStreamReader(container.getInputStream())));
            stdoutForwarderThread = new Thread(logProcessor);
            stdoutForwarderThread.start();

            try {
                waitForReadiness();
            } catch (Exception e) {
                if (logProcessor.containsBuildTimeOptionsError()) {
                    log.warn("The build time options have values that differ from what is persisted. Restarting container...");
                    container.destroy();
                    container = startContainer();
                    logProcessor = new LogProcessor(new BufferedReader(new InputStreamReader(container.getInputStream())));
                    stdoutForwarderThread = new Thread(logProcessor);
                    stdoutForwarderThread.start();
                    waitForReadiness();
                } else {
                    throw e;
                }
            }
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

    private void executeCommand(File wrkDir, String command, String... args) throws IOException {
        final List<String> commands = new ArrayList<>();
        commands.add(getCommand());
        commands.add("-v");
        commands.add(command);
        addFeaturesOption(commands);
        if (args != null) {
            commands.addAll(Arrays.asList(args));
        }
        log.debugf("Non-server process arguments: %s", commands);
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process p = pb.directory(wrkDir).inheritIO().start();
        try {
            if (!p.waitFor(300, TimeUnit.SECONDS)) {
                throw new IOException("Command " + command + " did not finished in 300 seconds");
            }
            if (p.exitValue() != 0) {
                throw new IOException("Command " + command + " was executed with exit status " + p.exitValue());
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void importRealm() throws IOException, URISyntaxException {
        if (suiteContext.get().isAuthServerMigrationEnabled() && configuration.getImportFile() != null) {
            final String importFileName = configuration.getImportFile();

            log.infof("Importing realm from file '%s'", importFileName);

            final URL url = getClass().getResource("/migration-test/" + importFileName);
            if (url == null) {
                throw new IllegalArgumentException("Cannot find migration import file");
            }

            final Path path = Paths.get(url.toURI());
            final File wrkDir = configuration.getProvidersPath().resolve("bin").toFile();

            Path keycloakConf = Paths.get(wrkDir.toURI()).getParent().resolve("conf").resolve("keycloak.conf");

            // there are several issues with import in initial quarkus versions, so better use the keycloak.conf file
            StoreProvider storeProvider = StoreProvider.getCurrentProvider();
            List<String> storageOptions = storeProvider.getStoreOptionsToKeycloakConfImport();
            Path keycloakConfBkp = null;
            try {
                if (!storageOptions.isEmpty()) {
                    keycloakConfBkp = keycloakConf.getParent().resolve("keycloak.conf.bkp");
                    Files.copy(keycloakConf, keycloakConfBkp);
                    // write the options to the file
                    try ( BufferedWriter w = new BufferedWriter(new FileWriter(keycloakConf.toFile(), true))) {
                        for (String s : storageOptions) {
                            w.write(System.lineSeparator());
                            w.write(s);
                        }
                    }

                    // execute build command to set the storage options if needed
                    executeCommand(wrkDir, "build");
                }

                // execute the import
                executeCommand(wrkDir, "import", "--file=" + wrkDir.toPath().relativize(path));
            } finally {
                // restore initial keycloak.conf if modified for import
                if (keycloakConfBkp != null && Files.exists(keycloakConfBkp)) {
                    Files.move(keycloakConfBkp, keycloakConf, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Process startContainer() throws IOException {
        ProcessBuilder pb = getProcessBuilder();
        File wrkDir = configuration.getProvidersPath().resolve("bin").toFile();
        ProcessBuilder builder = pb.directory(wrkDir).redirectErrorStream(true);

        String javaOpts = configuration.getJavaOpts();

        if (javaOpts != null) {
            builder.environment().put("JAVA_OPTS", javaOpts);
        }

        if (restart.compareAndSet(false, true)) {
            deleteDirectory(configuration.getProvidersPath().resolve("data"));
        }

        return builder.start();
    }

    @Override
    protected List<String> configureArgs(List<String> args) {
        List<String> commands = new ArrayList<>(args);

        commands.add(0, getCommand());

        return commands;
    }

    private ProcessBuilder getProcessBuilder() {
        Map<String, String> env = new HashMap<>();
        String[] processCommands = getArgs(env).toArray(new String[0]);
        log.debugf("Quarkus process arguments: %s", Arrays.asList(processCommands));
        ProcessBuilder pb = new ProcessBuilder(processCommands);
        pb.environment().putAll(env);

        return pb;
    }

    private String getCommand() {
        if (isWindows()) {
            return configuration.getProvidersPath().resolve("bin").resolve("kc.bat").toString();
        }
        return "./kc.sh";
    }

    private void destroyDescendantsOnWindows(Process parent, boolean force) {
        if (!isWindows()) {
            return;
        }

        // Wait some time before killing the windows processes. Otherwise there is a risk that some already commited H2 transactions
        // won't be written to disk in time and hence those transactions may be lost, which could result in test failures in the next step after server restart.
        // See http://repository.transtep.com/repository/thirdparty/H2/1.0.63/docs/html/advanced.html#durability_problems for the details
        WaitUtils.pause(2000);

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

    public static void deleteDirectory(final Path directory) throws IOException {
        if (Files.isDirectory(directory, new LinkOption[0])) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(file);
                    } catch (IOException var4) {
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
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

    @Override
    protected void checkLiveness() {
        if (!container.isAlive()) {
            throw new IllegalStateException("Keycloak unexpectedly died :(");
        }
    }

    @Override
    public String getRemoteLog() {
        return logProcessor.getBufferedLog();
    }

    private static class LogProcessor implements Runnable {
        public static final int MAX_LOGGED_LINES = 512;
        private List<String> loggedLines = new ArrayList<>();
        private BufferedReader inputReader;

        public LogProcessor(BufferedReader inputReader) {
            this.inputReader = inputReader;
        }

        @Override
        public void run() {
            String line;
            try {
                while ((line = inputReader.readLine()) != null) {
                    System.out.println(line);

                    loggedLines.add(line);
                    if (loggedLines.size() > MAX_LOGGED_LINES) {
                        loggedLines.remove(0);
                    }
                }
            } catch (IOException e) {
                if ("Stream closed".equals(e.getMessage())) {
                    System.out.println("Log has ended");
                } else {
                    throw new RuntimeException(e);
                }
            }
        }

        public String getBufferedLog() {
            return String.join("\n", loggedLines);
        }

        public boolean containsBuildTimeOptionsError() {
            return loggedLines.stream().anyMatch(line -> line.contains("The following build time options have values that differ from what is persisted"));
        }
    }
}
