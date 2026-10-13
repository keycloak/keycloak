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

package org.keycloak.exportimport.singlefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.util.ExportImportSessionTask;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.util.ObjectMapperResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SingleFileExportProvider implements ExportProvider {

    private static final Logger logger = Logger.getLogger(SingleFileExportProvider.class);

    private File file;

    private final KeycloakSessionFactory factory;
    private String realmName;

    public SingleFileExportProvider(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    public SingleFileExportProvider withFile(File file) {
        this.file = file;
        return this;
    }

    @Override
    public void exportModel() {

        new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                if (realmName != null) {
                    ServicesLogger.LOGGER.realmExportRequested(realmName);
                    exportRealm(session, realmName);
                } else {
                    ServicesLogger.LOGGER.fullModelExportRequested();
                    logger.infof("Exporting model into file %s", file.getAbsolutePath());
                    Stream<RealmRepresentation> realms = session.realms().getRealmsStream()
                            .peek(realm -> session.getContext().setRealm(realm))
                            .map(realm -> ExportUtils.exportRealm(session, realm, true, true));

                    writeToFile(realms);
                }
            }
        }.runTask(factory);
        ServicesLogger.LOGGER.exportSuccess();
    }

    private void exportRealm(KeycloakSession session, final String realmName) throws IOException {
        logger.infof("Exporting realm '%s' into file %s", realmName, this.file.getAbsolutePath());
        RealmModel realm = session.realms().getRealmByName(realmName);
        Objects.requireNonNull(realm, "realm not found by realm name '" + realmName + "'");
        session.getContext().setRealm(realm);
        RealmRepresentation realmRep = ExportUtils.exportRealm(session, realm, true, true);
        writeToFile(realmRep);
    }

    @Override
    public void close() {
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper streamSerializer = ObjectMapperResolver.createStreamSerializer();
        streamSerializer.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        streamSerializer.enable(SerializationFeature.INDENT_OUTPUT);
        return streamSerializer;
    }

    private void writeToFile(Object reps) throws IOException {
        FileOutputStream stream = new FileOutputStream(this.file);
        getObjectMapper().writeValue(stream, reps);
    }

    public ExportProvider withRealmName(String realmName) {
        this.realmName = realmName;
        return this;
    }
}
