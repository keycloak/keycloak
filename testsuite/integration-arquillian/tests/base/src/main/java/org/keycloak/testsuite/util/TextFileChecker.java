/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 *
 * @author hmlnarik
 */
public class TextFileChecker {

    private static final Logger log = Logger.getLogger(TextFileChecker.class);

    private final Map<Path, Long> lastCheckedPositions = new HashMap<>();

    private final Path[] paths;

    public TextFileChecker(Path... paths) {
        this.paths = paths;
    }

    private void updateLastCheckedPositionsOfAllFilesToEndOfFile(Path path) throws IOException {
        if (Files.exists(path)) {
            lastCheckedPositions.put(path, Files.size(path));
        } else {
            lastCheckedPositions.remove(path);
        }
    }

    public void checkFiles(boolean verbose, Consumer<Stream<String>> lineChecker) throws IOException {
        for (Path path : paths) {
            log.logf(verbose ? Level.INFO : Level.DEBUG, "Checking server log: '%s'", path.toAbsolutePath());

            if (! Files.exists(path)) {
                continue;
            }

            try (InputStream in = Files.newInputStream(path)) {
                Long lastCheckedPosition = lastCheckedPositions.computeIfAbsent(path, p -> 0L);
                in.skip(lastCheckedPosition);
                try (BufferedReader b = new BufferedReader(new InputStreamReader(in))) {
                    lineChecker.accept(b.lines());
                }
            }
        }
    }

    public void updateLastCheckedPositionsOfAllFilesToEndOfFile() throws IOException {
        for (Path path : paths) {
            updateLastCheckedPositionsOfAllFilesToEndOfFile(path);
        }
    }
}
