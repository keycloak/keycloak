/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.jpa.util;

import static java.util.Optional.*;
import static org.keycloak.common.util.CollectionUtil.*;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Static stateless methods for reuse in model adapter classes.
 */
public class ModelAdapterUtil {

    /**
     * Common logic for updating an attribute with potentially multiple values.
     * Actually, the same value might occur multiple times.
     * 
     * @param attrName The case-sensitive name of the attribute (must not be null).
     * @param newValues The new list of values to be set. If this is null or empty, the attribute will be removed.
     * @param currentAttributesProvider The provider of the current (i.e. persistent) attribute values (must not be null).
     * @param attrRemover The Supplier that is used to remove the attribute with all its values from persistence (must not be null).
     * @param attrSingleValuePersister A function that persists a single pair of attribute name and value (must not be null).
     */
    public static void setMultiValueAttribute(String attrName, List<String> newValues,
            Supplier<Map<String, List<String>>> currentAttributesProvider, Consumer<String> attrRemover,
            BiConsumer<String, String> attrSingleValuePersister) {

        List<String> currentValues = currentAttributesProvider.get().getOrDefault(attrName, List.of());

        if (collectionEquals(currentValues, ofNullable(newValues).orElse(List.of()))) {
            return;
        }

        // Remove all existing
        attrRemover.accept(attrName);

        if (newValues != null) {
            // Put all new
            for (String value : newValues) {
                attrSingleValuePersister.accept(attrName, value);
            }
        }
    }
}
