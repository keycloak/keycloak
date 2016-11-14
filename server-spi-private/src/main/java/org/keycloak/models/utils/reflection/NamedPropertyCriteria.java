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

package org.keycloak.models.utils.reflection;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A criteria that matches a property based on name
 *
 * @see PropertyCriteria
 */
public class NamedPropertyCriteria implements PropertyCriteria {
    private final String[] propertyNames;

    public NamedPropertyCriteria(String... propertyNames) {
        this.propertyNames = propertyNames;
    }

    public boolean fieldMatches(Field f) {
        for (String propertyName : propertyNames) {
            if (propertyName.equals(f.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean methodMatches(Method m) {
        String[] validPrefix = {"get", "is"};
        for (String propertyName : propertyNames) {
            for (String prefix : validPrefix) {
                if (m.getName().startsWith(prefix) &&
                        Introspector.decapitalize(m.getName().substring(prefix.length())).equals(propertyName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
