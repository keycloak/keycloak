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

package org.keycloak.quarkus.runtime.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "keycloak",
        header = {
                "Keycloak - Open Source Identity and Access Management",
                "",
                "Find more information at: https://www.keycloak.org/docs/latest",
                ""
        },
        description = "%nUse this command-line tool to manage your Keycloak cluster.%n",
        footerHeading = "%nExamples:%n%n"
                + "  Start the server in development mode for local development or testing:%n%n"
                + "      $ ${COMMAND-NAME} start-dev%n%n"
                + "  Building an optimized server runtime:%n%n"
                + "      $ ${COMMAND-NAME} build <OPTIONS>%n%n"
                + "  Start the server in production mode:%n%n"
                + "      $ ${COMMAND-NAME} start <OPTIONS>%n%n"
                + "  Enable auto-completion to bash/zsh:%n%n"
                + "      $ source <(${COMMAND-NAME} tools completion)%n%n"
                + "  Please, take a look at the documentation for more details before deploying in production.%n",
        footer = {
                "",
                "Use \"${COMMAND-NAME} start --help\" for the available options when starting the server.",
                "Use \"${COMMAND-NAME} <command> --help\" for more information about other commands.",
        },
        optionListHeading = "Options%n%n",
        commandListHeading = "%nCommands%n%n",
        abbreviateSynopsis = true,
        versionProvider = VersionProvider.class,
        subcommands = {
                Build.class,
                Start.class,
                StartDev.class,
                Export.class,
                Import.class,
                ShowConfig.class,
                Tools.class
        })
public final class Main {

    @Option(names = "-D<key>=<value>",
            description = "Set a Java system property",
            order = 0)
    Boolean sysProps;

    @Option(names = { "-h", "--help" },
            description = "This help message.",
            usageHelp = true)
    boolean help;

    @Option(names = { "-V", "--version" },
            description = "Show version information",
            versionHelp = true)
    boolean version;

    @Option(names = { "-v", "--verbose" },
            description = "Print out more details when running this command. Useful for troubleshooting if some unexpected error occurs.",
            required = false,
            scope = ScopeType.INHERIT)
    Boolean verbose;
}