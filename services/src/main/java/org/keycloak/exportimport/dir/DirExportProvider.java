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

import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.exportimport.util.MultipleStepsExportProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirExportProvider extends MultipleStepsExportProvider {

    private final File rootDirectory;

    public DirExportProvider() {
        // Determine system tmp directory
        String tempDir = System.getProperty("java.io.tmpdir");

        this.rootDirectory = new File(tempDir + "/keycloak-export");
        this.rootDirectory.mkdirs();

        logger.infof("Exporting into directory %s", this.rootDirectory.getAbsolutePath());
    }

    public DirExportProvider(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.rootDirectory.mkdirs();

        logger.infof("Exporting into directory %s", this.rootDirectory.getAbsolutePath());
    }

    public static boolean recursiveDeleteDir(File dirPath) {
        if (dirPath.exists()) {
            File[] files = dirPath.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    recursiveDeleteDir(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        if (dirPath.exists())
            return dirPath.delete();
        else
            return true;
    }

    @Override
    public void writeRealm(String fileName, RealmRepresentation rep) throws IOException {
        File file = new File(this.rootDirectory, fileName);
        FileOutputStream stream = new FileOutputStream(file);
        JsonSerialization.prettyMapper.writeValue(stream, rep);
    }

    @Override
    protected void writeUsers(String fileName, KeycloakSession session, RealmModel realm, List<UserModel> users) throws IOException {
        File file = new File(this.rootDirectory, fileName);
        FileOutputStream os = new FileOutputStream(file);
        ExportUtils.exportUsersToStream(session, realm, users, JsonSerialization.prettyMapper, os);
    }

    @Override
    protected void writeFederatedUsers(String fileName, KeycloakSession session, RealmModel realm, List<String> users) throws IOException {
        File file = new File(this.rootDirectory, fileName);
        FileOutputStream os = new FileOutputStream(file);
        ExportUtils.exportFederatedUsersToStream(session, realm, users, JsonSerialization.prettyMapper, os);
    }

    @Override
    public void close() {
    }
}
