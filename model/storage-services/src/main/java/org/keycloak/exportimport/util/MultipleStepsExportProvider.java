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
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.UsersExportStrategy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.storage.UserStorageUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class MultipleStepsExportProvider<T extends MultipleStepsExportProvider<?>> implements ExportProvider {

    protected final Logger logger = Logger.getLogger(getClass());

    protected final KeycloakSessionFactory factory;

    private String realmId;
    private int usersPerFile;
    private UsersExportStrategy usersExportStrategy;
    private final JpaConnectionProvider connectionProvider;

    public MultipleStepsExportProvider(KeycloakSessionFactory factory, JpaConnectionProvider connectionProvider) {
        this.factory = factory;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void exportModel() {
        KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                if (realmId != null) {
                    ServicesLogger.LOGGER.realmExportRequested(realmId);
                    exportRealm(realmId);
                } else {
                    ServicesLogger.LOGGER.fullModelExportRequested();
                            session.realms().getRealmsStream().map(RealmModel::getName)
                                    .forEach(MultipleStepsExportProvider.this::exportRealm);
                }
            }
        });
        ServicesLogger.LOGGER.exportSuccess();
    }

    public T withRealmName(String realmName) {
        this.realmId = realmName;
        return (T) this;
    }

    public T withUsersPerFile(int usersPerFile) {
        this.usersPerFile = usersPerFile;
        return (T) this;
    }

    public T withUsersExportStrategy(UsersExportStrategy usersExportStrategy) {
        this.usersExportStrategy = usersExportStrategy;
        return (T) this;
    }

    private void exportRealm(String realmName) {
        // the main benefit of running in a batch here is to ensure
        // that queries are in flushMode COMMIT
        // there is also some clearing of the context to limit how much is in memory when running under a single
        // transaction - this could be further optimized if the writing strategy were more streaming, rather than being based upon lists
        this.connectionProvider.runInBatch(batchControl -> exportRealmImpl(realmName, batchControl));
    }

    private void exportRealmImpl(final String realmName, JpaConnectionProvider.BatchControl batchControl) {
        final UsersHolder usersHolder = new UsersHolder();
        final boolean exportUsersIntoRealmFile = usersExportStrategy == UsersExportStrategy.REALM_FILE;
        FederatedUsersHolder federatedUsersHolder = new FederatedUsersHolder();

        KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                RealmModel realm = session.realms().getRealmByName(realmName);
                session.getContext().setRealm(realm);
                RealmRepresentation rep = ExportUtils.exportRealm(session, realm, exportUsersIntoRealmFile, true);
                writeRealm(realmName + "-realm.json", rep);
                logger.info("Realm '" + realmName + "' - data exported");

                // Count total number of users
                if (!exportUsersIntoRealmFile) {
                    usersHolder.totalCount = session.users().getUsersCount(realm, true);
                    if (UserStorageUtil.userFederatedStorage(session) != null) {
                        federatedUsersHolder.totalCount = UserStorageUtil.userFederatedStorage(session).getStoredUsersCount(realm);
                    } else {
                        federatedUsersHolder.totalCount = 0;
                    }
                }
                batchControl.clear();
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
                        session.getContext().setRealm(realm);
                        usersHolder.users = session.users()
                                .searchForUserStream(realm, Collections.emptyMap(), usersHolder.currentPageStart, usersHolder.currentPageEnd - usersHolder.currentPageStart)
                                .collect(Collectors.toList());

                        writeUsers(realmName + "-users-" + (usersHolder.currentPageStart / countPerPage) + ".json", session, realm, usersHolder.users);

                        logger.info("Users " + usersHolder.currentPageStart + "-" + (usersHolder.currentPageEnd -1) + " exported");
                    }

                });

                usersHolder.currentPageStart = usersHolder.currentPageEnd;
                batchControl.clear();
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
                        session.getContext().setRealm(realm);
                        federatedUsersHolder.users = UserStorageUtil.userFederatedStorage(session)
                                .getStoredUsersStream(realm, federatedUsersHolder.currentPageStart, federatedUsersHolder.currentPageEnd - federatedUsersHolder.currentPageStart)
                                .collect(Collectors.toList());

                        writeFederatedUsers(realmName + "-federated-users-" + (federatedUsersHolder.currentPageStart / countPerPage) + ".json", session, realm, federatedUsersHolder.users);

                        logger.info("Users " + federatedUsersHolder.currentPageStart + "-" + (federatedUsersHolder.currentPageEnd -1) + " exported");
                    }

                });

                federatedUsersHolder.currentPageStart = federatedUsersHolder.currentPageEnd;
                batchControl.clear();
            }
        }
    }

    protected abstract void writeRealm(String fileName, RealmRepresentation rep) throws IOException;

    protected abstract void writeUsers(String fileName, KeycloakSession session, RealmModel realm, List<UserModel> users) throws IOException;
    protected abstract void writeFederatedUsers(String fileName, KeycloakSession session, RealmModel realm, List<String> users) throws IOException;

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
