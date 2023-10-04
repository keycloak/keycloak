package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.config.RealmConfigData;
import org.keycloak.client.registration.cli.util.IoUtil;
import org.keycloak.client.registration.cli.util.ParseUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.keycloak.client.registration.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.registration.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.registration.cli.util.IoUtil.warnfOut;
import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.EOL;
import static org.keycloak.client.registration.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.registration.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "initial-token", description = "[--server SERVER] --realm REALM [--delete | TOKEN] [ARGUMENTS]")
public class ConfigInitialTokenCmd extends AbstractAuthOptionsCmd implements Command {

    private ConfigCmd parent;

    private boolean delete;
    private boolean keepDomain;


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
        // skip the first argument 'initial-token'
        it.next();

        while (it.hasNext()) {
            String arg = it.next();
            switch (arg) {
                case "-d":
                case "--delete": {
                    delete = true;
                    break;
                }
                case "-k":
                case "--keep-domain": {
                    keepDomain = true;
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

        if (realm == null) {
            throw new IllegalArgumentException("Realm not specified");
        }

        if (token != null && token.startsWith("-")) {
            warnfOut(ParseUtil.TOKEN_OPTION_WARN, token);
        }

        checkUnsupportedOptions(
                "--client", clientId,
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
            token = IoUtil.readSecret("Enter Initial Access Token: ", commandInvocation);
        }

        // now update the config
        processGlobalOptions();

        String initialToken = token;
        saveMergeConfig(config -> {
            if (!keepDomain && !delete) {
                config.setServerUrl(server);
                config.setRealm(realm);
            }
            if (delete) {
                RealmConfigData rdata = config.getRealmConfigData(server, realm);
                if (rdata != null) {
                    rdata.setInitialToken(null);
                }
            } else {
                RealmConfigData rdata = config.ensureRealmConfigData(server, realm);
                rdata.setInitialToken(initialToken);
            }
        });

        return CommandResult.SUCCESS;
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help config initial-token' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " config initial-token --server SERVER --realm REALM [--delete | TOKEN] [ARGUMENTS]");
        out.println();
        out.println("Command to configure an initial access token to be used with '" + CMD + " create' command. Even if an ");
        out.println("authenticated session exists as a result of '" + CMD + " config credentials' its access token will not");
        out.println("be used - initial access token will be used instead. By default, current server, and realm will");
        out.println("be set to the new values thus subsequent commands will use these values as default.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                      Print full stack trace when exiting with error");
        out.println("    --config                Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println();
        out.println("  Command specific options:");
        out.println("    --server SERVER         Server endpoint url (e.g. 'http://localhost:8080')");
        out.println("    --realm REALM           Realm name to use");
        out.println("    -k, --keep-domain       Don't overwrite default server and realm");
        out.println("    -d, --delete            Indicates that initial access token should be removed");
        out.println("    TOKEN                   Initial access token (prompted for if not specified, unless -d is used)");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Specify initial access token for server, and realm. Token is passed via env variable:");
        out.println("  " + PROMPT + " " + CMD + " config initial-token --server http://localhost:9080 --realm master " + OS_ARCH.envVar("TOKEN"));
        out.println();
        out.println("Remove initial access token:");
        out.println("  " + PROMPT + " " + CMD + " config initial-token --server http://localhost:9080 --realm master --delete");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
