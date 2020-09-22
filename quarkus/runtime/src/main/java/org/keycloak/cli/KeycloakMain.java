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

import static org.keycloak.cli.Picocli.createCommandSpec;
import static org.keycloak.cli.Picocli.parseConfigArgs;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.common.Version;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain(name = "keycloak")
public class KeycloakMain {

    public static void main(String args[]) {
        System.setProperty("kc.version", Version.VERSION_KEYCLOAK);

        if (args.length != 0) {
            CommandLine cmd = new CommandLine(createCommandSpec());
            List<String> argsList = new LinkedList<>(Arrays.asList(args));

            try {
                System.setProperty("kc.config.args", parseConfigArgs(argsList));
                CommandLine.ParseResult result = cmd.parseArgs(argsList.toArray(new String[argsList.size()]));

                if (!result.hasSubcommand() && (!result.isUsageHelpRequested() && !result.isVersionHelpRequested())) {
                    argsList.add(0, "start");
                }
            } catch (CommandLine.UnmatchedArgumentException e) {
                if (!cmd.getParseResult().hasSubcommand()) {
                    argsList.add(0, "start");
                } else {
                    cmd.getErr().println(e.getMessage());
                    System.exit(CommandLine.ExitCode.SOFTWARE);
                }
            }

            System.exit(cmd.execute(argsList.toArray(new String[argsList.size()])));
        }

        Quarkus.run(args);
        Quarkus.waitForExit();
    }
}
