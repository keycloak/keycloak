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

import org.jboss.logging.Logger;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.UsersExportStrategy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class MultipleStepsExportProvider implements ExportProvider {

    protected final Logger logger = Logger.getLogger(getClass());

    @Override
    public void exportModel(KeycloakSessionFactory factory) throws IOException {
        final RealmsHolder holder = new RealmsHolder();

        KeycloakModelUtils.runJobInTransaction(factory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                List<RealmModel> realms = session.realms().getRealms();
                holder.realms = realms;
            }

        });

        for (RealmModel realm : holder.realms) {
            exportRealmImpl(factory, realm.getName());
        }
    }

    @Override
    public void exportRealm(KeycloakSessionFactory factory, String realmName) throws IOException {
        exportRealmImpl(factory, realmName);
    }

    protected void exportRealmImpl(KeycloakSessionFactory factory, final String realmName) throws IOException {
        final UsersExportStrategy usersExportStrategy = ExportImportConfig.getUsersExportStrategy();
        final int usersPerFile = ExportImportConfig.getUsersPerFile();
        final UsersHolder usersHolder = new UsersHolder();
        final boolean exportUsersIntoRealmFile = usersExportStrategy == UsersExportStrategy.REALM_FILE;
        FederatedUsersHolder federatedUsersHolder = new FederatedUsersHolder();

        KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                RealmModel realm = session.realms().getRealmByName(realmName);
                RealmRepresentation rep = ExportUtils.exportRealm(session, realm, exportUsersIntoRealmFile, true);
                writeRealm(realmName + "-realm.json", rep);
                logger.info("Realm '" + realmName + "' - data exported");

                // Count total number of users
                if (!exportUsersIntoRealmFile) {
                    usersHolder.totalCount = session.users().getUsersCount(realm, true);
                    federatedUsersHolder.totalCount = session.userFederatedStorage().getStoredUsersCount(realm);
                }
            }

        });

        if (usersExportStrategy != UsersExportStrategy.SKIP && !exportUsersIntoRealmFile) {
            // We need to export users now
            usersHolder.currentPageStart = 0;

            // usersExportStrategy==SAME_FILE  means exporting all users into single file (but separate to realm)
            final int countPerPage = (usersExportStrategy == UsersExportStrategy.SAME_FILE) ? usersHolder.totalCount : usersPerFile;

            while (usersHolder.currentPageStart < usersHolder.totalCount) {
                if (usersHolder.currentPageStart + countPerPage < usersHolder.totalCount) {
                    usersHolder.currentPageEnd = usersHolder.currentPageStart + countPerPage;
                } else {
                    usersHolder.currentPageEnd = usersHolder.totalCount;
                }

                KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

                    @Override
                    protected void runExportImportTask(KeycloakSession session) throws IOException {
                        RealmModel realm = session.realms().getRealmByName(realmName);
                        usersHolder.users = session.users().getUsers(realm, usersHolder.currentPageStart, usersHolder.currentPageEnd - usersHolder.currentPageStart, true);

                        writeUsers(realmName + "-users-" + (usersHolder.currentPageStart / countPerPage) + ".json", session, realm, usersHolder.users);

                        logger.info("Users " + usersHolder.currentPageStart + "-" + (usersHolder.currentPageEnd -1) + " exported");
                    }

                });

                usersHolder.currentPageStart = usersHolder.currentPageEnd;
            }
        }
        if (usersExportStrategy != UsersExportStrategy.SKIP && !exportUsersIntoRealmFile) {
            // We need to export users now
            federatedUsersHolder.currentPageStart = 0;

            // usersExportStrategy==SAME_FILE  means exporting all users into single file (but separate to realm)
            final int countPerPage = (usersExportStrategy == UsersExportStrategy.SAME_FILE) ? federatedUsersHolder.totalCount : usersPerFile;

            while (federatedUsersHolder.currentPageStart < federatedUsersHolder.totalCount) {
                if (federatedUsersHolder.currentPageStart + countPerPage < federatedUsersHolder.totalCount) {
                    federatedUsersHolder.currentPageEnd = federatedUsersHolder.currentPageStart + countPerPage;
                } else {
                    federatedUsersHolder.currentPageEnd = federatedUsersHolder.totalCount;
                }

                KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

                    @Override
                    protected void runExportImportTask(KeycloakSession session) throws IOException {
                        RealmModel realm = session.realms().getRealmByName(realmName);
                        federatedUsersHolder.users = session.userFederatedStorage().getStoredUsers(realm, federatedUsersHolder.currentPageStart, federatedUsersHolder.currentPageEnd - federatedUsersHolder.currentPageStart);

                        writeFederatedUsers(realmName + "-federated-users-" + (federatedUsersHolder.currentPageStart / countPerPage) + ".json", session, realm, federatedUsersHolder.users);

                        logger.info("Users " + federatedUsersHolder.currentPageStart + "-" + (federatedUsersHolder.currentPageEnd -1) + " exported");
                    }

                });

                federatedUsersHolder.currentPageStart = federatedUsersHolder.currentPageEnd;
            }
        }
    }

    protected abstract void writeRealm(String fileName, RealmRepresentation rep) throws IOException;

    protected abstract void writeUsers(String fileName, KeycloakSession session, RealmModel realm, List<UserModel> users) throws IOException;
    protected abstract void writeFederatedUsers(String fileName, KeycloakSession session, RealmModel realm, List<String> users) throws IOException;

    public static class RealmsHolder {
        List<RealmModel> realms;

    }

    public static class UsersHolder {
        List<UserModel> users;
        int totalCount;
        int currentPageStart;
        int currentPageEnd;
    }
    public static class FederatedUsersHolder {
        List<String> users;
        int totalCount;
        int currentPageStart;
        int currentPageEnd;
    }
}
