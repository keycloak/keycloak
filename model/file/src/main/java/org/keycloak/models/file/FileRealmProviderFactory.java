/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.models.file;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RealmProviderFactory;


/**
 * RealmProviderFactory for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class FileRealmProviderFactory implements RealmProviderFactory {

    private String directory;
    private String fileName;

    @Override
    public void init(Config.Scope config) {
        this.fileName = config.get("fileName");
        if (fileName == null) fileName = "keycloak-model.json";
        InMemoryModel.setFileName(fileName);

        this.directory = config.get("directory");
        if (directory == null) directory = System.getProperty("jboss.server.data.dir");
        if (directory == null) directory = ".";
        InMemoryModel.setDirectory(directory);
    }

    @Override
    public String getId() {
        return "file";
    }

    @Override
    public RealmProvider create(KeycloakSession session) {
        return new FileRealmProvider(session, InMemoryModel.getModelForSession(session));
    }

    @Override
    public void close() {
    }

}
