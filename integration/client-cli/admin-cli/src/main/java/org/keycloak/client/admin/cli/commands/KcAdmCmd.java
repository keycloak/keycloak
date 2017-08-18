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

import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@GroupCommandDefinition(name = "kcadm", description = "COMMAND [ARGUMENTS]", groupCommands = {
    HelpCmd.class, ConfigCmd.class, NewObjectCmd.class, CreateCmd.class, GetCmd.class, UpdateCmd.class, DeleteCmd.class,
        AddRolesCmd.class, RemoveRolesCmd.class, GetRolesCmd.class, SetPasswordCmd.class} )
public class KcAdmCmd extends AbstractGlobalOptionsCmd {

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            // if --help was requested then status is SUCCESS
            // if not we print help anyway, but status is FAILURE
            if (printHelp()) {
                return CommandResult.SUCCESS;
            } else if (args != null && args.size() > 0) {
                printErr("Unknown command: " + args.get(0));
                return CommandResult.FAILURE;
            } else {
                printOut(usage());
                return CommandResult.FAILURE;
            }
        } finally {
            commandInvocation.stop();
        }
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Keycloak Admin CLI");
        out.println();
        out.println("Use '" + CMD + " config credentials' command with username and password to start a session against a specific");
        out.println("server and realm.");
        out.println();
        out.println("For example:");
        out.println();
        out.println("  " + PROMPT + " " + CMD + " config credentials --server http://localhost:8080/auth --realm master --user admin");
        out.println("  Enter password: ");
        out.println("  Logging into http://localhost:8080/auth as user admin of realm master");
        out.println();
        out.println("Any configured username can be used for login, but to perform admin operations the user");
        out.println("needs proper roles, otherwise operations will fail.");
        out.println();
        out.println("Usage: " + CMD + " COMMAND [ARGUMENTS]");
        out.println();
        out.println("Global options:");
        out.println("  -x            Print full stack trace when exiting with error");
        out.println("  --help        Print help for specific command");
        out.println("  --config      Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println();
        out.println("Commands: ");
        out.println("  config        Set up credentials, and other configuration settings using the config file");
        out.println("  create        Create new resource");
        out.println("  get           Get a resource");
        out.println("  update        Update a resource");
        out.println("  delete        Delete a resource");
        out.println("  get-roles     List roles for a user or a group");
        out.println("  add-roles     Add role to a user or a group");
        out.println("  remove-roles  Remove role from a user or a group");
        out.println("  set-password  Re-set password for a user");
        out.println("  help          This help");
        out.println();
        out.println("Use '" + CMD + " help <command>' for more information about a given command.");
        return sb.toString();
    }
}
