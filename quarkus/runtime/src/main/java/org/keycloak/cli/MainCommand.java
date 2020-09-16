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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.configuration.KeycloakConfigSourceProvider;
import org.keycloak.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.KeycloakRecorder;
import org.keycloak.util.Environment;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.runtime.Quarkus;
import io.smallrye.config.ConfigValue;
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
        parameterListHeading = "Server Options%n%n", 
        optionListHeading = "%nConfiguration Options%n%n", 
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

    @CommandLine.Parameters(paramLabel = "server options", description = "Server options")
    List<String> serverOptions;

    @CommandLine.Parameters(paramLabel = "system properties", description = "Any Java system property you want set")
    List<String> systemProperties;

    @Option(names = "--profile", arity = "1", description = "Set the profile. Use 'dev' profile to enable development mode", scope = CommandLine.ScopeType.INHERIT)
    public void setProfile(String profile) {
        System.setProperty("kc.profile", profile);
    }

    @Option(names = "--config-file", arity = "1", description = "Set the path to a configuration file", paramLabel = "<path>", scope = CommandLine.ScopeType.INHERIT)
    public void setConfigFile(String path) {
        System.setProperty(KeycloakConfigSourceProvider.KEYCLOAK_CONFIG_FILE_PROP, path);
    }

    @Command(name = "config", description = "Update the server configuration", mixinStandardHelpOptions = true, usageHelpAutoWidth = true)
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

    @Command(name = "start-dev", description = "Start the server in development mode", mixinStandardHelpOptions = true)
    public void startDev() {
        System.setProperty("kc.profile", "dev");
        start();
    }
    
    @Command(name = "start", description = "Start the server", mixinStandardHelpOptions = true, usageHelpAutoWidth = true)
    public void start(
            @CommandLine.Parameters(paramLabel = "show-config", arity = "0..1", 
                    description = "Show all configuration options before starting the server") String showConfig) {
        if (showConfig != null) {
            System.setProperty("kc.show.config.runtime", Boolean.TRUE.toString());
            System.setProperty("kc.show.config", "all");
        }
        start();
    }

    @Command(name = "show-config", description = "Print out the current configuration", mixinStandardHelpOptions = true)
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
