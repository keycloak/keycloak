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

import org.keycloak.policy.DenylistPasswordPolicyProviderFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// Builds a pre-computed Bloom filter (.bloom) alongside a plaintext password denylist.
// The server loads the .bloom file instead of rebuilding from plaintext, reducing reload latency.
@Command(name = BuildPasswordDenylist.NAME,
        header = BuildPasswordDenylist.HEADER,
        sortOptions = false,
        description = "%n" + BuildPasswordDenylist.HEADER
                + "%n%nKeycloak's password-denylist policy rejects passwords found in a plaintext denylist file."
                + " For large lists, loading from plaintext on every startup or reload can take seconds."
                + " Run this command once after creating or updating DENYLIST_FILE to generate a pre-computed"
                + " .bloom file. To use it, configure the password policy with the .bloom filename instead of"
                + " the plaintext file. The server detects the file type by extension and loads it accordingly,"
                + " reducing load time to milliseconds.",
        footerHeading = "%nExamples:%n",
        footer = {
                "  ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} /path/to/denylist.txt%n",
                "  ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} /path/to/denylist.txt --fpp 0.00001 -o /path/to/out.bloom%n"
        })
public class BuildPasswordDenylist extends AbstractCommand {

    public static final String NAME = "build-password-denylist";
    public static final String HEADER = "Pre-compute a Bloom filter for a password denylist.";

    @Parameters(index = "0",
            paramLabel = "DENYLIST_FILE",
            description = "Path to the plaintext password denylist file (one password per line, UTF-8).")
    private Path inputFile;

    @Option(names = "--fpp",
            paramLabel = "PROBABILITY",
            description = "Desired false-positive probability for the Bloom filter, defaults to 0.0001.",
            defaultValue = "0.0001")
    private double fpp;

    @Option(names = {"-o", "--output"},
            paramLabel = "OUTPUT_FILE",
            description = "Path for the generated .bloom file. Must end with '.bloom'. Defaults to <DENYLIST_FILE>.bloom in the same directory.")
    private Path outputFile;

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

        if (outputFile == null) {
            outputFile = inputFile.resolveSibling(inputFile.getFileName() + ".bloom");
        } else if (!outputFile.getFileName().toString().endsWith(".bloom")) {
            executionError(spec.commandLine(), "--output must end with '.bloom', got: " + outputFile);
        } else {
            Path outputParent = outputFile.toAbsolutePath().getParent();
            if (outputParent != null && !Files.isDirectory(outputParent)) {
                executionError(spec.commandLine(), "Output directory does not exist: " + outputParent);
            }
        }
        picocli.println("Building Bloom filter from: " + inputFile);
        picocli.println("  False-positive probability: " + fpp);

        try {
            long startMs = System.currentTimeMillis();
            DenylistPasswordPolicyProviderFactory.buildBloomFile(inputFile, outputFile, fpp);
            long elapsedMs = System.currentTimeMillis() - startMs;
            long outputSizeBytes = Files.size(outputFile);
            String sizeStr;
            if (outputSizeBytes < 1024) {
                sizeStr = outputSizeBytes + " B";
            } else if (outputSizeBytes < 1024 * 1024) {
                sizeStr = (outputSizeBytes / 1024) + " KB";
            } else {
                sizeStr = (outputSizeBytes / (1024 * 1024)) + " MB";
            }
            picocli.println("Done in " + elapsedMs + " ms. Output: " + outputFile + " (" + sizeStr + ")");
            picocli.println("Next step: place " + outputFile.getFileName() + " in your password-blacklists folder and"
                    + " configure the password blacklist policy value to '" + outputFile.getFileName() + "'.");
        } catch (IOException e) {
            executionError(spec.commandLine(), "Failed to build Bloom filter: " + e.getMessage(), e);
        }
    }
}
