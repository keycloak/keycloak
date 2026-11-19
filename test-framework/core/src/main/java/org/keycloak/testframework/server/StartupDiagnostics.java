package org.keycloak.testframework.server;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.keycloak.quarkus.runtime.Environment;

/**
 * Collects information about a managed Keycloak server that failed to start, so that the cause of a hanging or slow
 * startup can be analyzed from the CI logs alone.
 */
final class StartupDiagnostics {

    private static final int THREAD_DUMP_TIMEOUT_SECONDS = 20;

    private StartupDiagnostics() {
    }

    static String collect(Process process, List<String> output, String errorOutput) {
        StringBuilder sb = new StringBuilder("Keycloak server failed to start, diagnostics:\n");

        sb.append("Process alive: ").append(process.isAlive()).append('\n');
        if (!process.isAlive()) {
            sb.append("Exit code: ").append(process.exitValue()).append('\n');
        }

        List<ProcessHandle> processes = Stream.concat(
                Stream.of(process.toHandle()), process.descendants()).toList();
        sb.append("Processes:\n");
        processes.forEach(ph -> sb.append("  ").append(ph.pid()).append(' ')
                .append(ph.info().commandLine().or(() -> ph.info().command()).orElse("<unknown>"))
                .append(" alive=").append(ph.isAlive()).append('\n'));

        sb.append("Server output (").append(output.size()).append(" lines):\n");
        output.forEach(l -> sb.append("  ").append(l).append('\n'));

        if (!errorOutput.isBlank()) {
            sb.append("Server stderr:\n").append(errorOutput).append('\n');
        }

        processes.stream()
                .filter(ProcessHandle::isAlive)
                .filter(ph -> ph.info().command().map(c -> c.toLowerCase().contains("java")).orElse(false))
                .forEach(ph -> sb.append("Thread dump of ").append(ph.pid()).append(":\n").append(threadDump(ph.pid())).append('\n'));

        return sb.toString();
    }

    // jcmd works on all platforms, in contrast to a SIGQUIT which is not available on Windows
    private static String threadDump(long pid) {
        File jcmd = new File(System.getProperty("java.home"), "bin/jcmd" + (Environment.isWindows() ? ".exe" : ""));
        try {
            // Redirect to a file so that the timeout is honored even if jcmd hangs
            Path out = Files.createTempFile("kc-thread-dump-", ".txt");
            try {
                Process p = new ProcessBuilder(jcmd.getPath(), String.valueOf(pid), "Thread.print")
                        .redirectErrorStream(true).redirectOutput(out.toFile()).start();
                boolean finished = p.waitFor(THREAD_DUMP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    p.destroyForcibly();
                }
                return Files.readString(out, StandardCharsets.UTF_8) + (finished ? "" : "\n<thread dump timed out>");
            } finally {
                Files.deleteIfExists(out);
            }
        } catch (Exception e) {
            return "<thread dump failed: " + e + ">";
        }
    }

}
