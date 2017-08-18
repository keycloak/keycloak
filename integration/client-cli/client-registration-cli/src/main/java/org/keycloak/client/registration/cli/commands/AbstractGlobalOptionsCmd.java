package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.keycloak.client.registration.cli.aesh.Globals;

import static org.keycloak.client.registration.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractGlobalOptionsCmd implements Command {

    @Option(shortName = 'x', description = "Print full stack trace when exiting with error", hasValue = false)
    protected boolean dumpTrace;

    @Option(name = "help", description = "Print command specific help", hasValue = false)
    protected boolean help;

    protected void initFromParent(AbstractGlobalOptionsCmd parent) {
        dumpTrace = parent.dumpTrace;
        help = parent.help;
    }

    protected void processGlobalOptions() {
        Globals.dumpTrace = dumpTrace;
    }

    protected boolean printHelp() {
        if (help || nothingToDo()) {
            printOut(help());
            return true;
        }

        return false;
    }

    protected boolean nothingToDo() {
        return false;
    }

    protected String help() {
        return KcRegCmd.usage();
    }
}
