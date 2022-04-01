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

import java.io.IOException;
import java.net.URL;
import java.sql.Driver;
import java.util.Collections;
import java.util.Enumeration;

public class KeycloakClassLoader extends ClassLoader {

    KeycloakClassLoader() {
        super(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // drivers are going to be loaded lazily, and we avoid loading all available drivers
        // see https://github.com/quarkusio/quarkus/pull/7089
        if (name.contains(Driver.class.getName())) {
            return Collections.emptyEnumeration();
        }

        return super.getResources(name);
    }
}
