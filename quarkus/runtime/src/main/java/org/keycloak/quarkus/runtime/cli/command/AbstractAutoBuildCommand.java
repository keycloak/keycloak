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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import picocli.CommandLine;

import static org.keycloak.quarkus.runtime.Environment.isDevMode;
import static org.keycloak.quarkus.runtime.Environment.isDevProfile;
import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;

public abstract class AbstractAutoBuildCommand extends AbstractCommand {

    public static final String OPTIMIZED_BUILD_OPTION_LONG = "--optimized";

    public static final int FEATURE_DISABLED_EXIT_CODE = 4;
    public static final int REBUILT_EXIT_CODE = 10;

    @CommandLine.Mixin
    DryRunMixin dryRunMixin = new DryRunMixin();

    @CommandLine.Mixin
    HelpAllMixin helpAllMixin;

    @Override
    protected Optional<Integer> callCommand() {
        if (isRebuildCheck()) {
            if (requiresReAugmentation()) {
                runReAugmentation();
                return Optional.of(REBUILT_EXIT_CODE);
            }
            // clear the check, and change to the command runtime profile
            String profile = org.keycloak.common.util.Environment.getProfile();
            Environment.setRebuildCheck(false);
            String runtimeProfile = getInitProfile();
            if (!Objects.equals(profile, runtimeProfile)) {
                Environment.setProfile(runtimeProfile);
                Configuration.resetConfig();
            }
        }
        return Optional.empty();
    }

    static boolean requiresReAugmentation() {
        Map<String, String> rawPersistedProperties = Configuration.getRawPersistedProperties();
        if (rawPersistedProperties.isEmpty()) {
            return true; // no build yet
        }
        var current = Picocli.getNonPersistedBuildTimeOptions();

        // everything but the optimized value must match
        String key = Configuration.KC_OPTIMIZED;
        Optional.ofNullable(rawPersistedProperties.get(key)).ifPresentOrElse(value -> current.put(key, value), () -> current.remove(key));
        return !rawPersistedProperties.equals(current);
    }

    private void runReAugmentation() {
        if(!isDevMode()) {
            spec.commandLine().getOut().println("Changes detected in configuration. Updating the server image.");
            if (Configuration.isOptimized()) {
                picocli.checkChangesInBuildOptionsDuringAutoBuild(spec.commandLine().getOut());
            }
        }

        directBuild();

        if(!isDevMode()) {
            spec.commandLine().getOut().printf("Next time you run the server, just add %s to the command to ensure this build is used.\n", OPTIMIZED_BUILD_OPTION_LONG);
        }
    }

    void directBuild() {
        Build build = new Build();
        build.dryRunMixin = this.dryRunMixin;
        build.setPicocli(picocli);
        build.spec = spec;
        build.runCommand();
    }

    @Override
    protected void runCommand() {
        doBeforeRun();
        validateConfig();

        if (isDevProfile()) {
            picocli.getOutWriter().println(picocli.getColorMode().string(
                    "@|bold,red Running the server in development mode. DO NOT use this configuration in production.|@"));
        }
        if (shouldStart() && !Boolean.TRUE.equals(dryRunMixin.dryRun)) {
            picocli.start();
        }
    }

    protected void doBeforeRun() {

    }

    @Override
    public boolean isHelpAll() {
        return helpAllMixin != null ? helpAllMixin.allOptions : false;
    }

    abstract protected OptimizedMixin getOptimizedMixin();

    @Override
    public boolean isOptimized() {
        return Optional.ofNullable(getOptimizedMixin()).map(o -> o.optimized).orElse(false);
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

}
