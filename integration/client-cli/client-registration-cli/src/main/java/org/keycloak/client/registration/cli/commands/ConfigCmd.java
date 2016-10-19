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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.util.OsUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@GroupCommandDefinition(name = "config", description = "COMMAND [ARGUMENTS]", groupCommands = {ConfigCredentialsCmd.class} )
public class ConfigCmd extends AbstractAuthOptionsCmd implements Command {

    @Arguments
    protected List<String> args;


    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (args.size() == 0) {
                throw new RuntimeException("Sub-command required by '" + OsUtil.CMD + " config' - one of: 'credentials', 'truststore', 'initial-token', 'registration-token'");
            }

            String cmd = args.get(0);
            switch (cmd) {
                case "credentials": {
                    return new ConfigCredentialsCmd(this).execute(commandInvocation);
                }
                case "truststore": {
                    return new ConfigTruststoreCmd(this).execute(commandInvocation);
                }
                case "initial-token": {
                    return new ConfigInitialTokenCmd(this).execute(commandInvocation);
                }
                case "registration-token": {
                    return new ConfigRegistrationTokenCmd(this).execute(commandInvocation);
                }
                default:
                    throw new RuntimeException("Unknown sub-command: " + cmd);
            }

        } finally {
            commandInvocation.stop();
        }
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + OsUtil.CMD + " config SUB_COMMAND [ARGUMENTS]");
        out.println();
        out.println("Where SUB_COMMAND is one of: 'credentials', 'truststore', 'initial-token', 'registration-token'");
        out.println();
        out.println();
        out.println("Use '" + OsUtil.CMD + " help config SUB_COMMAND' for more info.");
        out.println("Use '" + OsUtil.CMD + " help' for general information and a list of commands.");
        return sb.toString();
    }
}
