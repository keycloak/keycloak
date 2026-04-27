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

package org.keycloak.quarkus.runtime.cli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// Builds a pre-computed Bloom filter (.bloom) alongside a plaintext password denylist.
// The server loads the .bloom file instead of rebuilding from plaintext, reducing reload latency.
@Command(name = BuildPasswordDenylist.NAME,
        header = BuildPasswordDenylist.HEADER,
        description = "%n" + BuildPasswordDenylist.HEADER
                + "%n%nKeycloak's password-blacklist policy rejects passwords found in a plaintext denylist file."
                + " For large lists, loading from plaintext on every startup or reload can take seconds."
                + " Run this command once after creating or updating DENYLIST_FILE to generate a pre-computed"
                + " .bloom file alongside it. The server will pick it up automatically, reducing load time"
                + " to milliseconds. The plaintext file must remain present; the .bloom file is supplementary only.",
        footerHeading = "%nExamples:%n",
        footer = {
                "  ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} /path/to/denylist.txt%n",
                "  ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} /path/to/denylist.txt --fpp 0.00001%n"
        })
public class BuildPasswordDenylist extends AbstractCommand {

    public static final String NAME = "build-password-denylist";
    public static final String HEADER = "Pre-compute a password denylist to speed up server startup and reload.";

    @Parameters(index = "0",
            paramLabel = "DENYLIST_FILE",
            description = "Path to the plaintext password denylist file (one password per line, UTF-8).")
    private Path inputFile;

    @Option(names = "--fpp",
            paramLabel = "PROBABILITY",
            description = "Desired false-positive probability for the Bloom filter, defaults to 0.0001.",
            defaultValue = "0.0001")
    private double fpp;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isHelpAll() {
        return false;
    }

    @Override
    protected void runCommand() {
        if (!Files.isRegularFile(inputFile)) {
            executionError(spec.commandLine(), "File not found or not a regular file: " + inputFile);
        }
        if (fpp <= 0.0 || fpp >= 1.0) {
            executionError(spec.commandLine(), "--fpp must be between 0 and 1 (exclusive), got: " + fpp);
        }

        Path outputFile = inputFile.resolveSibling(inputFile.getFileName() + ".bloom");
        picocli.println("Building Bloom filter from: " + inputFile);
        picocli.println("  False-positive probability: " + fpp);

        try {
            long startMs = System.currentTimeMillis();
            BlacklistPasswordPolicyProviderFactory.buildBloomFile(inputFile, fpp);
            long elapsedMs = System.currentTimeMillis() - startMs;
            long outputSizeKb = Files.size(outputFile) / 1024;
            picocli.println("Done in " + elapsedMs + " ms. Output: " + outputFile + " (" + outputSizeKb + " KB)");
        } catch (IOException e) {
            executionError(spec.commandLine(), "Failed to build Bloom filter: " + e.getMessage(), e);
        }
    }
}
