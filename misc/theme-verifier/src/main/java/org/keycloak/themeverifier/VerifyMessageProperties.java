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
package org.keycloak.themeverifier;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VerifyMessageProperties {

    private final File file;
    private List<String> messages;

    public VerifyMessageProperties(File file) {
        this.file = file;
    }

    public List<String> verify() throws MojoExecutionException {
        messages = new ArrayList<>();
        try {
            String contents = Files.readString(file.toPath());
            verifyNoDuplicateKeys(contents);
        } catch (IOException e) {
            throw new MojoExecutionException("Can not read file " + file, e);
        }
        return messages;
    }

    private void verifyNoDuplicateKeys(String contents) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(contents));
        String line;
        HashSet<String> seenKeys = new HashSet<>();
        HashSet<String> duplicateKeys = new HashSet<>();
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            int split = line.indexOf("=");
            if (split != -1) {
                String key = line.substring(0, split).trim();
                if (seenKeys.contains(key)) {
                    duplicateKeys.add(key);
                } else {
                    seenKeys.add(key);
                }
            }
        }
        if (!duplicateKeys.isEmpty()) {
            messages.add("Duplicate keys in file '" + file.getAbsolutePath() + "': " + duplicateKeys);
        }
    }
}
