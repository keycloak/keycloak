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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.connections.jpa.support.EntityManagers;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.UsersExportStrategy;
import org.keycloak.exportimport.util.ExportImportSessionTask.Mode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class MultipleStepsExportProvider<T extends MultipleStepsExportProvider<?>> implements ExportProvider {

    protected final Logger logger = Logger.getLogger(getClass());

    protected final KeycloakSessionFactory factory;

    private String realmId;
    private int usersPerFile = 50;
    private UsersExportStrategy usersExportStrategy;

    public MultipleStepsExportProvider(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void exportModel() {
        if (realmId != null) {
            ServicesLogger.LOGGER.realmExportRequested(realmId);
            exportRealmImpl(realmId);
        } else {
            ServicesLogger.LOGGER.fullModelExportRequested();
            List<RealmModel>[] realms = new List[1];
            new ExportImportSessionTask() {
                @Override
                protected void runExportImportTask(KeycloakSession session) throws IOException {
                    realms[0] = session.realms().getRealmsStream().collect(Collectors.toList());
                }
            }.runTask(factory);
            for (RealmModel realm : realms[0]) {
                exportRealmImpl(realm.getName());
            }
        }
        ServicesLogger.LOGGER.exportSuccess();
    }

    public T withRealmName(String realmName) {
        this.realmId = realmName;
        return (T) this;
    }

    public T withUsersPerFile(int usersPerFile) {
        if (usersPerFile < 1) {
            throw new IllegalArgumentException("usersPerFile must be greater than 0");
        }
        this.usersPerFile = usersPerFile;
        return (T) this;
    }

    public T withUsersExportStrategy(UsersExportStrategy usersExportStrategy) {
        this.usersExportStrategy = usersExportStrategy;
        return (T) this;
    }

    protected void exportRealmImpl(final String realmName) {
        final boolean exportUsersIntoRealmFile = usersExportStrategy == UsersExportStrategy.REALM_FILE;

        new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                RealmModel realm = session.realms().getRealmByName(realmName);
                Objects.requireNonNull(realm, "realm not found by realm name '" + realmName + "'");
                session.getContext().setRealm(realm);
                RealmRepresentation rep = ExportUtils.exportRealm(session, realm, exportUsersIntoRealmFile, true);
                writeRealm(realmName + "-realm.json", rep);
                logger.info("Realm '" + realmName + "' - data exported");

                // Count total number of users
                if (usersExportStrategy != UsersExportStrategy.SKIP && !exportUsersIntoRealmFile) {
                    Stream<UserModel> users = UserStoragePrivateUtil.userLocalStorage(session).searchForUserStream(realm, Map.of());
                    exportUsers(realmName, session, realm, users, false);

                    if (UserStorageUtil.userFederatedStorage(session) != null) {
                        Stream<String> federatedUsers = UserStorageUtil.userFederatedStorage(session).getStoredUsersStream(realm, null, null);
                        exportUsers(realmName, session, realm, federatedUsers, true);
                    }
                }
            }

            private <U> void exportUsers(final String realmName, KeycloakSession session, RealmModel realm, Stream<U> users, boolean federated)
                    throws IOException {
                final UsersHolder usersHolder = new UsersHolder();

                final Integer countPerPage = (usersExportStrategy == UsersExportStrategy.SAME_FILE) ? null : usersPerFile;

                List<U> usersBatch = new ArrayList<U>();

                users.forEachOrdered(user -> {
                    usersBatch.add(user);

                    if (countPerPage != null && usersBatch.size() >= countPerPage) {
                        try {
                            flushUsers(realmName, session, realm, usersHolder, countPerPage, usersBatch, federated);
                        } catch (IOException e) {
                            throw new RuntimeException("Error during export/import: " + e.getMessage(), e);
                        }
                    }
                });

                if (!usersBatch.isEmpty()) {
                    flushUsers(realmName, session, realm, usersHolder, countPerPage, usersBatch, federated);
                }
            }

            private <U> void flushUsers(final String realmName, KeycloakSession session, RealmModel realm,
                    final UsersHolder usersHolder, final Integer countPerPage, List<U> usersBatch, boolean federated)
                    throws IOException {
                if (federated) {
                    writeFederatedUsers(realmName + "-federated-users-" + usersHolder.file + ".json", session, realm, (List<String>) usersBatch);
                } else {
                    writeUsers(realmName + "-users-" + usersHolder.file + ".json", session, realm, (List<UserModel>) usersBatch);
                }
                int start = countPerPage == null ? 0 : countPerPage * usersHolder.file;
                logger.infof("%sUsers %s - %s exported", federated ? "Federated " : "", start, start + usersBatch.size() - 1);
                usersHolder.file++;
                usersBatch.clear();
                EntityManagers.flush(session, true);
            }

        }.runTask(factory, Mode.BATCHED);
    }

    protected abstract void writeRealm(String fileName, RealmRepresentation rep) throws IOException;

    protected abstract void writeUsers(String fileName, KeycloakSession session, RealmModel realm, List<UserModel> users) throws IOException;
    protected abstract void writeFederatedUsers(String fileName, KeycloakSession session, RealmModel realm, List<String> users) throws IOException;

    public static class UsersHolder {
        int file;
    }
}
