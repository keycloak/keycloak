package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.keycloak.client.registration.cli.aesh.Globals;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractGlobalOptionsCmd implements Command {

    @Option(shortName = 'x', description = "Print full stack trace when exiting with error", hasValue = false)
    protected boolean dumpTrace;

    protected void init(AbstractGlobalOptionsCmd parent) {
        dumpTrace = parent.dumpTrace;
    }

    protected void processGlobalOptions() {
        Globals.dumpTrace = dumpTrace;
    }
}
