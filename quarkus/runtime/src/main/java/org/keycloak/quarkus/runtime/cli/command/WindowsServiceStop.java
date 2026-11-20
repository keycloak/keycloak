package org.keycloak.quarkus.runtime.cli.command;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.WindowsServiceLifecycle;

import picocli.CommandLine.Command;

/** Internal stop image, invoked by Procrun under the service account. */
@Command(name = WindowsServiceStop.NAME, hidden = true)
public class WindowsServiceStop extends AbstractCommand {

    public static final String NAME = "stop";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isHelpAll() {
        return false;
    }

    @Override
    protected void runCommand() {
        if (!Environment.isWindows()) {
            executionError(spec.commandLine(), "Windows service management is only available on Windows.");
        }
        String serviceId = System.getenv(WindowsServiceLifecycle.SERVICE_ID_ENV);
        if (serviceId == null) {
            executionError(spec.commandLine(), "This command must be invoked by the Windows service wrapper.");
        }
        try {
            WindowsServiceLifecycle.requestStopWhenReady(serviceId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executionError(spec.commandLine(), "Interrupted while waiting for the Windows service shutdown event.", e);
        }
    }
}
