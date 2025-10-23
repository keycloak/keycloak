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

package org.keycloak.quarkus.runtime.cli.command;

import org.keycloak.quarkus.runtime.cli.Help;

import picocli.CommandLine;

public final class HelpAllMixin {

    public static final String HELP_ALL_OPTION = "--help-all";

    @CommandLine.Option(names = {HELP_ALL_OPTION}, usageHelp = true, description = "This same help message but with additional options.")
    public void setHelpAll(boolean allOptions) {
        Help.setAllOptions(true);
    }
}
