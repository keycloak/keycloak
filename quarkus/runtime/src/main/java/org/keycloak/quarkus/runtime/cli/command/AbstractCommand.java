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

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Option;

public abstract class AbstractCommand {

    @Spec
    protected CommandSpec spec;

    @Option(names = { "-h", "--help" },
            description = "This help message.",
            usageHelp = true)
    boolean help;

    @Option(names = {"-pf", "--profile"},
            description = "Set the profile. Use 'dev' profile to enable development mode.")
    public void setProfile(String profile) {
        Environment.setProfile(profile);
    }

    @Option(names = { "-cf", "--config-file" },
            arity = "1",
            description = "Set the path to a configuration file. By default, configuration properties are read from the \"keycloak.properties\" file in the \"conf\" directory.",
            paramLabel = "file",
            scope = CommandLine.ScopeType.INHERIT)
    public void setConfigFile(String path) {
        System.setProperty(KeycloakConfigSourceProvider.KEYCLOAK_CONFIG_FILE_PROP, path);
    }
}
