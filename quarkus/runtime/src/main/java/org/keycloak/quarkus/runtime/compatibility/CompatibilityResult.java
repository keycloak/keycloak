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
 * It is composed by the exit code, to help building scripts around this tool and it makes easy to check the exit code
 * than parsing logs, and an optional error message.
 */
public interface CompatibilityResult {

    int COMPATIBLE_EXIT_CODE = 0;
    int INCOMPATIBLE_EXIT_CODE = 3;

    /**
     * The compatible {@link CompatibilityResult} implementation
     */
    CompatibilityResult OK = () -> COMPATIBLE_EXIT_CODE;

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

}
