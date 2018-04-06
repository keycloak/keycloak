package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.util.List;

import static org.keycloak.client.registration.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "help", description = "This help")
public class HelpCmd implements Command {

    @Arguments
    private List<String> args;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (args == null || args.size() == 0) {
                printOut(KcRegCmd.usage());
            } else {
                outer:
                switch (args.get(0)) {
                    case "config": {
                        if (args.size() > 1) {
                            switch (args.get(1)) {
                                case "credentials": {
                                    printOut(ConfigCredentialsCmd.usage());
                                    break outer;
                                }
                                case "initial-token": {
                                    printOut(ConfigInitialTokenCmd.usage());
                                    break outer;
                                }
                                case "registration-token": {
                                    printOut(ConfigRegistrationTokenCmd.usage());
                                    break outer;
                                }
                                case "truststore": {
                                    printOut(ConfigTruststoreCmd.usage());
                                    break outer;
                                }
                            }
                        }
                        printOut(ConfigCmd.usage());
                        break;
                    }
                    case "create": {
                        printOut(CreateCmd.usage());
                        break;
                    }
                    case "get": {
                        printOut(GetCmd.usage());
                        break;
                    }
                    case "update": {
                        printOut(UpdateCmd.usage());
                        break;
                    }
                    case "delete": {
                        printOut(DeleteCmd.usage());
                        break;
                    }
                    case "attrs": {
                        printOut(AttrsCmd.usage());
                        break;
                    }
                    case "update-token": {
                        printOut(UpdateTokenCmd.usage());
                        break;
                    }
                    default: {
                        throw new RuntimeException("Unknown command: " + args.get(0));
                    }
                }
            }

            return CommandResult.SUCCESS;
        } finally {
            commandInvocation.stop();
        }
    }
}
