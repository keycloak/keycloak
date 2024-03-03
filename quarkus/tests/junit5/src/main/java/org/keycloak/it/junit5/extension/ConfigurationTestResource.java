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

package org.keycloak.it.junit5.extension;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;

import java.util.Map;

/**
 * Used to reset the configuration for {@link CLITest}s
 */
public class ConfigurationTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return Map.of();
    }

    @Override
    public void stop() {

    }

    @Override
    public void inject(Object testInstance) {
        ConfigArgsConfigSource.setCliArgs(CLITestExtension.CLI_ARGS);
        KeycloakConfigSourceProvider.reload();
        SmallRyeConfig config = ConfigUtils.configBuilder(true, LaunchMode.NORMAL).build();
        SmallRyeConfigProviderResolver resolver = new SmallRyeConfigProviderResolver();
        resolver.registerConfig(config, Thread.currentThread().getContextClassLoader());
        ConfigProviderResolver.setInstance(resolver);
    }

}
