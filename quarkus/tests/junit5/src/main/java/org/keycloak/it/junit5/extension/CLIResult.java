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

import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.main.LaunchResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.containsString;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.is;
import static org.testcontainers.shaded.org.hamcrest.Matchers.not;

public interface CLIResult extends LaunchResult {

    static CLIResult create(List<String> outputStream, List<String> errStream, int exitCode) {
        return new CLIResult() {
            @Override
            public List<String> getOutputStream() {
                return outputStream;
            }

            @Override
            public String getErrorOutput() {
                return String.join("\n", errStream).replace("\r","");
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
        assertThat("The standard output should not contain a warning about log queue overrun.",
                getOutput(), not(containsString("The delayed handler's queue was overrun and log record(s) were lost (Did you forget to configure logging?)")));
        assertThat("The standard output does not include \"Listening on:\"",
                getOutput(), containsString("Listening on:"));
        assertNotDevMode();
    }

    default void assertNotDevMode() {
        assertThat("The standard output does include the Start Dev output",
                getOutput(), not(containsString("Running the server in development mode.")));
    }

    default void assertStartedDevMode() {
        assertThat("The standard output does not include the Start Dev output",
                getOutput(), containsString("Running the server in development mode."));
    }

    default void assertError(String msg) {
        assertThat("The error output does not contain: " + msg,
                getErrorOutput(), containsString(msg));
    }

    default void assertNoError(String msg) {
        assertThat("The error output contains: " + msg,
                getErrorOutput(), not(containsString(msg)));
    }

    default void assertWarning(String msg) {
        assertError(msg); //seems that warnings are printed on stderr
    }

    default void assertExitCode(int code) {
        assertThat("Exit codes do not match: ", exitCode(), is(code));
    }

    default void assertMessage(String message) {
        assertThat(getOutput(), containsString(message));
    }

    default void assertNoMessage(String message) {
        assertThat(getOutput(), not(containsString(message)));
    }

    default void assertMessageWasShownExactlyNumberOfTimes(String message, long numberOfShownTimes) {
        long msgCount = getOutput().lines().filter(oneMessage -> oneMessage.contains(message)).count();
        assertThat(msgCount, equalTo(numberOfShownTimes));
    }

    default void assertBuild() {
        assertMessage("Server configuration updated and persisted");
    }

    default void assertNoBuild() {
        assertNoMessage("Server configuration updated and persisted");
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

    default void assertJsonLogDefaultsApplied() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String[] splittedOutput = getOutput().split("\n");

        int counter = 0;

        for (String line: splittedOutput) {
            if (!line.trim().startsWith("{")) {
                counter++;
                //we ignore non-json output for now. Problem: the build done by start-dev does not know about the runtime configuration,
                // so when invoking start-dev and a build is done, the output is not json but unstructured console output
                continue;
            }
            JsonNode json = objectMapper.readTree(line);
            assertTrue(json.has("timestamp"));
            assertTrue(json.has("message"));
            assertTrue(json.has("level"));
        }

        if (counter == splittedOutput.length) {
            fail("No JSON found in output.");
        }
    }

    default void assertStringCount(String msg, int count) {
        Pattern pattern = Pattern.compile(msg);
        assertThat((int) pattern.matcher(getOutput()).results().count(), is(count));
    }
}
