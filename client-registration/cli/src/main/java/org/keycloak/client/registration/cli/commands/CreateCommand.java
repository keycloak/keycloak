package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.Arguments;
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
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@CommandDefinition(name="create", description = "[OPTIONS] FILE")
public class CreateCommand implements Command {

    @Option(shortName = 'h', hasValue = false, description = "display this help and exit")
    private boolean help;

    @Arguments(description = "files or directories thats listed")
    private List<Resource> arguments;

    private Context context;

    public CreateCommand(Context context) {
        this.context = context;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
        System.out.println(help);


        if(help) {
            commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("create"));
        }
        else {

            if(arguments != null) {
                for(Resource f : arguments) {
                    System.out.println(f.getAbsolutePath());
                    ClientRepresentation rep = JsonSerialization.readValue(f.read(), ClientRepresentation.class);
                    try {
                        context.getReg().create(rep);
                    } catch (ClientRegistrationException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
//            reg.create();

        return CommandResult.SUCCESS;
    }

}
