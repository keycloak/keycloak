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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.admin.cli.util.IoUtil.readSecret;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "truststore", description = "PATH [ARGUMENTS]")
public class ConfigTruststoreCmd extends AbstractAuthOptionsCmd {

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
        return noOptions();
    }

    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        List<String> args = new ArrayList<>();

        Iterator<String> it = parent.args.iterator();

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
                "--alias", alias,
                "--no-config", booleanOptionForCheck(noconfig));

        // now update the config
        processGlobalOptions();

        String store;
        String pass;

        if (!delete) {

            if (truststore == null) {
                throw new IllegalArgumentException("No truststore specified");
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
                throw new IllegalArgumentException("Option --delete is mutually exclusive with specifying a TRUSTSTORE");
            }
            if (trustPass != null) {
                throw new IllegalArgumentException("Options --trustpass and --delete are mutually exclusive");
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

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help config truststore' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWORD] [ARGUMENTS]");
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
        out.println("Specify a truststore, and password - truststore will automatically be used without prompting for password:");
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
