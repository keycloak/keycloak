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

package org.keycloak.quarkus.runtime.integration.jaxrs;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.platform.Platform;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.integration.QuarkusPlatform;
import org.keycloak.services.resources.KeycloakApplication;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/")
@Blocking
public class QuarkusKeycloakApplication extends KeycloakApplication {

    void onStartupEvent(@Observes StartupEvent event) {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        platform.started();
        startup();
    }

    void onShutdownEvent(@Observes ShutdownEvent event) {
        shutdown();
    }

    @Override
    public KeycloakSessionFactory createSessionFactory() {
        QuarkusKeycloakSessionFactory instance = QuarkusKeycloakSessionFactory.getInstance();
        instance.init();
        return instance;
    }

    @Override
    protected void loadConfig() {
        // no need to load config provider because we force quarkus impl
    }

}
