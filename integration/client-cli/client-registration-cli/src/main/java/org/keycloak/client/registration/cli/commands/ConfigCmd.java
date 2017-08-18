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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.EOL;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@GroupCommandDefinition(name = "config", description = "COMMAND [ARGUMENTS]", groupCommands = {ConfigCredentialsCmd.class} )
public class ConfigCmd extends AbstractAuthOptionsCmd implements Command {

    @Arguments
    protected List<String> args;


    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (args != null && args.size() > 0) {
                String cmd = args.get(0);
                switch (cmd) {
                    case "credentials": {
                        ConfigCredentialsCmd command = new ConfigCredentialsCmd();
                        command.initFromParent(this);
                        return command.execute(commandInvocation);
                    }
                    case "truststore": {
                        ConfigTruststoreCmd command = new ConfigTruststoreCmd();
                        command.initFromParent(this);
                        return command.execute(commandInvocation);
                    }
                    case "initial-token": {
                        ConfigInitialTokenCmd command = new ConfigInitialTokenCmd();
                        command.initFromParent(this);
                        return command.execute(commandInvocation);
                    }
                    case "registration-token": {
                        ConfigRegistrationTokenCmd command = new ConfigRegistrationTokenCmd();
                        command.initFromParent(this);
                        return command.execute(commandInvocation);
                    }
                    default: {
                        if (printHelp()) {
                            return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
                        }
                        throw new IllegalArgumentException("Unknown sub-command: " + cmd + suggestHelp());
                    }
                }
            }

            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            throw new IllegalArgumentException("Sub-command required by '" + CMD + " config' - one of: 'credentials', 'truststore', 'initial-token', 'registration-token'");

        } finally {
            commandInvocation.stop();
        }
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help config' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " config SUB_COMMAND [ARGUMENTS]");
        out.println();
        out.println("Where SUB_COMMAND is one of: 'credentials', 'truststore', 'initial-token', 'registration-token'");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help config SUB_COMMAND' for more info.");
        out.println("Use '" + CMD + " help' for general information and a list of commands.");
        return sb.toString();
    }
}
