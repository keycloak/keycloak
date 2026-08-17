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

package org.keycloak.quarkus.runtime;

import java.util.Collection;
import java.util.Set;

import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;

public final class Providers {

    public static ProviderManager getProviderManager(ClassLoader classLoader) {
        return getProviderManager(classLoader, Set.of());
    }

    /**
     * @param providerFactoryClasses provider factory classes discovered at build time via the
     * {@link org.keycloak.provider.KeycloakProvider} annotation, registered in addition to
     * {@link java.util.ServiceLoader} discovery
     */
    public static ProviderManager getProviderManager(ClassLoader classLoader,
            Collection<Class<? extends ProviderFactory>> providerFactoryClasses) {
        KeycloakDeploymentInfo keycloakDeploymentInfo = KeycloakDeploymentInfo.create()
                .name("classpath")
                .services()
                .themeResources();

        providerFactoryClasses.forEach(keycloakDeploymentInfo::addProviderFactoryClass);

        return new ProviderManager(keycloakDeploymentInfo, classLoader);
    }
}
