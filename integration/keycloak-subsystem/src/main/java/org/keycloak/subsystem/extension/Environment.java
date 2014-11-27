/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

import org.jboss.as.version.Version;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Allows the Keycloak subsystem to learn about its environment.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class Environment {
    private static final ModuleIdentifier KEYCLOAK_SUBSYSTEM = ModuleIdentifier.create("org.keycloak.keycloak-subsystem");

    private static final boolean isWildFly = findIsWildFly();

    public Environment() {
    }

    private static boolean findIsWildFly() {
        try {
            return !Version.AS_VERSION.startsWith("7");
        } catch (Exception e) {
            return false;
        }
    }

    public Module getSubsysModule() {
        // Unfortunately, we can't cache this because unit tests will fail
        try {
            return Module.getModuleFromCallerModuleLoader(KEYCLOAK_SUBSYSTEM);
        } catch (ModuleLoadException e) {
            throw new IllegalStateException("Can't find Keycloak subsystem.", e);
        }
    }

    public static boolean isWildFly() {
        return isWildFly;
    }
}
