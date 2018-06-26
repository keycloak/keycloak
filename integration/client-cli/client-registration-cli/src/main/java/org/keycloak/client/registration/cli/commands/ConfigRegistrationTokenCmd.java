package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.config.RealmConfigData;
import org.keycloak.client.registration.cli.util.IoUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.keycloak.client.registration.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.registration.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.EOL;
import static org.keycloak.client.registration.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.registration.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "registration-token", description = "[--server SERVER] --realm REALM --client CLIENT [--delete | TOKEN] [ARGUMENTS]")
public class ConfigRegistrationTokenCmd extends AbstractAuthOptionsCmd implements Command {

    private ConfigCmd parent;

    private boolean delete;


    protected void initFromParent(ConfigCmd parent) {
        this.parent = parent;
        super.initFromParent(parent);
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            return process(commandInvocation);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions() && parent.args.size() == 1;
    }

    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        List<String> args = new ArrayList<>();

        Iterator<String> it = parent.args.iterator();
        // skip the first argument 'registration-token'
        it.next();

        while (it.hasNext()) {
            String arg = it.next();
            switch (arg) {
                case "-d":
                case "--delete": {
                    delete = true;
                    break;
                }
                default: {
                    args.add(arg);
                }
            }
        }

        if (args.size() > 1) {
            throw new IllegalArgumentException("Invalid option: " + args.get(1));
        }

        String token = args.size() == 1 ? args.get(0) : null;

        if (server == null) {
            throw new IllegalArgumentException("Required option not specified: --server");
        }

        if (realm == null) {
            throw new IllegalArgumentException("Required option not specified: --realm");
        }

        if (clientId == null) {
            throw new IllegalArgumentException("Required option not specified: --client");
        }

        checkUnsupportedOptions(
                "--user", user,
                "--password", password,
                "--secret", secret,
                "--keystore", keystore,
                "--storepass", storePass,
                "--keypass", keyPass,
                "--alias", alias,
                "--truststore", trustStore,
                "--trustpass", keyPass,
                "--no-config", booleanOptionForCheck(noconfig));


        if (!delete && token == null) {
            token = IoUtil.readSecret("Enter Registration Access Token: ", commandInvocation);
        }

        // now update the config
        processGlobalOptions();

        String registrationToken = token;
        saveMergeConfig(config -> {
            RealmConfigData rdata = config.getRealmConfigData(server, realm);
            if (delete) {
                if (rdata != null) {
                    rdata.getClients().remove(clientId);
                }
            } else {
                config.ensureRealmConfigData(server, realm).getClients().put(clientId, registrationToken);
            }
        });

        return CommandResult.SUCCESS;
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help config registration-token' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " config registration-token --server SERVER --realm REALM --client CLIENT [--delete | TOKEN] [ARGUMENTS]");
        out.println();
        out.println("Command to configure a registration access token to be used with 'kcreg get / update / delete' commands. Even if an ");
        out.println("authenticated session exists as a result of '" + CMD + " config credentials' its access token will not be used - registration");
        out.println("access token will be used instead.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                      Print full stack trace when exiting with error");
        out.println("    --config                Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println();
        out.println("  Command specific options:");
        out.println("    --server SERVER         Server endpoint url (e.g. 'http://localhost:8080/auth')");
        out.println("    --realm REALM           Realm name to use");
        out.println("    --client CLIENT         ClientId of client whose token to set");
        out.println("    -d, --delete            Indicates that registration access token should be removed");
        out.println("    TOKEN                   Registration access token (prompted for if not specified, unless -d is used)");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Specify registration access token for server, and realm. Token is passed via env variable:");
        out.println("  " + PROMPT + " " + CMD + " config registration-token --server http://localhost:9080/auth --realm master --client my_client " + OS_ARCH.envVar("TOKEN"));
        out.println();
        out.println("Remove registration access token:");
        out.println("  " + PROMPT + " " + CMD + " config registration-token --server http://localhost:9080/auth --realm master --client my_client --delete");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
