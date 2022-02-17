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

package org.keycloak.it.junit5.extension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.approvaltests.Approvals;
import io.quarkus.test.junit.main.LaunchResult;

public interface CLIResult extends LaunchResult {

    static Object create(List<String> outputStream, List<String> errStream, int exitCode) {
        return new CLIResult() {
            @Override
            public List<String> getOutputStream() {
                return outputStream;
            }

            @Override
            public List<String> getErrorStream() {
                return errStream;
            }

            @Override
            public int exitCode() {
                return exitCode;
            }
        };
    }

    default void assertStarted() {
        assertFalse(getOutput().contains("The delayed handler's queue was overrun and log record(s) were lost (Did you forget to configure logging?)"), () -> "The standard Output:\n" + getOutput() + "should not contain a warning about log queue overrun.");
        assertTrue(getOutput().contains("Listening on:"), () -> "The standard output:\n" + getOutput() + "does include \"Listening on:\"");
        assertNotDevMode();
    }

    default void assertNotDevMode() {
        assertFalse(getOutput().contains("Running the server in development mode."),
                () -> "The standard output:\n" + getOutput() + "\ndoes include the Start Dev output");
    }

    default void assertStartedDevMode() {
        assertTrue(getOutput().contains("Running the server in development mode."),
                () -> "The standard output:\n" + getOutput() + "\ndoesn't include the Start Dev output");
    }

    default void assertError(String msg) {
        assertTrue(getErrorOutput().contains(msg),
                () -> "The Error Output:\n " + getErrorOutput() + "\ndoesn't contains " + msg);
    }

    default void assertHelp() {
        try {
            Approvals.verify(getOutput());
        } catch (Exception cause) {
            throw new RuntimeException("Failed to assert help", cause);
        }
    }

    default void assertMessage(String message) {
        assertTrue(getOutput().contains(message));
    }

    default void assertBuild() {
        assertMessage("Server configuration updated and persisted");
    }

    default void assertNoBuild() {
        assertFalse(getOutput().contains("Server configuration updated and persisted"));
    }

    default boolean isClustered() {
        return getOutput().contains("Starting JGroups channel `ISPN`");
    }

    default void assertLocalCache() {
        assertFalse(isClustered());
    }

    default void assertClusteredCache() {
        assertTrue(isClustered());
    }
}
