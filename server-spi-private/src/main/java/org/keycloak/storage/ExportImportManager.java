/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage;

import java.io.InputStream;

import org.keycloak.exportimport.ExportAdapter;
import org.keycloak.exportimport.ExportOptions;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Manage importing and updating of realms for the store.
 *
 * @author Alexander Schwartz
 */
public interface ExportImportManager {
    void importRealm(RealmRepresentation rep, RealmModel newRealm, Runnable userImport);

    PartialImportResults partialImportRealm(RealmModel realm, InputStream requestBody);

    void updateRealm(RealmRepresentation rep, RealmModel realm);

    UserModel createUser(RealmModel realm, UserRepresentation userRep);

    void exportRealm(RealmModel realm, ExportOptions options, ExportAdapter callback);

    RealmModel importRealm(InputStream requestBody);
}
