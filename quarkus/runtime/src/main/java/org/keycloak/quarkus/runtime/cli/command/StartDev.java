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

import picocli.CommandLine;

@CommandLine.Command(name = StartDev.NAME,
        description = "Start the server in development mode.",
        mixinStandardHelpOptions = true,
        optionListHeading = "%nOptions%n",
        parameterListHeading = "Available Commands%n")
public final class StartDev extends AbstractStartCommand implements Runnable {

    public static final String NAME = "start-dev";

    @Override
    protected void doBeforeRun() {
        Environment.forceDevProfile();
        spec.commandLine().getOut().printf("Running the server in dev mode. DO NOT run the '%s' command in production.%n", NAME);
    }
}
