package org.keycloak.client.registration.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.keycloak.client.cli.config.RealmConfigData;
import org.keycloak.client.registration.cli.KcRegMain;
import org.keycloak.common.util.IoUtils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "registration-token", description = "[--server SERVER] --realm REALM --client CLIENT [--delete | TOKEN] [ARGUMENTS]")
public class ConfigRegistrationTokenCmd extends AbstractAuthOptionsCmd {

    @Option(names = {"-d", "--delete"}, description = "Indicates that initial access token should be removed")
    private boolean delete;

    @Parameters(arity = "0..1")
    private String token;

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && token == null && !delete;
    }

    @Override
    protected void process() {
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
            token = IoUtils.readPasswordFromConsole("Registration Access Token");
        }

        // now update the config

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
    }

    @Override
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
        out.println("    --config                Path to the config file (" + KcRegMain.DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println();
        out.println("  Command specific options:");
        out.println("    --server SERVER         Server endpoint url (e.g. 'http://localhost:8080')");
        out.println("    --realm REALM           Realm name to use");
        out.println("    --client CLIENT         ClientId of client whose token to set");
        out.println("    -d, --delete            Indicates that registration access token should be removed");
        out.println("    TOKEN                   Registration access token (prompted for if not specified, unless -d is used)");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Specify registration access token for server, and realm. Token is passed via env variable:");
        out.println("  " + PROMPT + " " + CMD + " config registration-token --server http://localhost:9080 --realm master --client my_client " + OS_ARCH.envVar("TOKEN"));
        out.println();
        out.println("Remove registration access token:");
        out.println("  " + PROMPT + " " + CMD + " config registration-token --server http://localhost:9080 --realm master --client my_client --delete");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
