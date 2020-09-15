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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.configuration.PropertyMapper;
import org.keycloak.configuration.PropertyMappers;
import org.keycloak.util.Environment;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain(name = "keycloak")
public class KeycloakQuarkusMain {

    public static void main(String args[]) {
        System.setProperty("kc.version", Version.VERSION_KEYCLOAK);

        if (args.length != 0) {
            CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new MainCommand())
                    .name(Environment.getCommand());
            
            addOption(spec, "start", PropertyMappers.getRuntimeMappers());
            
            addOption(spec, "config", PropertyMappers.getRuntimeMappers());
            addOption(spec, "config", PropertyMappers.getBuiltTimeMappers());
            spec.subcommands().get("config").getCommandSpec().addOption(CommandLine.Model.OptionSpec.builder("--features")
                    .description("Enables a group of features. Possible values are: " 
                            + String.join(",", Arrays.asList(Profile.Type.values()).stream().map(
                            type -> type.name().toLowerCase()).toArray((IntFunction<CharSequence[]>) String[]::new)))
                    .type(String.class)
                    .build());

            for (Profile.Feature feature : Profile.Feature.values()) {
                spec.subcommands().get("config").getCommandSpec().addOption(CommandLine.Model.OptionSpec.builder("--features-" + feature.name().toLowerCase())
                        .description("Enables the " + feature.name() + " feature. Set enabled to enable the feature or disabled otherwise.")
                        .type(String.class)
                        .build());
            }
            
            CommandLine cmd = new CommandLine(spec);
            List<String> argsList = new LinkedList<>(Arrays.asList(args));

            if (argsList.isEmpty() || argsList.get(0).startsWith("--")) {
                argsList.add(0, "start");
            }
            
            try {
                System.setProperty("kc.config.args", parseConfigArgs(argsList));
                cmd.parseArgs(argsList.toArray(new String[argsList.size()]));
            } catch (CommandLine.UnmatchedArgumentException e) {
                cmd.getErr().println(e.getMessage());
                System.exit(CommandLine.ExitCode.SOFTWARE);
            }

            int exitCode = cmd.execute(argsList.toArray(new String[argsList.size()]));

            if (exitCode != -1) {
                System.exit(exitCode);
            }
        }

        Quarkus.run(args);
        Quarkus.waitForExit();
    }

    private static String parseConfigArgs(List<String> argsList) {
        StringBuilder options = new StringBuilder();
        Iterator<String> iterator = argsList.iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();

            // TODO: ignore properties for providers for now, need to fetch them from the providers, otherwise CLI will complain about invalid options
            if (key.startsWith("--spi")) {
                iterator.remove();
            }
            
            if (key.startsWith("--")) {
                if (options.length() > 0) {
                    options.append(",");
                }
                options.append(key);       
            }
        }

        return options.toString();
    }

    public static void addOption(CommandLine.Model.CommandSpec spec, String command, List<PropertyMapper> mappers) {
        CommandLine.Model.CommandSpec commandSpec = spec.subcommands().get(command).getCommandSpec();

        for (PropertyMapper mapper : mappers) {
            String name = "--" + PropertyMappers.toCLIFormat(mapper.getFrom()).substring(3);
            String description = mapper.getDescription();

            if (description == null || commandSpec.optionsMap().containsKey(name)) {
                continue;
            }

            CommandLine.Model.OptionSpec.Builder builder = CommandLine.Model.OptionSpec.builder(name).type(String.class);

            builder.description(description);

            commandSpec.addOption(builder.build());
        }
    }
}
