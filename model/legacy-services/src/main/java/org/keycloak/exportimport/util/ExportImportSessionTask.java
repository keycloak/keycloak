/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.exportimport.util;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;

import java.io.IOException;

/**
 * Just to wrap {@link IOException}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class ExportImportSessionTask implements KeycloakSessionTask {

    @Override
    public void run(KeycloakSession session) {
        try {
            runExportImportTask(session);
        } catch (IOException ioe) {
            throw new RuntimeException("Error during export/import: " + ioe.getMessage(), ioe);
        }
    }

    protected abstract void runExportImportTask(KeycloakSession session) throws IOException;
}
