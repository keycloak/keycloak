/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.cli;

import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_PREFIX;

import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.utils.StringUtil;

import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

public final class PropertyMapperParameterConsumer implements CommandLine.IParameterConsumer {

    static final CommandLine.IParameterConsumer INSTANCE = new PropertyMapperParameterConsumer();

    private PropertyMapperParameterConsumer() {
        // singleton
    }

    @Override
    public void consumeParameters(Stack<String> args, ArgSpec argSpec,
            CommandSpec commandSpec) {
        if (argSpec instanceof OptionSpec) {
            validateOption(args, argSpec, commandSpec);
        }
    }

    private void validateOption(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
        OptionSpec option = (OptionSpec) argSpec;
        String name = String.join(", ", option.names());
        CommandLine commandLine = commandSpec.commandLine();

        if (args.isEmpty() || !isOptionValue(args.peek())) {
            throw new ParameterException(
                    commandLine, "Missing required value for option '" + name + "' (" + argSpec.paramLabel() + ")." + getExpectedValuesMessage(argSpec, option));
        }

        // consumes the value
        String value = args.pop();

        if (!args.isEmpty() && isOptionValue(args.peek())) {
            throw new ParameterException(
                    commandLine, "Option '" + name + "' expects a single value (" + argSpec.paramLabel() + ")" + getExpectedValuesMessage(argSpec, option));
        }

        if (isExpectedValue(option, value)) {
            return;
        }

        throw new ParameterException(
                commandLine, "Invalid value for option '" + name + "': " + value + "." + getExpectedValuesMessage(argSpec, option));
    }

    private boolean isOptionValue(String arg) {
        return !(arg.startsWith(ARG_PREFIX) || arg.startsWith(Picocli.ARG_SHORT_PREFIX));
    }

    private String getExpectedValuesMessage(ArgSpec argSpec, OptionSpec option) {
        return option.completionCandidates().iterator().hasNext() ? " Expected values are: " + String.join(", ", argSpec.completionCandidates()) : "";
    }

    private boolean isExpectedValue(OptionSpec option, String value) {
        List<String> expectedValues = StreamSupport.stream(option.completionCandidates().spliterator(), false).collect(Collectors.toList());

        if (expectedValues.isEmpty()) {
            // accept any
            return true;
        }

        if (StringUtil.isBlank(value)) {
            return false;
        }

        for (String v : value.split(",")) {
            if (!expectedValues.contains(v)) {
                return false;
            }
        }

        return true;
    }
}
