package org.keycloak.client.registration.cli;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalString;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.cli.commands.CreateCommand;
import org.keycloak.client.registration.cli.commands.ExitCommand;
import org.keycloak.client.registration.cli.commands.SetupCommand;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationCLI {

    private static ClientRegistration reg;

    public static void main(String[] args) throws CommandLineParserException, CommandNotFoundException {
        reg = ClientRegistration.create().url("http://localhost:8080/auth/realms/master").build();
        reg.auth(Auth.token("..."));

        Context context = new Context();

        List<Command> commands = new LinkedList<>();
        commands.add(new SetupCommand(context));
        commands.add(new CreateCommand(context));
        commands.add(new ExitCommand(context));

        SettingsBuilder builder = new SettingsBuilder().logging(true);
        builder.enableMan(true).readInputrc(false);

        Settings settings = builder.create();

        AeshCommandRegistryBuilder commandRegistryBuilder = new AeshCommandRegistryBuilder();
        for (Command c : commands) {
            commandRegistryBuilder.command(c);
        }

        AeshConsole aeshConsole = new AeshConsoleBuilder()
                .commandRegistry(commandRegistryBuilder.create())
                .settings(settings)
                .prompt(new Prompt(new TerminalString("[clientreg]$ ",
                        new TerminalColor(Color.GREEN, Color.DEFAULT, Color.Intensity.BRIGHT))))
                .create();

        aeshConsole.start();


/*
        if (args.length > 0) {
            CommandContainer command = registry.getCommand(args[0], null);
            ParserGenerator.parseAndPopulate(command, args[0], Arrays.copyOfRange(args, 1, args.length));
        }*/

        //commandInvocation.getCommandRegistry().getAllCommandNames()
    }

}

