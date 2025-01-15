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

import java.util.Optional;

/**
 * A generic {@link CompatibilityResult} implementation to signal an incompatible configuration.
 * <p>
 * It returns an error message logging the metadata entry that is no compatible and its values.
 *
 * @param type     The group where the metadata entry belongs (versions, cli options, features or other).
 * @param key      The metadata entry's key.
 * @param oldValue The metadata entry's old value.
 * @param newValue The metadata entry's new value.
 */
public record IncompatibleResult(String type, String key, String oldValue, String newValue) implements CompatibilityResult {
    @Override
    public int exitCode() {
        return CompatibilityResult.INCOMPATIBLE_EXIT_CODE;
    }

    @Override
    public Optional<String> errorMessage() {
        return Optional.of("[%s] %s is incompatible: Old=%s, New=%s".formatted(type, key, oldValue, newValue));
    }
}
