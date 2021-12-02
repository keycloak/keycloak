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

package org.keycloak.quarkus.runtime.integration;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.keycloak.platform.Platform;

@ApplicationScoped
public class QuarkusLifecycleObserver {

    void onStartupEvent(@Observes StartupEvent event) {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        platform.started();
        QuarkusPlatform.exitOnError();
        Runnable startupHook = platform.startupHook;

        if (startupHook != null) {
            startupHook.run();
        }
    }

    void onShutdownEvent(@Observes ShutdownEvent event) {

        Runnable shutdownHook = ((QuarkusPlatform) Platform.getPlatform()).shutdownHook;

        if (shutdownHook != null)
            shutdownHook.run();

    }
}
