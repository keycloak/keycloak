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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.keycloak.models.file.adapter.RealmAdapter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * This class provides an in-memory copy of the entire model for each
 * Keycloak session.  At the start of the session, the model is read
 * from JSON.  When the session's transaction ends, the model is written.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class InMemoryModel implements KeycloakTransaction {
    private static final Logger logger = Logger.getLogger(InMemoryModel.class);

    private static String directory;
    private static String fileName;
    private final static Map<KeycloakSession, InMemoryModel> allModels = new HashMap<KeycloakSession, InMemoryModel>();

    private final KeycloakSession session;
    private final Map<String, RealmModel> allRealms = new HashMap<String, RealmModel>();

    //                realmId,    userId, userModel
    private final Map<String, Map<String,UserModel>> allUsers = new HashMap<String, Map<String,UserModel>>();

    private boolean isRollbackOnly = false;

    static void setFileName(String dataFileName) {
        fileName = dataFileName;
    }

    static void setDirectory(String dataDirectory) {
        directory = dataDirectory;
    }

    /**
     * Static factory to retrieve the model assigned to the session.
     *
     * @param session The Keycloak session.
     * @return The in-memory model that will be flushed when the session is over.
     */
    static InMemoryModel getModelForSession(KeycloakSession session) {

        synchronized (allModels) {
            InMemoryModel model = allModels.get(session);
            if (model == null) {
                model = new InMemoryModel(session);
                allModels.put(session, model);
                session.getTransaction().enlist(model);
                model.readModelFile();
            }

            return model;
        }
    }

    private InMemoryModel(KeycloakSession session) {
        this.session = session;
    }

    private void readModelFile() {
        File kcdata = new File(directory, fileName);
        if (!kcdata.exists()) return;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(kcdata);
            ImportUtils.importFromStream(session, JsonSerialization.mapper, fis, Strategy.IGNORE_EXISTING);
        } catch (IOException ioe) {
            logger.error("Unable to read model file " + kcdata.getAbsolutePath(), ioe);
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {
                logger.error("Failed to close output stream.", e);
            }
        }
    }

    void writeModelFile() {
        FileOutputStream outStream = null;
        File keycloakModelFile = new File(directory, fileName);
        try {
            outStream = new FileOutputStream(keycloakModelFile);
            exportModel(outStream);
        } catch (IOException e) {
            logger.error("Unable to write model file " + keycloakModelFile.getAbsolutePath(), e);
        } finally {
            try {
                if (outStream != null) outStream.close();
            } catch (IOException e) {
                logger.error("Failed to close output stream.", e);
            }
        }
    }

    protected void exportModel(FileOutputStream outStream) throws IOException {
        List<RealmModel> realms = session.realms().getRealms();
        List<RealmRepresentation> reps = new ArrayList<RealmRepresentation>();
        for (RealmModel realm : realms) {
            reps.add(ExportUtils.exportRealm(session, realm, true));
        }

        JsonSerialization.prettyMapper.writeValue(outStream, reps);
    }

    public void putRealm(String id, RealmAdapter realm) {
        allRealms.put(id, realm);
        allUsers.put(id, new HashMap<String, UserModel>());
    }

    public RealmModel getRealm(String id) {
        return allRealms.get(id);
    }

    public Collection<RealmModel> getRealms() {
       return allRealms.values();
    }

    public RealmModel getRealmByName(String name) {
        for (RealmModel realm : getRealms()) {
            if (realm.getName().equals(name)) return realm;
        }

        return null;
    }

    public boolean removeRealm(String id) {
        allUsers.remove(id);
        return (allRealms.remove(id) != null);
    }

    protected Map<String, UserModel> realmUsers(String realmId) {
        Map<String, UserModel> realmUsers = allUsers.get(realmId);
        if (realmUsers == null) throw new NullPointerException("Realm users not found for id=" + realmId);
        return realmUsers;
    }

    public void putUser(String realmId, String userId, UserModel user) {
        realmUsers(realmId).put(userId, user);
    }

    public UserModel getUser(String realmId, String userId) {
        return realmUsers(realmId).get(userId);
    }

    public boolean hasUserWithUsername(String realmId, String username) {
        for (UserModel user : getUsers(realmId)) {
            if (user.getUsername().equals(username)) return true;
        }

        return false;
    }

    public Collection<UserModel> getUsers(String realmId) {
       return realmUsers(realmId).values();
    }

    public boolean removeUser(String realmId, String userId) {
        return (realmUsers(realmId).remove(userId) != null);
    }

    @Override
    public void begin() {
    }

    // commitCount is used for debugging.  This allows you to easily run a test
    // to a particular point and then examine the JSON file.
    // private static int commitCount = 0;

    @Override
    public void commit() {
//        commitCount++;
        synchronized (allModels) {
            // in case commit was somehow called twice on the same session
            if (!allModels.containsKey(session)) return;

            try {
                writeModelFile();
            } finally {
                allModels.remove(session);
             //   System.out.println("*** commitCount=" + commitCount);
            }

        // if (commitCount == 61) System.exit(0);
        }
    }

    @Override
    public void rollback() {
        synchronized (allModels) {
            allModels.remove(session);
        }
    }

    @Override
    public void setRollbackOnly() {
        isRollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return isRollbackOnly;
    }

    @Override
    public boolean isActive() {
        synchronized (allModels) {
            return allModels.containsKey(session);
        }
    }

}
