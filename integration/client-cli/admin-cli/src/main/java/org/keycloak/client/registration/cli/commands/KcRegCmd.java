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
package org.keycloak.client.registration.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.keycloak.client.cli.common.BaseGlobalOptionsCmd;
import org.keycloak.client.registration.cli.KcRegMain;

import picocli.CommandLine.Command;

import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@Command(name = "kcreg",
header = {
        "Keycloak - Open Source Identity and Access Management",
        "",
        "Find more information at: https://www.keycloak.org/docs/latest"
},
description = {
        "%nCOMMAND [ARGUMENTS]"
},
subcommands = {
        HelpCmd.class,
        ConfigCmd.class,
        CreateCmd.class,
        GetCmd.class,
        UpdateCmd.class,
        DeleteCmd.class,
        AttrsCmd.class,
        UpdateTokenCmd.class
})
public class KcRegCmd extends BaseGlobalOptionsCmd {

    @Override
    protected boolean nothingToDo() {
        return true;
    }

    @Override
    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Keycloak Client Registration CLI");
        out.println();
        out.println("Use '" + CMD + " config credentials' command with username and password to start a session against a specific");
        out.println("server and realm.");
        out.println();
        out.println("For example:");
        out.println();
        out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:8080 --realm master --user admin");
        out.println("  Enter password: ");
        out.println("  Logging into http://localhost:8080 as user admin of realm master");
        out.println();
        out.println("Any configured username can be used for login, but to perform client registration operations the user");
        out.println("needs proper roles, otherwise attempts to create, update, read, or delete clients will fail.");
        out.println("Alternatively, the user without the necessary roles can use an Initial Access Token provided by realm");
        out.println("administrator when creating a new client with 'create' command. For example:");
        out.println();
        out.println("  " + PROMPT + " " + CMD + " create -f my_client.json -t -");
        out.println("  Enter Initial Access Token: ");
        out.println("  Registered new client with client_id 'my_client'");
        out.println();
        out.println("When Initial Access Token is used the server issues a Registration Access Token which is automatically");
        out.println("handled by " + CMD + ", saved into a local config file, and automatically used for any follow-up operations");
        out.println("on the same client. For example:");
        out.println();
        out.println("  " + PROMPT + " " + CMD + " get my_client");
        out.println("  " + PROMPT + " " + CMD + " update my_client -s enabled=false");
        out.println("  " + PROMPT + " " + CMD + " delete my_client");
        out.println();
        out.println();
        out.println("Usage: " + CMD + " COMMAND [ARGUMENTS]");
        out.println();
        out.println("Global options:");
        out.println("  -x            Print full stack trace when exiting with error");
        out.println("  --help        Print help for specific command");
        out.println("  --config      Path to the config file (" + KcRegMain.DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println("  --no-config   Don't use config file - no authentication info is loaded or saved");
        out.println();
        out.println("Commands: ");
        out.println("  config        Set up credentials, and other configuration settings using the config file");
        out.println("  create        Register a new client");
        out.println("  get           Get configuration of existing client in Keycloak or OIDC format, or adapter install configuration");
        out.println("  update        Update a client configuration");
        out.println("  delete        Delete a client");
        out.println("  attrs         List available attributes");
        out.println("  update-token  Update Registration Access Token for a client");
        out.println("  help          This help");
        out.println();
        out.println("Use '" + CMD + " help <command>' for more information about a given command.");
        return sb.toString();
    }
}
