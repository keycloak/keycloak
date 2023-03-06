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

import static org.keycloak.quarkus.runtime.Messages.cliExecutionError;

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class AbstractCommand {
    private static final PropertyMapper<?>[] EMPTY_MAPPERS = new PropertyMapper<?>[]{};

    @Spec
    protected CommandSpec spec;

    protected void executionError(CommandLine cmd, String message) {
        executionError(cmd, message, null);
    }

    protected void executionError(CommandLine cmd, String message, Throwable cause) {
        cliExecutionError(cmd, message, cause);
    }

    /**
     * Get property mappers associated to particular command
     *
     * @return array of property mappers
     */
    public PropertyMapper<?>[] getMappers() {
        return EMPTY_MAPPERS;
    }
}
