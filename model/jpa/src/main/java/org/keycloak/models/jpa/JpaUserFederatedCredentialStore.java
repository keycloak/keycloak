/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.federated.UserFederatedUserCredentialStore;
import org.keycloak.storage.jpa.entity.FederatedUser;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.utils.StreamsUtil.closing;

/**
 * Manages credential storage for federated users.
 *
 * @author Alexander Schwartz
 */
public class JpaUserFederatedCredentialStore implements UserFederatedUserCredentialStore {

    private final KeycloakSession session;

    private final EntityManager em;

    protected static final Logger logger = Logger.getLogger(JpaUserFederatedCredentialStore.class);

    public JpaUserFederatedCredentialStore(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public void updateCredential(RealmModel realm, String userId, CredentialModel cred) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, cred.getId());
        if (!checkCredentialEntity(entity, userId)) return;
        createIndex(realm, userId);
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setType(cred.getType());
        entity.setCredentialData(cred.getCredentialData());
        entity.setSecretData(cred.getSecretData());
        entity.setUserLabel(cred.getUserLabel());
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, String userId, CredentialModel cred) {
        createIndex(realm, userId);
        FederatedUserCredentialEntity entity = new FederatedUserCredentialEntity();
        String id = cred.getId() == null ? KeycloakModelUtils.generateId() : cred.getId();
        entity.setId(id);
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setType(cred.getType());
        entity.setCredentialData(cred.getCredentialData());
        entity.setSecretData(cred.getSecretData());
        entity.setUserLabel(cred.getUserLabel());

        entity.setUserId(userId);
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());

        //add in linkedlist to last position
        List<FederatedUserCredentialEntity> credentials = getStoredCredentialEntitiesStream(userId).collect(Collectors.toList());
        int priority = credentials.isEmpty() ? JpaUserCredentialStore.PRIORITY_DIFFERENCE : credentials.get(credentials.size() - 1).getPriority() + JpaUserCredentialStore.PRIORITY_DIFFERENCE;
        entity.setPriority(priority);

        em.persist(entity);
        return toModel(entity);
    }

    /**
     * We create an entry so that its easy to iterate over all things in the database.  Specifically useful for export
     *
     */
    protected void createIndex(RealmModel realm, String userId) {
        if (em.find(FederatedUser.class, userId) == null) {
            FederatedUser fedUser = new FederatedUser();
            fedUser.setId(userId);
            fedUser.setRealmId(realm.getId());
            fedUser.setStorageProviderId(new StorageId(userId).getProviderId());
            em.persist(fedUser);
        }
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, String userId, String id) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (!checkCredentialEntity(entity, userId)) return false;

        int currentPriority = entity.getPriority();

        this.getStoredCredentialEntitiesStream(userId).filter(credentialEntity -> credentialEntity.getPriority() > currentPriority)
                .forEach(credentialEntity -> credentialEntity.setPriority(credentialEntity.getPriority() - JpaUserCredentialStore.PRIORITY_DIFFERENCE));

        em.remove(entity);
        return true;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, String userId, String id) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, id);
        if (!checkCredentialEntity(entity, userId)) return null;
        CredentialModel model = toModel(entity);
        return model;
    }

    private boolean checkCredentialEntity(FederatedUserCredentialEntity entity, String userId) {
        return entity != null && entity.getUserId() != null && entity.getUserId().equals(userId);
    }

    protected CredentialModel toModel(FederatedUserCredentialEntity entity) {
        CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setCreatedDate(entity.getCreatedDate());
        model.setUserLabel(entity.getUserLabel());

        // Backwards compatibility - users from previous version still have "salt" in the DB filled.
        // We migrate it to new secretData format on-the-fly
        if (entity.getSalt() != null) {
            String newSecretData = entity.getSecretData().replace("__SALT__", Base64.encodeBytes(entity.getSalt()));
            entity.setSecretData(newSecretData);
            entity.setSalt(null);
        }

        model.setSecretData(entity.getSecretData());
        model.setCredentialData(entity.getCredentialData());
        return model;
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, String userId) {
        return this.getStoredCredentialEntitiesStream(userId).map(this::toModel);
    }

    private Stream<FederatedUserCredentialEntity> getStoredCredentialEntitiesStream(String userId) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByUser", FederatedUserCredentialEntity.class)
                .setParameter("userId", userId);
        return closing(query.getResultStream());
    }


    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, String userId, String type) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByUserAndType", FederatedUserCredentialEntity.class)
                .setParameter("type", type)
                .setParameter("userId", userId);
        return closing(query.getResultStream().map(this::toModel));
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, String userId, String name, String type) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByNameAndType", FederatedUserCredentialEntity.class)
                .setParameter("type", type)
                .setParameter("userLabel", name)
                .setParameter("userId", userId);
        List<FederatedUserCredentialEntity> results = query.getResultList();
        if (results.isEmpty()) return null;
        return toModel(results.get(0));
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String credentialId, String newPreviousCredentialId) {
        return moveCredentialTo(realm, user.getId(), credentialId, newPreviousCredentialId);
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, String userId, String credentialId, String newPreviousCredentialId) {

        // 1 - Create new list and move everything to it.
        List<FederatedUserCredentialEntity> newList = this.getStoredCredentialEntitiesStream(userId).collect(Collectors.toList());

        // 2 - Find indexes of our and newPrevious credential
        int ourCredentialIndex = -1;
        int newPreviousCredentialIndex = -1;
        FederatedUserCredentialEntity ourCredential = null;
        int i = 0;
        for (FederatedUserCredentialEntity credential : newList) {
            if (credentialId.equals(credential.getId())) {
                ourCredentialIndex = i;
                ourCredential = credential;
            } else if(newPreviousCredentialId != null && newPreviousCredentialId.equals(credential.getId())) {
                newPreviousCredentialIndex = i;
            }
            i++;
        }

        if (ourCredentialIndex == -1) {
            logger.warnf("Not found credential with id [%s] of user with id [%s]", credentialId, userId);
            return false;
        }

        if (newPreviousCredentialId != null && newPreviousCredentialIndex == -1) {
            logger.warnf("Can't move up credential with id [%s] of user with id [%s]", credentialId, userId);
            return false;
        }

        // 3 - Compute index where we move our credential
        int toMoveIndex = newPreviousCredentialId==null ? 0 : newPreviousCredentialIndex + 1;

        // 4 - Insert our credential to new position, remove it from the old position
        newList.add(toMoveIndex, ourCredential);
        int indexToRemove = toMoveIndex < ourCredentialIndex ? ourCredentialIndex + 1 : ourCredentialIndex;
        newList.remove(indexToRemove);

        // 5 - newList contains credentials in requested order now. Iterate through whole list and change priorities accordingly.
        int expectedPriority = 0;
        for (FederatedUserCredentialEntity credential : newList) {
            expectedPriority += JpaUserCredentialStore.PRIORITY_DIFFERENCE;
            if (credential.getPriority() != expectedPriority) {
                credential.setPriority(expectedPriority);

                logger.tracef("Priority of credential [%s] of user with id [%s] changed to [%d]", credential.getId(), userId, expectedPriority);
            }
        }
        return true;
    }

    @Override
    public void close() {

    }
}