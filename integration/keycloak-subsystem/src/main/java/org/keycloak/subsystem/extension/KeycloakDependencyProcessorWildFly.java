/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.subsystem.extension;

import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Add platform-specific modules for WildFly.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class KeycloakDependencyProcessorWildFly extends KeycloakDependencyProcessor {

    private static final ModuleIdentifier KEYCLOAK_WILDFLY_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-wildfly-adapter");
    private static final ModuleIdentifier KEYCLOAK_UNDERTOW_ADAPTER = ModuleIdentifier.create("org.keycloak.keycloak-undertow-adapter");

    @Override
    protected void addPlatformSpecificModules(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        // ModuleDependency(ModuleLoader moduleLoader, ModuleIdentifier identifier, boolean optional, boolean export, boolean importServices, boolean userSpecified)
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_WILDFLY_ADAPTER, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, KEYCLOAK_UNDERTOW_ADAPTER, false, false, false, false));
    }
}
