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

import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.configuration.mappers.HostnameV2PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;

import static org.keycloak.quarkus.runtime.Environment.isDevProfile;

public abstract class AbstractStartCommand extends AbstractCommand implements Runnable {
    public static final String OPTIMIZED_BUILD_OPTION_LONG = "--optimized";

    @CommandLine.Mixin
    DryRunMixin dryRunMixin = new DryRunMixin();

    @Override
    public void run() {
        doBeforeRun();
        validateConfig();

        if (isDevProfile()) {
            picocli.getOutWriter().println(Ansi.AUTO.string(
                    "@|bold,red Running the server in development mode. DO NOT use this configuration in production.|@"));
        }
        if (!Boolean.TRUE.equals(dryRunMixin.dryRun)) {
            picocli.start();
        }
    }

    protected void doBeforeRun() {

    }

    @Override
    protected void validateConfig() {
        super.validateConfig(); // we want to run the generic validation here first to check for unknown options
        HttpPropertyMappers.validateConfig();
        HostnameV2PropertyMappers.validateConfig(picocli);
    }

    @Override
    public List<OptionCategory> getOptionCategories() {
        EnumSet<OptionCategory> excludedCategories = excludedCategories();
        return super.getOptionCategories().stream().filter(optionCategory -> !excludedCategories.contains(optionCategory)).collect(Collectors.toList());
    }

    protected EnumSet<OptionCategory> excludedCategories() {
        return EnumSet.of(OptionCategory.IMPORT, OptionCategory.EXPORT);
    }

}
