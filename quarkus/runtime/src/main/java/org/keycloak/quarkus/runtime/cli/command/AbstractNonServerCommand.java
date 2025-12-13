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

import java.util.EnumSet;

import org.keycloak.common.util.Environment;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.integration.jaxrs.QuarkusKeycloakApplication;

import picocli.CommandLine;

public abstract class AbstractNonServerCommand extends AbstractAutoBuildCommand {

    @CommandLine.Mixin
    OptimizedMixin optimizedMixin = new OptimizedMixin();

    private static EnumSet<OptionCategory> hidden = EnumSet.of(OptionCategory.HTTP, OptionCategory.HTTP_ACCESS_LOG,
            OptionCategory.PROXY, OptionCategory.HOSTNAME_V1, OptionCategory.HOSTNAME_V2, OptionCategory.METRICS,
            OptionCategory.SECURITY, OptionCategory.CACHE, OptionCategory.HEALTH, OptionCategory.MANAGEMENT);

    @Override
    public String getDefaultProfile() {
        return Environment.NON_SERVER_MODE;
    }

    @Override
    public boolean isHiddenCategory(OptionCategory category) {
        return super.isHiddenCategory(category) || hidden.contains(category);
    }

    public void onStart(QuarkusKeycloakApplication application) {
    }

    @Override
    protected OptimizedMixin getOptimizedMixin() {
        return optimizedMixin;
    }

}
