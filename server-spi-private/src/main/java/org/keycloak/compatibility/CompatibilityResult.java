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

package org.keycloak.compatibility;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The result of {@link CompatibilityMetadataProvider#isCompatible(Map)}.
 * <p>
 * It is composed by the exit code (to help building scripts around this tool as it is easier than parsing logs), and an
 * optional error message.
 */
public interface CompatibilityResult {

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

    default Optional<Set<String>> incompatibleAttributes() {return Optional.empty();}

    static CompatibilityResult providerCompatible(String providerId) {
        return new ProviderCompatibleResult(Objects.requireNonNull(providerId));
    }

    static CompatibilityResult incompatibleAttribute(String providerId, String attribute, String previousValue, String currentValue) {
        return new ProviderIncompatibleResult(Objects.requireNonNull(providerId), Objects.requireNonNull(attribute),
                previousValue, currentValue);
    }

    enum ExitCode {
        ROLLING(0),
        // see picocli.CommandLine.ExitCode
        // 1 -> software error
        // 2 -> usage error
        RECREATE(3);
        // 4 -> feature 'rolling-updates' disabled

        final int exitCode;

        ExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        public int value() {
            return exitCode;
        }
    }
}
