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

package org.keycloak.services.resteasy;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.services.DefaultKeycloakSessionFactory;

public class ResteasyKeycloakSessionFactory extends DefaultKeycloakSessionFactory {

    @Override
    public void init() {
        ProviderManager pm = new ProviderManager(KeycloakDeploymentInfo.create().services(), getClass().getClassLoader(), Config.scope().getArray("providers"));
        for (Spi spi : pm.loadSpis()) {
            if (spi.isEnabled()) {
                spis.add(spi);
            }
        }

        factoriesMap = loadFactories(pm);

        checkProvider();
        super.init();
    }

    @Override
    public KeycloakSession create() {
        return new ResteasyKeycloakSession(this);
    }

}
