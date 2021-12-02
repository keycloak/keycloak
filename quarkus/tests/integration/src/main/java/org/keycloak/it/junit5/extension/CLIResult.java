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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.keycloak.quarkus.runtime.cli.Picocli;

import io.quarkus.test.junit.main.LaunchResult;
import picocli.CommandLine;

public interface CLIResult extends LaunchResult {

    static Object create(List<String> outputStream, List<String> errStream, int exitCode, boolean distribution) {
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

            @Override
            public boolean isDistribution() {
                return distribution;
            }
        };
    }

    boolean isDistribution();

    default void assertStarted() {
        assertFalse(getOutput().contains("The delayed handler's queue was overrun and log record(s) were lost (Did you forget to configure logging?)"), () -> "The standard Output:\n" + getOutput() + "should not contain a warning about log queue overrun.");
        assertTrue(getOutput().contains("Listening on:"), () -> "The standard output:\n" + getOutput() + "does include \"Listening on:\"");
        assertNotDevMode();
    }

    default void assertNotDevMode() {
        assertFalse(getOutput().contains("Running the server in dev mode."),
                () -> "The standard output:\n" + getOutput() + "does include the Start Dev output");
    }

    default void assertStartedDevMode() {
        assertTrue(getOutput().contains("Running the server in dev mode."),
                () -> "The standard output:\n" + getOutput() + "doesn't include the Start Dev output");
    }

    default void assertError(String msg) {
        assertTrue(getErrorOutput().contains(msg),
                () -> "The Error Output:\n " + getErrorOutput() + "\ndoesn't contains " + msg);
    }

    default void assertHelp(String command) {
        if (command == null) {
            fail("No command provided");
        }

        CommandLine cmd = Picocli.createCommandLine(Arrays.asList(command, "--help"));

        if (isDistribution()) {
            cmd.setCommandName("kc.sh");
        }

        try (
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                PrintStream printStream = new PrintStream(outStream, true)
        ) {
            if ("kc.sh".equals(command)) {
                cmd.usage(printStream);
            } else {
                cmd.getSubcommands().get(command).usage(printStream);
            }

            // not very reliable, we should be comparing the output with some static reference to the help message.
            assertTrue(getOutput().trim().equals(outStream.toString().trim()),
                    () -> "The Output:\n " + getOutput() + "\ndoesnt't contains " + outStream.toString().trim());
        } catch (IOException cause) {
            throw new RuntimeException("Failed to assert help", cause);
        }
    }
}
