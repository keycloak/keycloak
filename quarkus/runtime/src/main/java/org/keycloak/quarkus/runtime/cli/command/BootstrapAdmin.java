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

package org.keycloak.quarkus.runtime.cli.command;

import org.keycloak.quarkus.runtime.cli.Picocli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = BootstrapAdmin.NAME, header = BootstrapAdmin.HEADER, description = "%n"
        + BootstrapAdmin.HEADER, subcommands = {BootstrapAdminUser.class, BootstrapAdminService.class})
public class BootstrapAdmin {

    public static final String NAME = "bootstrap-admin";
    public static final String HEADER = "Commands for bootstrapping admin access";
    public static final String KEYCLOAK_BOOTSTRAP_ADMIN_EXPIRATION_ENV_VAR = "KEYCLOAK_BOOTSTRAP_ADMIN_EXPIRATION";

    @Option(arity = "0", paramLabel = Picocli.NO_PARAM_LABEL, names = { "--no-prompt" }, description = "Run non-interactive without prompting", scope = ScopeType.INHERIT)
    boolean noPrompt;

    /*@Option(names = {
            "--expiration" }, description = "Specifies the number of minutes after which the account expires. Defaults to "
                    + ApplianceBootstrap.DEFAULT_TEMP_ADMIN_EXPIRATION + ", or to the value of the "
                    + KEYCLOAK_BOOTSTRAP_ADMIN_EXPIRATION_ENV_VAR + " env variable if set", defaultValue = "${env:"
                            + KEYCLOAK_BOOTSTRAP_ADMIN_EXPIRATION_ENV_VAR + "}", scope = ScopeType.INHERIT)
    */Integer expiration;

}
