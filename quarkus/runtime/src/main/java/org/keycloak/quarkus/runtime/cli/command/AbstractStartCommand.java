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
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.KeycloakMain;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.mappers.HostnameV2PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers;
import org.keycloak.url.HostnameV2ProviderFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import picocli.CommandLine;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getRawPersistedProperties;

public abstract class AbstractStartCommand extends AbstractCommand implements Runnable {
    public static final String OPTIMIZED_BUILD_OPTION_LONG = "--optimized";
    
    private boolean skipStart;

    @Override
    public void run() {
        Environment.setParsedCommand(this);
        doBeforeRun();
        CommandLine cmd = spec.commandLine();
        HttpPropertyMappers.validateConfig();
        HostnameV2PropertyMappers.validateConfig();
        validateConfig();

        if (ConfigArgsConfigSource.getAllCliArgs().contains(OPTIMIZED_BUILD_OPTION_LONG) && !wasBuildEverRun()) {
            executionError(spec.commandLine(), Messages.optimizedUsedForFirstStartup());
        }

        if (!skipStart) {
            KeycloakMain.start((ExecutionExceptionHandler) cmd.getExecutionExceptionHandler(), cmd.getErr(), cmd.getParseResult().originalArgs().toArray(new String[0]));
        }
    }

    protected void doBeforeRun() {

    }

    public static boolean wasBuildEverRun() {
        return !getRawPersistedProperties().isEmpty();
    }

    @Override
    public List<OptionCategory> getOptionCategories() {
        EnumSet<OptionCategory> excludedCategories = excludedCategories();
        return super.getOptionCategories().stream().filter(optionCategory -> !excludedCategories.contains(optionCategory)).collect(Collectors.toList());
    }

    protected EnumSet<OptionCategory> excludedCategories() {
        return EnumSet.of(OptionCategory.IMPORT, OptionCategory.EXPORT);
    }
    
    public void setSkipStart(boolean skipStart) {
        this.skipStart = skipStart;
    }

}
