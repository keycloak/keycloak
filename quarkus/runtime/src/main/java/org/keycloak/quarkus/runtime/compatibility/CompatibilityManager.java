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

/**
 * This interface manages the metadata for backwards compatibility checks.
 */
public interface CompatibilityManager {

    /**
     * @return The current metadata.
     */
    ServerInfo current();

    /**
     * Adds a custom {@link CompatibilityComparator} to the version identified by {@code versionKey}.
     *
     * @param versionKey The version identification.
     * @param comparator The new {@link CompatibilityComparator}. If {@code null}, this method will do nothing.
     * @return This instance.
     */
    CompatibilityManager addVersionComparator(String versionKey, CompatibilityComparator comparator);

    /**
     * Checks if the metadata is backwards compatible with the current metadata.
     * <p>
     * If backwards compatible, it means Keycloak can have instance with the old and the current metadata running on the
     * same cluster.
     *
     * @param other The other metadata.
     * @return The {@link CompatibilityResult} with the result of comparing the current with the {@code other} metadata.
     */
    CompatibilityResult isCompatible(ServerInfo other);

}
