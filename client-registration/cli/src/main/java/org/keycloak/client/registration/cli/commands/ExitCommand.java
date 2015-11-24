package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.Context;

import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@CommandDefinition(name="exit", description = "Exit the program")
public class ExitCommand implements Command {

    private Context context;

    public ExitCommand(Context context) {
        this.context = context;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
        commandInvocation.stop();
        return CommandResult.SUCCESS;
    }

}
