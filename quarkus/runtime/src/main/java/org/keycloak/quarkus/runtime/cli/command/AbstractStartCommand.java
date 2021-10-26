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

import picocli.CommandLine;
import picocli.CommandLine.Option;

public abstract class AbstractStartCommand extends AbstractCommand implements Runnable {

    public static final String AUTO_BUILD_OPTION = "--auto-build";

    @Option(names = AUTO_BUILD_OPTION,
            description = "Automatically detects whether the server configuration changed and a new server image must be built" +
                    " prior to starting the server. This option provides an alternative to manually running the '" + Build.NAME + "'" +
                    " prior to starting the server. Use this configuration carefully in production as it might impact the startup time.",
            order = 1)
    Boolean autoConfig;

    @Override
    public void run() {
        doBeforeRun();
        CommandLine cmd = spec.commandLine();
        KeycloakMain.start(cmd.getParseResult().expandedArgs(), cmd.getErr());
    }

    protected void doBeforeRun() {

    }
}
