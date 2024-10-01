/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A base interface for defining a provider so that its corresponding JAR file can be installed before executing tests.
 */
public interface TestProvider {

    /**
     * The provider name.
     *
     * @return the name
     */
    default String getName() {
        return "provider";
    }

    /**
     * The classes that should be added to the provider JAR file.
     *
     * @return the classes
     */
    Class[] getClasses();

    /**
     * A {@link Map} where the key is the path of a file relative to the META-INF directory at the package where this provider
     * is located and the value is the name of the manifest resource that should be created in the provider JAR file.
     * @return
     */
    default Map<String, String> getManifestResources() {
        return Map.of();
    }

    /**
     * A {@link List} of Strings each a representation of a path to a file relative to the META-INF directory. This is used
     * for configuration files you want to handle separately and do not wish to include in the resulting JAR.
     * @return
     */
    default List<String> getAdditionalConfig() {
        return Collections.emptyList();
    }
}
