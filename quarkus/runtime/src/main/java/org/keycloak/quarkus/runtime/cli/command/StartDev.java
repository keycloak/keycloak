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

import org.keycloak.common.util.Environment;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = StartDev.NAME,
        header = "Start the server in development mode.",
        description = {
            "%nUse this command if you want to run the server locally for development or testing purposes.",
        },
        footer = "%nDo NOT start the server using this command when deploying to production.%n%n"
                + "Use '${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --help-all' to list all available options, including build options.")
public final class StartDev extends AbstractAutoBuildCommand {

    public static final String NAME = "start-dev";

    @CommandLine.Mixin
    ImportRealmMixin importRealmMixin;

    @Override
    public String getInitProfile() {
        return Environment.DEV_PROFILE_VALUE; // only ever dev - could be a validation instead
    }

    @Override
    public String getDefaultProfile() {
        return Environment.DEV_PROFILE_VALUE;
    }

    @Override
    public boolean includeRuntime() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isServing() {
        return true;
    }
}
