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

package org.keycloak.exportimport.dir;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.ExportProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.File;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirExportProviderFactory implements ExportProviderFactory {

    public static final String PROVIDER_ID = "dir";

    @Override
    public ExportProvider create(KeycloakSession session) {
        String dir = ExportImportConfig.getDir();
        return dir!=null ? new DirExportProvider(new File(dir)) : new DirExportProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
