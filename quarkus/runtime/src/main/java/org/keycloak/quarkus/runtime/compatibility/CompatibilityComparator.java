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

package org.keycloak.quarkus.runtime.compatibility;

import java.util.Objects;

/**
 * A comparator interface for configuration metadata entries.
 * <p>
 * An entry's old and new values are compatible if a Keycloak instance using the old value can make progress (and be
 * correct) with a Keycloak instance using the new value.
 */
public interface CompatibilityComparator {

    CompatibilityComparator EQUALS = (ignored, oldValue, newValue) -> Objects.equals(oldValue, newValue);

    /**
     * Checks if the metadata entry identified by the {@code key} parameter is compatible.
     *
     * @param key      The metadata entry key.
     * @param oldValue The metadata entry's old value.
     * @param newValue The metadata entry's new value.
     * @return {@code true} if the {@code oldValue} is backwards compatible with the {@code newValue} and {@code false}
     * otherwise.
     */
    boolean isCompatible(String key, String oldValue, String newValue);

}
