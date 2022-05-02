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

import static org.keycloak.quarkus.runtime.cli.Picocli.NO_PARAM_LABEL;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.KeycloakPropertiesConfigSource;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "keycloak",
        header = {
                "Keycloak - Open Source Identity and Access Management",
                "",
                "Find more information at: https://www.keycloak.org/docs/latest"
        },
        description = {
                "%nUse this command-line tool to manage your Keycloak cluster.",
                "Make sure the command is available on your \"PATH\" or prefix it with \"./\" (e.g.: \"./${COMMAND-NAME}\") to execute from the current folder."
        },
        footerHeading = "Examples:",
        footer = { "  Start the server in development mode for local development or testing:%n%n"
                + "      $ ${COMMAND-NAME} start-dev%n%n"
                + "  Building an optimized server runtime:%n%n"
                + "      $ ${COMMAND-NAME} build <OPTIONS>%n%n"
                + "  Start the server in production mode:%n%n"
                + "      $ ${COMMAND-NAME} start <OPTIONS>%n%n"
                + "  Enable auto-completion to bash/zsh:%n%n"
                + "      $ source <(${COMMAND-NAME} tools completion)%n%n"
                + "  Please, take a look at the documentation for more details before deploying in production.",
                "",
                "Use \"${COMMAND-NAME} start --help\" for the available options when starting the server.",
                "Use \"${COMMAND-NAME} <command> --help\" for more information about other commands."
        },
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
        })
public final class Main {

    public static final String PROFILE_SHORT_NAME = "-pf";
    public static final String PROFILE_LONG_NAME = "--profile";
    public static final String CONFIG_FILE_SHORT_NAME = "-cf";
    public static final String CONFIG_FILE_LONG_NAME = "--config-file";

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Option(names = { "-h", "--help" },
            description = "This help message.",
            usageHelp = true)
    boolean help;

    @Option(names = { "-V", "--version" },
            description = "Show version information",
            versionHelp = true)
    boolean version;

    @Option(names = { "-v", "--verbose" },
            description = "Print out error details when running this command.",
            paramLabel = NO_PARAM_LABEL)
    public void setVerbose(boolean verbose) {
        ExecutionExceptionHandler exceptionHandler = (ExecutionExceptionHandler) spec.commandLine().getExecutionExceptionHandler();
        exceptionHandler.setVerbose(verbose);
    }

    @Option(names = { PROFILE_SHORT_NAME, PROFILE_LONG_NAME },
            hidden = true,
            description = "Set the profile. Use 'dev' profile to enable development mode.")
    public void setProfile(String profile) {
        Environment.setProfile(profile);
    }

    @Option(names = { CONFIG_FILE_SHORT_NAME, CONFIG_FILE_LONG_NAME },
            arity = "1",
            description = "Set the path to a configuration file. By default, configuration properties are read from the \"keycloak.conf\" file in the \"conf\" directory.",
            paramLabel = "file")
    public void setConfigFile(String path) {
        System.setProperty(KeycloakPropertiesConfigSource.KEYCLOAK_CONFIG_FILE_PROP, path);
    }
}