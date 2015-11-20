package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.io.Resource;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.cli.Context;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@CommandDefinition(name="setup", description = "")
public class SetupCommand implements Command {

    @Option(shortName = 'h', hasValue = false, description = "display this help and exit")
    private boolean help;

    private Context context;

    public SetupCommand(Context context) {
        this.context = context;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
        System.out.println(help);

        if(help) {
            commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("create"));
        }

        return CommandResult.SUCCESS;
    }


    private String promptForUsername(CommandInvocation invocation) throws InterruptedException {
        invocation.print("username: ");
        return invocation.getInputLine();
    }

}
