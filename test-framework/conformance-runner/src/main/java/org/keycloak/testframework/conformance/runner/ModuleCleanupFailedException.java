/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testframework.conformance.runner;

/**
 * A module run failed, and stopping the module afterwards failed as well, so the module and its browser may still be
 * running. Running the module again would let the two interfere, so this must not be retried. The cause is the reason
 * the run failed, the suppressed exception the reason stopping the module failed.
 */
public class ModuleCleanupFailedException extends RuntimeException {

    public ModuleCleanupFailedException(String message, RuntimeException cause, RuntimeException cleanupFailure) {
        super(message, cause);
        addSuppressed(cleanupFailure);
    }
}
