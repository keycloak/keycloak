package org.keycloak.client.registration.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.keycloak.client.cli.config.RealmConfigData;
import org.keycloak.client.registration.cli.CmdStdinContext;
import org.keycloak.client.registration.cli.KcRegMain;
import org.keycloak.common.util.IoUtils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.cli.util.IoUtil.warnfOut;
import static org.keycloak.client.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "initial-token", description = "[--server SERVER] --realm REALM [--delete | TOKEN] [ARGUMENTS]")
public class ConfigInitialTokenCmd extends AbstractAuthOptionsCmd {

    @Option(names = {"-d", "--delete"}, description = "Indicates that initial access token should be removed")
    private boolean delete;
    @Option(names = {"-k", "--keep-domain"}, description = "Don't overwrite default server and realm")
    private boolean keepDomain;

    @Parameters(arity = "0..1")
    private String token;

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && token == null && !delete && !keepDomain;
    }

    @Override
    protected String[] getUnsupportedOptions() {
        return new String[] {
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
                "--no-config", booleanOptionForCheck(noconfig)};
    }

    @Override
    protected void process() {
        if (realm == null) {
            throw new IllegalArgumentException("Realm not specified");
        }

        if (token != null && token.startsWith("-")) {
            warnfOut(CmdStdinContext.TOKEN_OPTION_WARN, token);
        }

        if (!delete && token == null) {
            token = IoUtils.readPasswordFromConsole("Initial Access Token");
        }

        // now update the config

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
    }

    @Override
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
        out.println("    --config                Path to the config file (" + KcRegMain.DEFAULT_CONFIG_FILE_STRING + " by default)");
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
