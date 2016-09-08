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
package org.keycloak.storage.jpa;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialAttributeEntity;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaFederatedUserCredentialStore implements UserCredentialStore {

    private final KeycloakSession session;
    protected final EntityManager em;

    public JpaFederatedUserCredentialStore(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, cred.getId());
        if (entity == null) return;
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());
        if (entity.getCredentialAttributes().isEmpty() && (cred.getConfig() == null || cred.getConfig().isEmpty())) {

        } else {
            MultivaluedHashMap<String, String> attrs = cred.getConfig();
            MultivaluedHashMap<String, String> config = cred.getConfig();
            if (config == null) config = new MultivaluedHashMap<>();

            Iterator<FederatedUserCredentialAttributeEntity> it = entity.getCredentialAttributes().iterator();
            while (it.hasNext()) {
                FederatedUserCredentialAttributeEntity attr = it.next();
                List<String> values = config.getList(attr.getName());
                if (values == null || !values.contains(attr.getValue())) {
                    em.remove(attr);
                    it.remove();
                } else {
                    attrs.add(attr.getName(), attr.getValue());
                }

            }
            for (String key : config.keySet()) {
                List<String> values = config.getList(key);
                List<String> attrValues = attrs.getList(key);
                for (String val : values) {
                    if (attrValues == null || !attrValues.contains(val)) {
                        FederatedUserCredentialAttributeEntity attr = new FederatedUserCredentialAttributeEntity();
                        attr.setId(KeycloakModelUtils.generateId());
                        attr.setValue(val);
                        attr.setName(key);
                        attr.setCredential(entity);
                        em.persist(attr);
                        entity.getCredentialAttributes().add(attr);
                    }
                }
            }

        }

    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        FederatedUserCredentialEntity entity = new FederatedUserCredentialEntity();
        String id = cred.getId() == null ? KeycloakModelUtils.generateId() : cred.getId();
        entity.setId(id);
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());
        entity.setUserId(user.getId());
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(StorageId.resolveProviderId(user));
        em.persist(entity);
        MultivaluedHashMap<String, String> config = cred.getConfig();
        if (config != null || !config.isEmpty()) {

            for (String key : config.keySet()) {
                List<String> values = config.getList(key);
                for (String val : values) {
                    FederatedUserCredentialAttributeEntity attr = new FederatedUserCredentialAttributeEntity();
                    attr.setId(KeycloakModelUtils.generateId());
                    attr.setValue(val);
                    attr.setName(key);
                    attr.setCredential(entity);
                    em.persist(attr);
                    entity.getCredentialAttributes().add(attr);
                }
            }

        }
        return toModel(entity);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, id);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, id);
        if (entity == null) return null;
        CredentialModel model = toModel(entity);
        return model;
    }

    protected CredentialModel toModel(FederatedUserCredentialEntity entity) {
        CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setValue(entity.getValue());
        model.setAlgorithm(entity.getAlgorithm());
        model.setSalt(entity.getSalt());
        model.setPeriod(entity.getPeriod());
        model.setCounter(entity.getCounter());
        model.setCreatedDate(entity.getCreatedDate());
        model.setDevice(entity.getDevice());
        model.setDigits(entity.getDigits());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        model.setConfig(config);
        for (FederatedUserCredentialAttributeEntity attr : entity.getCredentialAttributes()) {
            config.add(attr.getName(), attr.getValue());
        }
        return model;
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByUser", FederatedUserCredentialEntity.class)
                .setParameter("userId", user.getId());
        List<FederatedUserCredentialEntity> results = query.getResultList();
        List<CredentialModel> rtn = new LinkedList<>();
        for (FederatedUserCredentialEntity entity : results) {
            rtn.add(toModel(entity));
        }
        return rtn;
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByUserAndType", FederatedUserCredentialEntity.class)
                .setParameter("type", type)
                .setParameter("userId", user.getId());
        List<FederatedUserCredentialEntity> results = query.getResultList();
        List<CredentialModel> rtn = new LinkedList<>();
        for (FederatedUserCredentialEntity entity : results) {
            rtn.add(toModel(entity));
        }
        return rtn;
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByNameAndType", FederatedUserCredentialEntity.class)
                .setParameter("type", type)
                .setParameter("device", name)
                .setParameter("userId", user.getId());
        List<FederatedUserCredentialEntity> results = query.getResultList();
        if (results.isEmpty()) return null;
        return toModel(results.get(0));
    }

    @Override
    public void close() {

    }
}
