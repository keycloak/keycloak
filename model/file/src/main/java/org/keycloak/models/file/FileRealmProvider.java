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

import org.keycloak.models.file.adapter.RealmAdapter;
import java.util.ArrayList;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.List;
import org.keycloak.connections.file.FileConnectionProvider;
import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.entities.RealmEntity;

/**
 * Realm Provider for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class FileRealmProvider implements RealmProvider {

    private final KeycloakSession session;
    private FileConnectionProvider fcProvider;
    private final InMemoryModel inMemoryModel;

    public FileRealmProvider(KeycloakSession session, FileConnectionProvider fcProvider) {
        this.session = session;
        this.fcProvider = fcProvider;
        session.enlistForClose(this);
        this.inMemoryModel = fcProvider.getModel();
    }

    @Override
    public void close() {
        fcProvider.sessionClosed(session);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        if (getRealmByName(name) != null) throw new ModelDuplicateException("Realm " + name + " already exists.");
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setName(name);
        realmEntity.setId(id);
        RealmAdapter realm = new RealmAdapter(session, realmEntity, inMemoryModel);
        inMemoryModel.putRealm(id, realm);

        return realm;
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmModel model = inMemoryModel.getRealm(id);
        return model;
    }

    @Override
    public List<RealmModel> getRealms() {
       return new ArrayList(inMemoryModel.getRealms());
    }

    @Override
    public RealmModel getRealmByName(String name) {
        RealmModel model = inMemoryModel.getRealmByName(name);
        return model;
    }

    @Override
    public boolean removeRealm(String id) {
        return inMemoryModel.removeRealm(id);
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        return realm.getRoleById(id);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        return realm.getApplicationById(id);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        return realm.getOAuthClientById(id);
    }

}
