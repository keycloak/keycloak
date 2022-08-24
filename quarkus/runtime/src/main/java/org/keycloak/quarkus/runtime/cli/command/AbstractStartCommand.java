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

import org.keycloak.quarkus.runtime.KeycloakMain;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;

import picocli.CommandLine;

public abstract class AbstractStartCommand extends AbstractCommand implements Runnable {

    // remove this once auto-build is removed
    public static final String AUTO_BUILD_OPTION_LONG = "--auto-build";
    public static final String AUTO_BUILD_OPTION_SHORT = "-b";
    public static final String OPTIMIZED_BUILD_OPTION_LONG = "--optimized";
    public static final String DEFAULT_WARN_MESSAGE_REPEATED_AUTO_BUILD_OPTION = "WARNING: The '" + AUTO_BUILD_OPTION_LONG + "' option for 'start' command is DEPRECATED and no longer needed. When executing the '" + Start.NAME + "' command, a new server image is automatically built based on the configuration. If you want to disable this behavior and achieve an optimal startup time, use the '" + OPTIMIZED_BUILD_OPTION_LONG + "' option instead.";

    @Override
    public void run() {
        doBeforeRun();
        CommandLine cmd = spec.commandLine();
        KeycloakMain.start((ExecutionExceptionHandler) cmd.getExecutionExceptionHandler(), cmd.getErr(), cmd.getParseResult().originalArgs().toArray(new String[0]));
    }

    protected void doBeforeRun() {

    }
}
