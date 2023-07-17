/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file.yaml;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.snakeyaml.engine.v2.api.StreamDataWriter;

/**
 *
 * @author hmlnarik
 */
public class PathWriter implements StreamDataWriter, Closeable {

    private final BufferedWriter writer;

    public PathWriter(Path path) throws IOException {
        this.writer = Files.newBufferedWriter(path);
    }

    @Override
    public void write(String str) {
        try {
            this.writer.write(str);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void write(String str, int off, int len) {
        try {
            this.writer.write(str, off, len);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void flush() {
        try {
            this.writer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
