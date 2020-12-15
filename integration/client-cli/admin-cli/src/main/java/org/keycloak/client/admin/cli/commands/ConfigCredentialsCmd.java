/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.OAuth2Constants;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.config.RealmConfigData;
import org.keycloak.client.admin.cli.util.AuthUtil;
import org.keycloak.representations.AccessTokenResponse;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import static org.keycloak.client.admin.cli.util.AuthUtil.getAuthTokens;
import static org.keycloak.client.admin.cli.util.AuthUtil.getAuthTokensByJWT;
import static org.keycloak.client.admin.cli.util.AuthUtil.getAuthTokensBySecret;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.getHandler;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.ConfigUtil.saveTokens;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.IoUtil.readSecret;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "credentials", description = "--server SERVER_URL --realm REALM [ARGUMENTS]")
public class ConfigCredentialsCmd extends AbstractAuthOptionsCmd {

    private int sigLifetime = 600;


    public void init(ConfigData configData) {
        if (server == null) {
            server = configData.getServerUrl();
        }
        if (realm == null) {
            realm = configData.getRealm();
        }
        if (trustStore == null) {
            trustStore = configData.getTruststore();
        }

        RealmConfigData rdata = configData.getRealmConfigData(server, realm);
        if (rdata == null) {
            return;
        }

        if (clientId == null) {
            clientId = rdata.getClientId();
        }
    }


    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            checkUnsupportedOptions("--no-config", booleanOptionForCheck(noconfig));

            processGlobalOptions();

