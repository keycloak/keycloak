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

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "tools",
        description = "%nUtilities for use and interaction with the server.",
        abbreviateSynopsis = true,
        commandListHeading = "Commands:",
        optionListHeading = "Options:",
        subcommands = {Completion.class})
public class Tools {

    @CommandLine.Option(names = { "-h", "--help" },
            description = "This help message.",
            usageHelp = true)
    boolean help;
}
