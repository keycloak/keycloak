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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.keycloak.common.util.StringPropertyReplacer;

import static org.keycloak.common.util.StringPropertyReplacer.replaceProperties;

public abstract class AbstractFileBasedImportProvider implements ImportProvider {

    private static final StringPropertyReplacer.PropertyResolver ENV_VAR_PROPERTY_RESOLVER = new StringPropertyReplacer.PropertyResolver() {
        @Override
        public String resolve(String property) {
            return Optional.ofNullable(property).map(System::getenv).orElse(null);
        }
    };

    protected InputStream parseFile(File importFile) throws IOException {
        if (ExportImportConfig.isReplacePlaceholders()) {
            return replaceProperties(new BufferedInputStream(new FileInputStream(importFile)), ENV_VAR_PROPERTY_RESOLVER);
        }

        return new FileInputStream(importFile);
    }

}
