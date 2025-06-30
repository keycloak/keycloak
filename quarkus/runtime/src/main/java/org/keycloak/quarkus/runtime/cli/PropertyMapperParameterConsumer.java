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

import java.util.Stack;

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
            throw new ParameterException(commandLine,
                    "Missing required value. " + getExpectedMessage(argSpec, option, name));
        }

        // consumes the value, actual value validation will be performed later
        args.pop();

        if (!args.isEmpty() && isOptionValue(args.peek())) {
            throw new ParameterException(commandLine, getExpectedMessage(argSpec, option, name));
        }
    }

    private String getExpectedMessage(ArgSpec argSpec, OptionSpec option, String name) {
        return String.format("Option '%s' (%s) expects %s.%s", name, argSpec.paramLabel(),
                option.typeInfo().isMultiValue() ? "one or more comma separated values without whitespace": "a single value",
                getExpectedValuesMessage(argSpec.completionCandidates(), option.completionCandidates()));
    }

    private boolean isOptionValue(String arg) {
        return !(arg.startsWith(ARG_PREFIX) || arg.startsWith(Picocli.ARG_SHORT_PREFIX));
    }

    public static String getExpectedValuesMessage(Iterable<String> specCandidates, Iterable<String> optionCandidates) {
        return optionCandidates.iterator().hasNext() ? " Expected values are: " + String.join(", ", specCandidates) : "";
    }

}
