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

import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "keycloak",
        usageHelpWidth = 150, 
        header = {
            "Keycloak - Open Source Identity and Access Management",
            "",
            "Find more information at: https://www.keycloak.org/",
            ""
        },
        description = "Use this command-line tool to manage your Keycloak cluster%n", footerHeading = "%nUse \"${COMMAND-NAME} <command> --help\" for more information about a command.",
        footer = "%nby Red Hat",
        optionListHeading = "Configuration Options%n%n",
        commandListHeading = "%nCommands%n%n",
        version = {
        "Keycloak ${sys:kc.version}",
        "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
        "OS: ${os.name} ${os.version} ${os.arch}"
        },
        subcommands = {
                Build.class,
                Start.class,
                StartDev.class,
                Export.class,
                Import.class,
                ShowConfig.class,
                Tools.class
        }
)
public final class Main {

    @CommandLine.Option(names = "-D<key>=<value>", description = "Set a Java system property", scope = CommandLine.ScopeType.INHERIT, order = 0)
    Boolean sysProps;

    @Option(names = { "-h", "--help" }, description = "This help message.", usageHelp = true)
    boolean help;

    @Option(names = { "-V", "--version" }, description = "Show version information", versionHelp = true)
    boolean version;

    @Option(names = { "-v", "--verbose" },
            description = "Print out more details when running this command. Useful for troubleshooting if some unexpected error occurs.", required = false,
            scope = CommandLine.ScopeType.INHERIT)
    Boolean verbose;

    @Option(names = {"-cf", "--config-file"}, arity = "1", description = "Set the path to a configuration file.", paramLabel = "<path>")
    public void setConfigFile(String path) {
        System.setProperty(KeycloakConfigSourceProvider.KEYCLOAK_CONFIG_FILE_PROP, path);
    }
}