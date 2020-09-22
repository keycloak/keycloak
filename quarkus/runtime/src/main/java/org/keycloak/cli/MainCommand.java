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

import org.keycloak.configuration.KeycloakConfigSourceProvider;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.runtime.Quarkus;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "keycloak", 
        usageHelpWidth = 150, 
        header = "Keycloak - Open Source Identity and Access Management\n\nFind more information at: https://www.keycloak.org/%n", 
        description = "Use this command-line tool to manage your Keycloak cluster%n", footerHeading = "%nUse \"${COMMAND-NAME} <command> --help\" for more information about a command.%nUse \"${COMMAND-NAME} options\" for a list of all command-line options.", 
        footer = "%nby Red Hat", 
        optionListHeading = "Configuration Options%n%n", 
        commandListHeading = "%nCommands%n%n", 
        version = {
        "Keycloak ${sys:kc.version}",
        "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
        "OS: ${os.name} ${os.version} ${os.arch}"
})
public class MainCommand {

    @Spec
    CommandSpec spec;

    @Option(names = { "--help" }, usageHelp = true, hidden = true)
    boolean help;

    @Option(names = { "--version" }, versionHelp = true, hidden = true)
    boolean version;

    @Option(names = "--profile", arity = "1", description = "Set the profile. Use 'dev' profile to enable development mode.", scope = CommandLine.ScopeType.INHERIT)
    public void setProfile(String profile) {
        System.setProperty("kc.profile", profile);
    }

    @Option(names = "--config-file", arity = "1", description = "Set the path to a configuration file.", paramLabel = "<path>", scope = CommandLine.ScopeType.INHERIT)
    public void setConfigFile(String path) {
        System.setProperty(KeycloakConfigSourceProvider.KEYCLOAK_CONFIG_FILE_PROP, path);
    }

    @Command(name = "config", 
            description = "%nCreates a new server image based on the options passed to this command. Once created, configuration will be read from the server image and the server can be started without passing the same options again. Some configuration options require this command to be executed in order to actually change a configuration. For instance, the database vendor.%n", 
            mixinStandardHelpOptions = true, 
            usageHelpAutoWidth = true,
            optionListHeading = "%nOptions%n",
            parameterListHeading = "Available Commands%n")
    public void reAugment() {
        System.setProperty("quarkus.launch.rebuild", "true");
        println("Updating the configuration and installing your custom providers, if any. Please wait.");
        try {
            QuarkusEntryPoint.main();
        } catch (Throwable throwable) {
            error("Failed to update server configuration.");
        } finally {
            System.exit(CommandLine.ExitCode.OK);
        }
    }

    @Command(name = "start-dev", 
            description = "%nStart the server in development mode.%n", 
            mixinStandardHelpOptions = true,
            optionListHeading = "%nOptions%n",
            parameterListHeading = "Available Commands%n")
    public void startDev() {
        System.setProperty("kc.profile", "dev");
        start();
    }

    @Command(name = "export", 
            description = "%nExport data from realms to a file or directory.%n", 
            mixinStandardHelpOptions = true, 
            showDefaultValues = true,
            optionListHeading = "%nOptions%n",
            parameterListHeading = "Available Commands%n")
    public void runExport(@Option(names = "--dir", arity = "1", description = "Set the path to a directory where files will be created with the exported data.", paramLabel = "<path>") String toDir,
            @Option(names = "--file", arity = "1", description = "Set the path to a file that will be created with the exported data.", paramLabel = "<path>") String toFile,
            @Option(names = "--realm", arity = "1", description = "Set the name of the realm to export", paramLabel = "<realm>") String realm,
            @Option(names = "--users", arity = "1", description = "Set how users should be exported. Possible values are: skip, realm_file, same_file, different_files.", paramLabel = "<strategy>", defaultValue = "different_files") String users,
            @Option(names = "--users-per-file", arity = "1", description = "Set the number of users per file. It’s used only if --users=different_files.", paramLabel = "<number>", defaultValue = "50") Integer usersPerFile) {
        System.setProperty("keycloak.migration.action", "export");

        if (toDir != null) {
            System.setProperty("keycloak.migration.provider", "dir");
            System.setProperty("keycloak.migration.dir", toDir);
        } else if (toFile != null) {
            System.setProperty("keycloak.migration.provider", "singleFile");
            System.setProperty("keycloak.migration.file", toFile);
        } else {
            error("Must specify either --dir or --file options.");
        }

        System.setProperty("keycloak.migration.usersExportStrategy", users.toUpperCase());
        
        if (usersPerFile != null) {
            System.setProperty("keycloak.migration.usersPerFile", usersPerFile.toString());
        }
        
        if (realm != null) {
            System.setProperty("keycloak.migration.realmName", realm);
        }
        start();
    }

    @Command(name = "import", 
            description = "%nImport data from a directory or a file.%n", 
            mixinStandardHelpOptions = true, 
            showDefaultValues = true,
            optionListHeading = "%nOptions%n",
            parameterListHeading = "Available Commands%n")
    public void runImport(@Option(names = "--dir", arity = "1", description = "Set the path to a directory containing the files with the data to import", paramLabel = "<path>") String toDir,
            @Option(names = "--file", arity = "1", description = "Set the path to a file with the data to import.", paramLabel = "<path>") String toFile,
            @Option(names = "--realm", arity = "1", description = "Set the name of the realm to import", paramLabel = "<realm>") String realm,
            @Option(names = "--override", arity = "1", description = "Set if existing data should be skipped or overridden.", paramLabel = "false", defaultValue = "true") boolean override) {
        System.setProperty("keycloak.migration.action", "import");
        if (toDir != null) {
            System.setProperty("keycloak.migration.provider", "dir");
            System.setProperty("keycloak.migration.dir", toDir);
        } else if (toFile != null) {
            System.setProperty("keycloak.migration.provider", "singleFile");
            System.setProperty("keycloak.migration.file", toFile);
        } else {
            error("Must specify either --dir or --file options.");
        }

        if (realm != null) {
            System.setProperty("keycloak.migration.realmName", realm);
        }

        System.setProperty("keycloak.migration.strategy", override ? "OVERWRITE_EXISTING" : "IGNORE_EXISTING");
        
        start();
    }
    
    @Command(name = "start", 
            description = "%nStart the server.%n", 
            mixinStandardHelpOptions = true, 
            usageHelpAutoWidth = true,
            optionListHeading = "%nOptions%n",
            parameterListHeading = "Available Commands%n")
    public void start(
            @CommandLine.Parameters(paramLabel = "show-config", arity = "0..1", 
                    description = "Print out the configuration options when starting the server.") String showConfig) {
        if (showConfig != null) {
            System.setProperty("kc.show.config.runtime", Boolean.TRUE.toString());
            System.setProperty("kc.show.config", "all");
        }
        start();
    }

    @Command(name = "show-config", 
            description = "Print out the current configuration.", 
            mixinStandardHelpOptions = true,
            optionListHeading = "%nOptions%n",
            parameterListHeading = "Available Commands%n")
    public void showConfiguration(
            @CommandLine.Parameters(paramLabel = "filter", defaultValue = "none", description = "Show all configuration options. Use 'all' to show all options.") String filter) {
        System.setProperty("kc.show.config", filter);
        start();
    }

    private void start() {
        Quarkus.run();
        Quarkus.waitForExit();
    }

    private void println(String message) {
        spec.commandLine().getOut().println(message);
    }

    private void error(String message) {
        spec.commandLine().getErr().println(message);
        System.exit(CommandLine.ExitCode.SOFTWARE);
    }
}
