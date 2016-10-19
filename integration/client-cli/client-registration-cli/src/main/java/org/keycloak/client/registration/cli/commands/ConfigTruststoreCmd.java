package org.keycloak.client.registration.cli.commands;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.keycloak.client.registration.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.registration.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.registration.cli.util.IoUtil.readSecret;
import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.registration.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "truststore", description = "PATH [ARGUMENTS]")
public class ConfigTruststoreCmd extends AbstractAuthOptionsCmd implements Command {

    private ConfigCmd parent;

    private boolean delete;

    public ConfigTruststoreCmd() {}

    public ConfigTruststoreCmd(ConfigCmd parent) {
        this.parent = parent;
        init(parent);
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            return process(commandInvocation);
        } finally {
            commandInvocation.stop();
        }
    }

    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        List<String> args = new ArrayList<>();

        Iterator<String> it = parent.args.iterator();
        // skip the first argument 'truststore'
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
            throw new RuntimeException("Invalid option: " + args.get(1));
        }

        String truststore = null;
        if (args.size() > 0) {
            truststore = args.get(0);
        }

        checkUnsupportedOptions("--server", server,
                "--realm", realm,
                "--client", clientId,
                "--user", user,
                "--password", password,
                "--secret", secret,
                "--truststore", trustStore,
                "--keystore", keystore,
                "--keypass", keyPass,
                "--alias", alias);

        // now update the config
        processGlobalOptions();

        String store;
        String pass;

        if (!delete) {

            if (truststore == null) {
                throw new RuntimeException("No truststore specified");
            }

            if (!new File(truststore).isFile()) {
                throw new RuntimeException("Truststore file not found: " + truststore);
            }

            if ("-".equals(trustPass)) {
                trustPass = readSecret("Enter truststore password: ", commandInvocation);
            }

            store = truststore;
            pass = trustPass;

        } else {
            if (truststore != null) {
                throw new RuntimeException("Option --delete is mutually exclusive with specifying a TRUSTSTORE");
            }
            if (trustPass != null) {
                throw new RuntimeException("Options --trustpass and --delete are mutually exclusive");
            }
            store = null;
            pass = null;
        }

        saveMergeConfig(config -> {
            config.setTruststore(store);
            config.setTrustpass(pass);
        });

        return CommandResult.SUCCESS;
    }


    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWOD] [ARGUMENTS]");
        out.println();
        out.println("Command to configure a global truststore to use when using https to connect to Keycloak server.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                      Print full stack trace when exiting with error");
        out.println("    --config                Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println();
        out.println("  Command specific options:");
        out.println("    TRUSTSTORE              Path to truststore file");
        out.println("    --trustpass PASSWORD    Truststore password to unlock truststore (prompted for if set to '-')");
        out.println("    -d, --delete            Remove truststore configuration");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Specify a truststore - you will be prompted for truststore password every time it is used:");
        out.println("  " + PROMPT + " " + CMD + " config truststore " + OS_ARCH.path("~/.keycloak/truststore.jks"));
        out.println();
        out.println("Specify a truststore, and password - truststore will automatically without prompting for password:");
        out.println("  " + PROMPT + " " + CMD + " config truststore --storepass " + OS_ARCH.envVar("PASSWORD") + " " + OS_ARCH.path("~/.keycloak/truststore.jks"));
        out.println();
        out.println("Remove truststore configuration:");
        out.println("  " + PROMPT + " " + CMD + " config truststore --delete");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
