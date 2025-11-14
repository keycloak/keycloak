/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration;

import java.util.List;

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import picocli.CommandLine;

import static org.keycloak.quarkus.runtime.cli.OptionRenderer.DUPLICIT_OPTION_SUFFIX;

/**
 * Custom CommandLine.UnmatchedArgumentException with amended suggestions
 */
public class KcUnmatchedArgumentException extends CommandLine.UnmatchedArgumentException {

    public KcUnmatchedArgumentException(CommandLine commandLine, List<String> args) {
        super(commandLine, args);
    }

    public KcUnmatchedArgumentException(CommandLine.UnmatchedArgumentException ex) {
        super(ex.getCommandLine(), ex.getUnmatched());
    }

    @Override
    public List<String> getSuggestions() {
        // filter out disabled mappers
        return super.getSuggestions().stream()
                .filter(f -> PropertyMappers.getKcKeyFromCliKey(f).filter(PropertyMappers::isDisabledMapper).isEmpty()
                        && !f.endsWith(DUPLICIT_OPTION_SUFFIX))
                .toList();
    }
}
