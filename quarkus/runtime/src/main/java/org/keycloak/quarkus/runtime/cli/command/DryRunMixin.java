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

import static org.keycloak.quarkus.runtime.cli.Picocli.NO_PARAM_LABEL;

public final class DryRunMixin {
    
    public static final String DRYRUN_OPTION_LONG = "--dry-run";
    public static final String KC_DRY_RUN_ENV = "KC_DRY_RUN";
    public static final String KC_DRY_RUN_BUILD_ENV = "KC_DRY_RUN_BUILD";

    @CommandLine.Option(names = {DRYRUN_OPTION_LONG},
            description = "Use this option to validate the command, but do nothing",
            paramLabel = NO_PARAM_LABEL,
            hidden = true,
            defaultValue = "${env:" + KC_DRY_RUN_ENV + "}")
    Boolean dryRun;
    
    public static boolean isDryRunBuild() {
        return Boolean.valueOf(System.getenv().get(KC_DRY_RUN_BUILD_ENV));
    }

}
