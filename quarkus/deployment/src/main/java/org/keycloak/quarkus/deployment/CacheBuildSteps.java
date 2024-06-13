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

import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.keycloak.quarkus.runtime.KeycloakRecorder;
import org.keycloak.quarkus.runtime.storage.legacy.infinispan.CacheManagerFactory;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

public class CacheBuildSteps {

    @Consume(ConfigBuildItem.class)
    // Consume LoggingSetupBuildItem.class and record RUNTIME_INIT are necessary to ensure that logging is set up before the caches are initialized.
    // This is to prevent the class TP in JGroups to pick up the trace logging at start up. While the logs will not appear on the console,
    // they will still be created and use CPU cycles and create garbage collection.
    // See: https://issues.redhat.com/browse/JGRP-2130 for the JGroups discussion, and https://github.com/keycloak/keycloak/issues/29129 for the issue Keycloak had with this.
    @Consume(LoggingSetupBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureInfinispan(KeycloakRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItems, ShutdownContextBuildItem shutdownContext) {
        syntheticBeanBuildItems.produce(SyntheticBeanBuildItem.configure(CacheManagerFactory.class)
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit()
                .runtimeValue(recorder.createCacheInitializer(shutdownContext)).done());
    }
}
