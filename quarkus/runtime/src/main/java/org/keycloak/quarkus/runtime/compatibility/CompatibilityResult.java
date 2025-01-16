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
 * The result of {@link CompatibilityManager#isCompatible(ServerInfo)}.
 * <p>
 * It is composed by the exit code (to help building scripts around this tool as is is easier than parsing logs), and an
 * optional error message.
 */
public interface CompatibilityResult {

    int ROLLING_UPGRADE_EXIT_CODE = 0;
    int RECREATE_UPGRADE_EXIT_CODE = 4;

    /**
     * The compatible {@link CompatibilityResult} implementation
     */
    CompatibilityResult OK = new CompatibilityResult() {
        @Override
        public int exitCode() {
            return ROLLING_UPGRADE_EXIT_CODE;
        }

        @Override
        public Optional<String> endMessage() {
            return Optional.of("[OK] Rolling Upgrade is available.");
        }
    };

    /**
     * @return The exit code to use to signal the compatibility result.
     */
    int exitCode();

    /**
     * @return An optional error message explaining what caused the incompatibility.
     */
    default Optional<String> errorMessage() {
        return Optional.empty();
    }

    /**
     * @return An optional message after the check is finished.
     */
    default Optional<String> endMessage() {
        return Optional.empty();
    }

}
