/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.exportimport;

import java.io.IOException;

/**
 * This adapter allows the exporter to act independent of APIs used to serve the exported data to the caller.
 *
 * @author Alexander Schwartz
 */
public interface ExportAdapter {
    /**
     * Set the mime type of the output.
     *
     * @param mediaType Mime Type
     */
    void setType(String mediaType);

    /**
     * Write to the output stream. Once writing is complete, close it.
     *
     * @param consumer A consumer to that accepts the output stream.
     */
    void writeToOutputStream(ConsumerOfOutputStream consumer);

    /**
     * Custom consumer that is allowed to throw an {@link IOException} as writing to an output stream might do this.
     */
    @FunctionalInterface
    interface ConsumerOfOutputStream {
        void accept(java.io.OutputStream t) throws IOException;
    }
}