            return process(commandInvocation);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions();
    }

    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        // check server
        if (server == null) {
            throw new IllegalArgumentException("Required option not specified: --server");
        }

        try {
            new URL(server);
        } catch (Exception e) {
            throw new RuntimeException("Invalid server endpoint url: " + server, e);
        }

        if (realm == null)
            throw new IllegalArgumentException("Required option not specified: --realm");

        String signedRequestToken = null;
        boolean clientSet = clientId != null;

        applyDefaultOptionValues();
        String grantTypeForAuthentication = null;

        if (user != null) {
            grantTypeForAuthentication = OAuth2Constants.PASSWORD;
            printErr("Logging into " + server + " as user " + user + " of realm " + realm);

            // if user was set there needs to be a password so we can authenticate
            if (password == null) {
                password = readSecret("Enter password: ", commandInvocation);
            }
            // if secret was set to be read from stdin, then ask for it
            if ("-".equals(secret) && keystore == null) {
                secret = readSecret("Enter client secret: ", commandInvocation);
            }
        } else if (keystore != null || secret != null || clientSet) {
            grantTypeForAuthentication = OAuth2Constants.CLIENT_CREDENTIALS;
            printErr("Logging into " + server + " as " + "service-account-" + clientId + " of realm " + realm);
            if (keystore == null) {
                if (secret == null) {
                    secret = readSecret("Enter client secret: ", commandInvocation);
                }
            }
        }

        if (keystore != null) {
            if (secret != null) {
                throw new IllegalArgumentException("Can't use both --keystore and --secret");
            }

            if (!new File(keystore).isFile()) {
                throw new RuntimeException("No such keystore file: " + keystore);
            }

            if (storePass == null) {
                storePass = readSecret("Enter keystore password: ", commandInvocation);
                keyPass = readSecret("Enter key password: ", commandInvocation);
            }

            if (keyPass == null) {
                keyPass = storePass;
            }

            if (alias == null) {
                alias = clientId;
            }

            String realmInfoUrl = server + "/realms/" + realm;

            signedRequestToken = AuthUtil.getSignedRequestToken(keystore, storePass, keyPass,
                    alias, sigLifetime, clientId, realmInfoUrl);
        }

        // if only server and realm are set, just save config and be done
        if (user == null && secret == null && keystore == null) {
            getHandler().saveMergeConfig(config -> {
                config.setServerUrl(server);
                config.setRealm(realm);
            });
            return CommandResult.SUCCESS;
        }

        setupTruststore(copyWithServerInfo(loadConfig()), commandInvocation);

        // now use the token endpoint to retrieve access token, and refresh token
        AccessTokenResponse tokens = signedRequestToken != null ?
                getAuthTokensByJWT(server, realm, user, password, clientId, signedRequestToken) :
                secret != null ?
                        getAuthTokensBySecret(server, realm, user, password, clientId, secret) :
                        getAuthTokens(server, realm, user, password, clientId);

        Long sigExpiresAt = signedRequestToken == null ? null : System.currentTimeMillis() + sigLifetime * 1000;

        // save tokens to config file
        saveTokens(tokens, server, realm, clientId, signedRequestToken, sigExpiresAt, secret, grantTypeForAuthentication);

        return CommandResult.SUCCESS;
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help config credentials' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " config credentials --server SERVER_URL --realm REALM --user USER [--password PASSWORD] [ARGUMENTS]");
        out.println("       " + CMD + " config credentials --server SERVER_URL --realm REALM --client CLIENT_ID [--secret SECRET] [ARGUMENTS]");
        out.println("       " + CMD + " config credentials --server SERVER_URL --realm REALM --client CLIENT_ID [--keystore KEYSTORE] [ARGUMENTS]");
        out.println();
        out.println("Command to establish an authenticated client session with the server. There are many authentication");
        out.println("options available, and it depends on server side client authentication configuration how client can or should authenticate.");
        out.println("The information always required includes --server, and --realm. Then, --user and / or --client need to be used to authenticate.");
        out.println("If --client is not provided it defaults to 'admin-cli'. The authentication options / requirements depend on how this client is configured.");
        out.println();
        out.println("If confidential client authentication is also configured, you may have to specify a client id, and client credentials in addition to");
        out.println("user credentials. Client credentials are either a client secret, or a keystore information to use Signed JWT mechanism.");
        out.println("If only client credentials are provided, and no user credentials, then the service account is used for login.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                      Print full stack trace when exiting with error");
        out.println("    --config                Path to a config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println("    --truststore PATH       Path to a truststore containing trusted certificates");
        out.println("    --trustpass PASSWORD    Truststore password (prompted for if not specified and --truststore is used)");
        out.println();
        out.println("  Command specific options:");
        out.println("    --server SERVER_URL     Server endpoint url (e.g. 'http://localhost:8080/auth')");
        out.println("    --realm REALM           Realm name to use");
        out.println("    --user USER             Username to login with");
        out.println("    --password PASSWORD     Password to login with (prompted for if not specified and --user is used)");
        out.println("    --client CLIENT_ID      ClientId used by this client tool ('admin-cli' by default)");
        out.println("    --secret SECRET         Secret to authenticate the client (prompted for if --client is specified, and no --keystore is specified)");
        out.println("    --keystore PATH         Path to a keystore containing private key");
        out.println("    --storepass PASSWORD    Keystore password (prompted for if not specified and --keystore is used)");
        out.println("    --keypass PASSWORD      Key password (prompted for if not specified and --keystore is used without --storepass,");
        out.println("                            otherwise defaults to keystore password)");
        out.println("    --alias ALIAS           Alias of the key inside a keystore (defaults to the value of ClientId)");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Login as 'admin' user of 'master' realm to a local Keycloak server running on default port.");
        out.println("You will be prompted for a password:");
        out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:8080/auth --realm master --user admin");
        out.println();
        out.println("Login to Keycloak server at non-default endpoint passing the password via standard input:");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " echo mypassword | " + CMD + " config credentials --server http://localhost:9080/auth --realm master --user admin");
        } else {
            out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:9080/auth --realm master --user admin << EOF");
            out.println("  mypassword");
            out.println("  EOF");
        }
        out.println();
        out.println("Login specifying a password through command line:");
        out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:9080/auth --realm master --user admin --password " + OS_ARCH.envVar("PASSWORD"));
        out.println();
        out.println("Login using a client service account of a custom client. You will be prompted for a client secret:");
        out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:9080/auth --realm master --client reg-cli");
        out.println();
        out.println("Login using a client service account of a custom client, authenticating with signed JWT.");
        out.println("You will be prompted for a keystore password, and a key password:");
        out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:9080/auth --realm master --client reg-cli --keystore " + OS_ARCH.path("~/.keycloak/keystore.jks"));
        out.println();
        out.println("Login as 'user' while also authenticating a custom client with signed JWT.");
        out.println("You will be prompted for a user password, a keystore password, and a key password:");
        out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:9080/auth --realm master --user user --client reg-cli --keystore " + OS_ARCH.path("~/.keycloak/keystore.jks"));
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
