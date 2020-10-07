/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cli;

import static org.keycloak.cli.Picocli.createCommandLine;
import static org.keycloak.cli.Picocli.error;
import static org.keycloak.cli.Picocli.parseConfigArgs;
import static org.keycloak.util.Environment.getProfileOrDefault;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.quarkus.runtime.Quarkus;
import org.keycloak.common.Version;

import io.quarkus.runtime.annotations.QuarkusMain;
import org.keycloak.util.Environment;
import picocli.CommandLine;

/**
 * <p>The main entry point, responsible for initialize and run the CLI as well as start the server.
 */
@QuarkusMain(name = "keycloak")
public class KeycloakMain {

    public static void main(String cliArgs[]) {
        System.setProperty("kc.version", Version.VERSION_KEYCLOAK);
        CommandLine cmd = createCommandLine();

        if (cliArgs.length == 0) {
            // no arguments, just start the server
            start(cmd);
            System.exit(cmd.getCommandSpec().exitCodeOnSuccess());
        }

        // parse arguments and execute any of the configured commands
        parseAndRun(cmd, cliArgs);
    }

    static void start(CommandLine cmd) {
        // We should use the method call below to start the server once Quarkus is released with https://github.com/quarkusio/quarkus/pull/12532.
        // It will allow us to properly handle exception thrown during startup and at runtime.
//        Quarkus.run(null, (integer, throwable) -> {
//            error(cmd, String.format("Failed to start server using profile (%s).", getProfileOrDefault("none")), throwable.getCause());
//        });
        Quarkus.run(null, (integer) -> {
            error(cmd, String.format("Failed to start server using profile (%s).", getProfileOrDefault("none")), null);
        });
        Quarkus.waitForExit();
    }
    
    private static void parseAndRun(CommandLine cmd, String[] args) {
        List<String> cliArgs = new LinkedList<>(Arrays.asList(args));

        // set the arguments as a system property so that arguments can be mapped to their respective configuration options
        System.setProperty("kc.config.args", parseConfigArgs(cliArgs));

        try {
            CommandLine.ParseResult result = cmd.parseArgs(cliArgs.toArray(new String[cliArgs.size()]));

            // if no command was set, the start command becomes the default
            if (!result.hasSubcommand() && (!result.isUsageHelpRequested() && !result.isVersionHelpRequested())) {
                cliArgs.add(0, "start");
            }
        } catch (CommandLine.UnmatchedArgumentException e) {
            // if no command was set but options were provided, the start command becomes the default
            if (!cmd.getParseResult().hasSubcommand() && cliArgs.get(0).startsWith("--")) {
                cliArgs.add(0, "start");
            } else {
                cmd.getErr().println(e.getMessage());
                System.exit(cmd.getCommandSpec().exitCodeOnInvalidInput());
            }
        } catch (Exception e) {
            cmd.getErr().println(e.getMessage());
            System.exit(cmd.getCommandSpec().exitCodeOnExecutionException());
        }

        System.exit(cmd.execute(cliArgs.toArray(new String[cliArgs.size()])));
    }
}
