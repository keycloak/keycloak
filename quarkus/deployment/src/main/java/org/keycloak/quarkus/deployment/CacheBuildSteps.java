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

package org.keycloak.quarkus.deployment;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfigValue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.infinispan.commons.util.FileLookupFactory;
import org.keycloak.quarkus.runtime.KeycloakRecorder;
import org.keycloak.quarkus.runtime.storage.legacy.infinispan.CacheManagerFactory;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

public class CacheBuildSteps {

    @Consume(KeycloakSessionFactoryPreInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsLegacyStoreEnabled.class)
    void configureInfinispan(KeycloakRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItems, ShutdownContextBuildItem shutdownContext) {
        String configFile = getConfigValue("kc.spi-connections-infinispan-quarkus-config-file").getValue();

        if (configFile != null) {
            Path configPath = Paths.get(configFile);
            String path;

            if (configPath.toFile().exists()) {
                path = configPath.toFile().getAbsolutePath();
            } else {
                path = configPath.getFileName().toString();
            }

            InputStream url = FileLookupFactory.newInstance().lookupFile(path, KeycloakProcessor.class.getClassLoader());

            if (url == null) {
                throw new IllegalArgumentException("Could not load cluster configuration file at [" + configPath + "]");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url))) {
                String config = reader.lines().collect(Collectors.joining("\n"));

                syntheticBeanBuildItems.produce(SyntheticBeanBuildItem.configure(CacheManagerFactory.class)
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .setRuntimeInit()
                        .runtimeValue(recorder.createCacheInitializer(config, shutdownContext)).done());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to read clustering configuration from [" + url + "]", cause);
            }
        } else {
            throw new IllegalArgumentException("Option 'configFile' needs to be specified");
        }
    }

}
