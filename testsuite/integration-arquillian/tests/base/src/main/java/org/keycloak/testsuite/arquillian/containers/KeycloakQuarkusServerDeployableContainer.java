package org.keycloak.testsuite.arquillian.containers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.exec.StreamPumper;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.model.StoreProvider;

/**
 * @author mhajas
 */
public class KeycloakQuarkusServerDeployableContainer extends AbstractQuarkusDeployableContainer {

    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;

    private static final Logger log = Logger.getLogger(KeycloakQuarkusServerDeployableContainer.class);

    private Process container;
    private Thread stdoutForwarderThread;

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
        ProcessBuilder pb = getProcessBuilder();
        File wrkDir = configuration.getProvidersPath().resolve("bin").toFile();
        ProcessBuilder builder = pb.directory(wrkDir).redirectErrorStream(true);

        String javaOpts = configuration.getJavaOpts();

        if (javaOpts != null) {
            builder.environment().put("JAVA_OPTS", javaOpts);
        }

        final StoreProvider storeProvider = StoreProvider.getCurrentProvider();
        final boolean isJpaStore = storeProvider.equals(StoreProvider.JPA) || storeProvider.equals(StoreProvider.LEGACY);

        if (!isJpaStore) {
            builder.environment().put("KEYCLOAK_ADMIN", "admin");
            builder.environment().put("KEYCLOAK_ADMIN_PASSWORD", "admin");
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
        commands.add("--optimized");

        log.debugf("Quarkus parameters: %s", commands);

        return commands;
    }

    private ProcessBuilder getProcessBuilder() {
        Map<String, String> env = new HashMap<>();
        String[] processCommands = getArgs(env).toArray(new String[0]);
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
