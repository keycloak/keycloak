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
package org.keycloak.connections.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * This class dispenses a FileConnectionProvider to Keycloak sessions.  It
 * makes sure that only one InMemoryModel is provided for each session and it
 * handles thread contention for the file where the model is read or saved.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class DefaultFileConnectionProviderFactory implements FileConnectionProviderFactory {

    protected static final Logger logger = Logger.getLogger(DefaultFileConnectionProviderFactory.class);

    private File kcdata;
    private final Map<KeycloakSession, FileConnectionProvider> allProviders = new HashMap<KeycloakSession, FileConnectionProvider>();

    @Override
    public void init(Config.Scope config) {
        String fileName = config.get("fileName");
        if (fileName == null) {
            fileName = "keycloak-model.json";
        }

        String directory = config.get("directory");
        if (directory == null) {
            directory = System.getProperty("jboss.server.data.dir");
        }
        if (directory == null) {
            directory = ".";
        }

        kcdata = new File(directory, fileName);
    }

    public void sessionClosed(KeycloakSession session) {
        synchronized(allProviders) {
            allProviders.remove(session);
            //logger.info("Removed session " + session.hashCode());
            //logger.info("sessionClosed: Session count=" + allModels.size());
        }
    }

    void readModelFile(KeycloakSession session) {
        synchronized(allProviders) {
            if (!kcdata.exists()) {
                return;
            }

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(kcdata);
                ImportUtils.importFromStream(session, JsonSerialization.mapper, fis, Strategy.IGNORE_EXISTING);
            } catch (IOException ioe) {
                logger.error("Unable to read model file " + kcdata.getAbsolutePath(), ioe);
            } finally {
                //logger.info("Read model file for session=" + session.hashCode());
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    logger.error("Failed to close output stream.", e);
                }
            }
        }
    }

    void writeModelFile(KeycloakSession session) {
        synchronized(allProviders) {
            FileOutputStream outStream = null;

            try {
                outStream = new FileOutputStream(kcdata);
                exportModel(session, outStream);
            } catch (IOException e) {
                logger.error("Unable to write model file " + kcdata.getAbsolutePath(), e);
            } finally {
                //logger.info("Wrote model file for session=" + session.hashCode());
                try {
                    if (outStream != null) {
                        outStream.close();
                    }
                } catch (IOException e) {
                    logger.error("Failed to close output stream.", e);
                }
            }
        }
    }

    private void exportModel(KeycloakSession session, FileOutputStream outStream) throws IOException {
        List<RealmModel> realms = session.realms().getRealms();
        List<RealmRepresentation> reps = new ArrayList<RealmRepresentation>();
        for (RealmModel realm : realms) {
            reps.add(ExportUtils.exportRealm(session, realm, true));
        }

        JsonSerialization.prettyMapper.writeValue(outStream, reps);
    }

    @Override
    public FileConnectionProvider create(KeycloakSession session) {
        synchronized (allProviders) {
            FileConnectionProvider fcProvider = allProviders.get(session);
            if (fcProvider == null) {
                InMemoryModel model = new InMemoryModel();
                fcProvider = new DefaultFileConnectionProvider(this, session, model);
                allProviders.put(session, fcProvider);
                session.getTransaction().enlist(fcProvider);
                readModelFile(session);
                //logger.info("Added session " + session.hashCode() + " total sessions=" + allModels.size());
            }

            return fcProvider;
        }
    }

    // commitCount is used for debugging.  This allows you to easily run a test
    // to a particular point and then examine the JSON file.
    //private static int commitCount = 0;
    void commit(KeycloakSession session) {
        //commitCount++;
        synchronized (allProviders) {
            // in case commit was somehow called twice on the same session
            if (!allProviders.containsKey(session)) return;

            try {
                writeModelFile(session);
            } finally {
                allProviders.remove(session);
                //logger.info("Removed session " + session.hashCode());
                //logger.info("*** commitCount=" + commitCount);
                //logger.info("commit(): Session count=" + allModels.size());
            }

    //     if (commitCount == 16) {Thread.dumpStack();System.exit(0);}
        }
    }

    void rollback(KeycloakSession session) {
        synchronized (allProviders) {
            allProviders.remove(session);
            //logger.info("rollback(): Session count=" + allModels.size());
        }
    }

    boolean isActive(KeycloakSession session) {
        synchronized (allProviders) {
            return allProviders.containsKey(session);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

}
