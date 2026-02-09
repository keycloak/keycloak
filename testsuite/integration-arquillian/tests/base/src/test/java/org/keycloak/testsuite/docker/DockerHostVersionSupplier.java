package org.keycloak.testsuite.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerHostVersionSupplier implements Supplier<Optional<DockerVersion>> {
    private static final Logger log = LoggerFactory.getLogger(DockerHostVersionSupplier.class);

    @Override
    public Optional<DockerVersion> get() {
        try {
            Process process = new ProcessBuilder()
                    .command("docker", "version", "--format", "'{{.Client.Version}}'")
                    .start();

            final BufferedReader stdout = getReader(process, Process::getInputStream);
            final BufferedReader err = getReader(process, Process::getErrorStream);

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                final String versionString = stdout.lines().collect(Collectors.joining()).replaceAll("'", "");
                return Optional.ofNullable(DockerVersion.parseVersionString(versionString));
            }
        } catch (IOException | InterruptedException e) {
            log.error("Could not determine host machine's docker version: ", e);
        }

        return Optional.empty();
    }

    private static BufferedReader getReader(final Process process, final Function<Process, InputStream> streamSelector) {
        return new BufferedReader(new InputStreamReader(streamSelector.apply(process)));
    }
}
