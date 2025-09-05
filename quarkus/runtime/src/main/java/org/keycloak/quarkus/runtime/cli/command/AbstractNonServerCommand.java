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
import org.keycloak.quarkus.runtime.integration.jaxrs.QuarkusKeycloakApplication;

import picocli.CommandLine;

import java.util.EnumSet;

public abstract class AbstractNonServerCommand extends AbstractStartCommand {

    @CommandLine.Mixin
    OptimizedMixin optimizedMixin = new OptimizedMixin();

    private static EnumSet<OptionCategory> excluded = EnumSet.of(OptionCategory.HTTP, OptionCategory.HTTP_ACCESS_LOG,
            OptionCategory.PROXY, OptionCategory.HOSTNAME_V1, OptionCategory.HOSTNAME_V2, OptionCategory.METRICS,
            OptionCategory.SECURITY, OptionCategory.CACHE, OptionCategory.HEALTH, OptionCategory.MANAGEMENT,
            OptionCategory.TRUSTSTORE);

    @Override
    public String getDefaultProfile() {
        return Environment.NON_SERVER_MODE;
    }

    @Override
    public boolean isExcludedCategory(OptionCategory category) {
        return super.isExcludedCategory(category) || excluded.contains(category);
    }

    @Override
    public boolean includeRuntime() {
        return true;
    }

    public void onStart(QuarkusKeycloakApplication application) {
    }

    @Override
    OptimizedMixin getOptimizedMixin() {
        return optimizedMixin;
    }
}
