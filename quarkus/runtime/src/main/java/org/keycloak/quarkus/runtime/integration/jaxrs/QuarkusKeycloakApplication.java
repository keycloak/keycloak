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

import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ApplicationPath;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.quarkus.runtime.services.resources.QuarkusWelcomeResource;
import org.keycloak.services.resources.WelcomeResource;

@ApplicationPath("/")
public class QuarkusKeycloakApplication extends KeycloakApplication {

    private static boolean filterSingletons(Object o) {
        return !WelcomeResource.class.isInstance(o);
    }

    @Override
    protected void startup() {
        QuarkusKeycloakSessionFactory instance = QuarkusKeycloakSessionFactory.getInstance();
        sessionFactory = instance;
        instance.init();
        ExportImportManager exportImportManager = bootstrap();

        if (exportImportManager.isRunExport()) {
            exportImportManager.runExport();
        }

        sessionFactory.publish(new PostMigrationEvent(sessionFactory));
    }

    @Override
    protected void loadConfig() {
        // no need to load config provider because we force quarkus impl
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = super.getSingletons().stream()
                .filter(QuarkusKeycloakApplication::filterSingletons)
                .collect(Collectors.toSet());

        singletons.add(new QuarkusWelcomeResource());

        return singletons;
    }
}
