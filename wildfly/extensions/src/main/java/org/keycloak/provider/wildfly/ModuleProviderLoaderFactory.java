/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider.wildfly;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.keycloak.provider.DefaultProviderLoader;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderLoader;
import org.keycloak.provider.ProviderLoaderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ModuleProviderLoaderFactory implements ProviderLoaderFactory {

    @Override
    public boolean supports(String type) {
        return "module".equals(type);
    }

    @Override
    public ProviderLoader create(KeycloakDeploymentInfo info, ClassLoader baseClassLoader, String resource) {
        try {
            Module module = Module.getContextModuleLoader().loadModule(ModuleIdentifier.fromString(resource));
            ModuleClassLoader classLoader = module.getClassLoader();
            return new DefaultProviderLoader(info, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
