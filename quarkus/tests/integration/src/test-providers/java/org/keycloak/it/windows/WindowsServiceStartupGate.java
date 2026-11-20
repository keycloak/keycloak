package org.keycloak.it.windows;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class WindowsServiceStartupGate {
    private FileChannel resource;
    private Path data;

    void holdStartup(@Observes @Priority(1) StartupEvent event) throws Exception {
        data = Path.of(System.getProperty("kc.home.dir"), "data");
        Path gate = data.resolve("windows-service-startup.gate");
        if (!Files.exists(gate)) {
            return;
        }
        resource = FileChannel.open(data.resolve("windows-service-startup.resource"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        Files.writeString(data.resolve("windows-service-startup.entered"), "startup held");
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(5);
        while (Files.exists(gate)) {
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("Windows service startup gate was not released");
            }
            Thread.sleep(50);
        }
    }

    @PreDestroy
    void close() throws IOException {
        if (resource != null) {
            resource.close();
            Files.writeString(data.resolve("windows-service-startup.closed"), "resource closed");
        }
    }
}
