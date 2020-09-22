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
import java.util.List;
import java.util.function.IntFunction;

import org.keycloak.common.Profile;
import org.keycloak.configuration.PropertyMapper;
import org.keycloak.configuration.PropertyMappers;
import org.keycloak.util.Environment;
import picocli.CommandLine;

final class Picocli {

    static CommandLine.Model.CommandSpec createCommandSpec() {
        CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new MainCommand())
                .name(Environment.getCommand());

        addOption(spec, "start", PropertyMappers.getRuntimeMappers());
        addOption(spec, "config", PropertyMappers.getRuntimeMappers());
        addOption(spec, "config", PropertyMappers.getBuiltTimeMappers());
        addOption(spec.subcommands().get("config").getCommandSpec(), "--features", "Enables a group of features. Possible values are: "
                + String.join(",", Arrays.asList(Profile.Type.values()).stream().map(
                type -> type.name().toLowerCase()).toArray((IntFunction<CharSequence[]>) String[]::new)));

        for (Profile.Feature feature : Profile.Feature.values()) {
            addOption(spec.subcommands().get("config").getCommandSpec(), "--features-" + feature.name().toLowerCase(),
                    "Enables the " + feature.name() + " feature. Set enabled to enable the feature or disabled otherwise.");
        }
        return spec;
    }

    static String parseConfigArgs(List<String> argsList) {
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

    private static void addOption(CommandLine.Model.CommandSpec spec, String command, List<PropertyMapper> mappers) {
        CommandLine.Model.CommandSpec commandSpec = spec.subcommands().get(command).getCommandSpec();

        for (PropertyMapper mapper : mappers) {
            String name = "--" + PropertyMappers.toCLIFormat(mapper.getFrom()).substring(3);
            String description = mapper.getDescription();

            if (description == null || commandSpec.optionsMap().containsKey(name)) {
                continue;
            }

            addOption(commandSpec, name, description);
        }
    }

    private static void addOption(CommandLine.Model.CommandSpec commandSpec, String name, String description) {
        commandSpec.addOption(CommandLine.Model.OptionSpec.builder(name)
                .description(description)
                .paramLabel("<value>")
                .type(String.class).build());
    }
}
