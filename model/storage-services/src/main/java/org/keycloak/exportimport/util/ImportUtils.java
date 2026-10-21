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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.Strategy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.datastore.DefaultExportImportManager;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ImportUtils {

    private static final Logger logger = Logger.getLogger(ImportUtils.class);

    public static void importRealms(KeycloakSession session, Collection<RealmRepresentation> realms, Strategy strategy) {
        boolean masterImported = false;

        // Import admin realm first
        for (RealmRepresentation realm : realms) {
            if (Config.getAdminRealm().equals(realm.getRealm())) {
                if (importRealm(session, realm, strategy, () -> {})) {
                    masterImported = true;
                }
            }
        }

        for (RealmRepresentation realm : realms) {
            if (!Config.getAdminRealm().equals(realm.getRealm())) {
                importRealm(session, realm, strategy, () -> {});
            }
        }

        // If master was imported, we may need to re-create realm management clients
        if (masterImported) {
            session.realms().getRealmsStream()
                    .filter(realm -> realm.getMasterAdminClient() == null)
                    .forEach(realm -> {
                        logger.infof("Re-created management client in master realm for realm '%s'", realm.getName());
                        new RealmManager(session).setupMasterAdminManagement(realm);
            });
        }
    }

    /**
     * Fully import realm from representation, save it to model and return model of newly created realm
     *
     * @param session
     * @param rep
     * @param strategy specifies whether to overwrite or ignore existing realm or user entries
     * @param skipUserDependent If true, then import of any models, which needs users already imported in DB, will be skipped. For example authorization
     * @return newly imported realm (or existing realm if ignoreExisting is true and realm of this name already exists)
     * @deprecated
     */
    @Deprecated
    public static boolean importRealm(KeycloakSession session, RealmRepresentation rep, Strategy strategy, boolean skipUserDependent) {
        return importRealm(session, rep, strategy, skipUserDependent ? null : () -> {});
    }

    /**
     * Fully import realm from representation, save it to model and return model of newly created realm
     *
     * @param session
     * @param rep
     * @param strategy specifies whether to overwrite or ignore existing realm or user entries
     * @param userImport use null to indicate additional users will not be imported
     * @return newly imported realm (or existing realm if ignoreExisting is true and realm of this name already exists)
     */
    public static boolean importRealm(KeycloakSession session, RealmRepresentation rep, Strategy strategy, Runnable userImport) {
        String realmName = rep.getRealm();
        RealmProvider model = session.realms();
        RealmModel realm = model.getRealmByName(realmName);

        if (realm != null) {
            session.getContext().setRealm(realm);
            if (strategy == Strategy.IGNORE_EXISTING) {
                logger.infof("Realm '%s' already exists. Import skipped", realmName);
                return false;
            } else {
                logger.infof("Realm '%s' already exists. Removing it before import", realmName);
                if (Config.getAdminRealm().equals(realm.getId())) {
                    // Delete all masterAdmin apps due to foreign key constraints
                   model.getRealmsStream().forEach(r -> r.setMasterAdminClient(null));
                }
                // TODO: For migration between versions, it should be possible to delete just realm but keep it's users
                model.removeRealm(realm.getId());
            }
            session.getContext().setRealm(null);
        }

        RealmManager realmManager = new RealmManager(session);
        realmManager.importRealm(rep, userImport);

        if (System.getProperty(ExportImportConfig.ACTION) != null) {
            logger.infof("Realm '%s' imported", realmName);
        }

        return true;
    }

    public static Map<String, RealmRepresentation> getRealmsFromStream(ObjectMapper mapper, InputStream is) throws IOException {
        Map<String, RealmRepresentation> result = new HashMap<String, RealmRepresentation>();

        JsonFactory factory = mapper.getFactory();
        JsonParser parser = factory.createParser(is);
        try {
            parser.nextToken();

            if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                // Case with more realms in stream
                parser.nextToken();

                while (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    RealmRepresentation realmRep = parser.readValueAs(RealmRepresentation.class);
                    parser.nextToken();
                    result.put(realmRep.getRealm(), realmRep);
                }
            } else if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                // Case with single realm in stream
                RealmRepresentation realmRep = parser.readValueAs(RealmRepresentation.class);
                result.put(realmRep.getRealm(), realmRep);
            }
        } finally {
            parser.close();
        }

        return result;
    }

    // Assuming that it's invoked inside transaction
    public static void importUsersFromStream(KeycloakSession session, String realmName, ObjectMapper mapper, InputStream is, boolean federated, Consumer<KeycloakSession> onUserCreated) throws IOException {
        RealmProvider model = session.realms();
        JsonFactory factory = mapper.getJsonFactory();
        RealmModel realm = model.getRealmByName(realmName);
        try (JsonParser parser = factory.createJsonParser(is)) {
            parser.nextToken();

            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                if ("realm".equals(parser.getText())) {
                    parser.nextToken();
                    String currRealmName = parser.getText();
                    if (!currRealmName.equals(realmName)) {
                        throw new IllegalStateException("Trying to import users into invalid realm. Realm name: " + realmName + ", Expected realm name: " + currRealmName);
                    }
                } else if ((federated?"federatedUsers":"users").equals(parser.getText())) {
                    parser.nextToken();

                    if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                        parser.nextToken();
                    }

                    while (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                        UserRepresentation user = parser.readValueAs(UserRepresentation.class);
                        if (federated) {
                            DefaultExportImportManager.importFederatedUser(session, realm, user);
                        } else {
                            RepresentationToModel.createUser(session, realm, user);
                        }
                        onUserCreated.accept(session);
                        parser.nextToken();
                    }

                    if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                        parser.nextToken();
                    }
                }
            }
        }
    }

}
