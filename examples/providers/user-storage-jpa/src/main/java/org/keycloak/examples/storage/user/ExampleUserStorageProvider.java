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
package org.keycloak.examples.storage.user;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ExampleUserStorageProvider implements UserStorageProvider, UserLookupProvider, UserRegistrationProvider {
    protected EntityManager em;
    protected ComponentModel model;
    protected KeycloakSession session;

    public ExampleUserStorageProvider(EntityManager em, ComponentModel model, KeycloakSession session) {
        this.em = em;
        this.model = model;
        this.session = session;
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void close() {
        em.close();
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        String persistenceId = StorageId.externalId(id);
        UserEntity entity = em.find(UserEntity.class, persistenceId);
        if (entity == null) return null;
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getUserByUsername", UserEntity.class);
        query.setParameter("username", username);
        List<UserEntity> result = query.getResultList();
        if (result.isEmpty()) return null;
        return new UserAdapter(session, realm, model, result.get(0));
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        List<UserEntity> result = query.getResultList();
        if (result.isEmpty()) return null;
        return new UserAdapter(session, realm, model, result.get(0));
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserEntity entity = new UserEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setUsername(username);
        em.persist(entity);
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String persistenceId = StorageId.externalId(user.getId());
        UserEntity entity = em.find(UserEntity.class, persistenceId);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {

    }
}
