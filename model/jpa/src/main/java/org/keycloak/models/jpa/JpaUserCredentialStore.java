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
package org.keycloak.models.jpa;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.LockModeType;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserCredentialStore implements UserCredentialStore {

    // Typical priority difference between 2 credentials
    public static final int PRIORITY_DIFFERENCE = 10;

    protected static final Logger logger = Logger.getLogger(JpaUserCredentialStore.class);

    private final KeycloakSession session;
    protected final EntityManager em;

    public JpaUserCredentialStore(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        CredentialEntity entity = em.find(CredentialEntity.class, cred.getId());
        if (entity == null) return;
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setUserLabel(cred.getUserLabel());
        entity.setType(cred.getType());
        entity.setSecretData(cred.getSecretData());
        entity.setCredentialData(cred.getCredentialData());
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        CredentialEntity entity = createCredentialEntity(realm, user, cred);
        return toModel(entity);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        CredentialEntity entity = removeCredentialEntity(realm, user, id);
        return entity != null;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        CredentialEntity entity = em.find(CredentialEntity.class, id);
        if (entity == null) return null;
        CredentialModel model = toModel(entity);
        return model;
    }

    CredentialModel toModel(CredentialEntity entity) {
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
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        List<CredentialEntity> results = getStoredCredentialEntities(realm, user);

        // list is ordered correctly by priority (lowest priority value first)
        return results.stream().map(this::toModel).collect(Collectors.toList());
    }

    private List<CredentialEntity> getStoredCredentialEntities(RealmModel realm, UserModel user) {
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        TypedQuery<CredentialEntity> query = em.createNamedQuery("credentialByUser", CredentialEntity.class)
                .setParameter("user", userEntity);
        return query.getResultList();
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        return getStoredCredentials(realm, user).stream().filter(credential -> type.equals(credential.getType())).collect(Collectors.toList());
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        List<CredentialModel> results = getStoredCredentials(realm, user).stream().filter(credential ->
                type.equals(credential.getType()) && name.equals(credential.getUserLabel())).collect(Collectors.toList());
        if (results.isEmpty()) return null;
        return results.get(0);
    }

    @Override
    public void close() {

    }

    CredentialEntity createCredentialEntity(RealmModel realm, UserModel user, CredentialModel cred) {
        CredentialEntity entity = new CredentialEntity();
        String id = cred.getId() == null ? KeycloakModelUtils.generateId() : cred.getId();
        entity.setId(id);
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setUserLabel(cred.getUserLabel());
        entity.setType(cred.getType());
        entity.setSecretData(cred.getSecretData());
        entity.setCredentialData(cred.getCredentialData());
        UserEntity userRef = em.getReference(UserEntity.class, user.getId());
        entity.setUser(userRef);

        //add in linkedlist to last position
        List<CredentialEntity> credentials = getStoredCredentialEntities(realm, user);
        int priority = credentials.isEmpty() ? PRIORITY_DIFFERENCE : credentials.get(credentials.size() - 1).getPriority() + PRIORITY_DIFFERENCE;
        entity.setPriority(priority);

        em.persist(entity);
        return entity;
    }

    CredentialEntity removeCredentialEntity(RealmModel realm, UserModel user, String id) {
        CredentialEntity entity = em.find(CredentialEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) return null;

        int currentPriority = entity.getPriority();

        List<CredentialEntity> credentials = getStoredCredentialEntities(realm, user);

        // Decrease priority of all credentials after our
        for (CredentialEntity cred : credentials) {
            if (cred.getPriority() > currentPriority) {
                cred.setPriority(cred.getPriority() - PRIORITY_DIFFERENCE);
            }
        }

        em.remove(entity);
        em.flush();
        return entity;
    }

    ////Operations to handle the linked list of credentials
    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId) {
        List<CredentialEntity> sortedCreds = getStoredCredentialEntities(realm, user);

        // 1 - Create new list and move everything to it.
        List<CredentialEntity> newList = new ArrayList<>();
        newList.addAll(sortedCreds);

        // 2 - Find indexes of our and newPrevious credential
        int ourCredentialIndex = -1;
        int newPreviousCredentialIndex = -1;
        CredentialEntity ourCredential = null;
        int i = 0;
        for (CredentialEntity credential : newList) {
            if (id.equals(credential.getId())) {
                ourCredentialIndex = i;
                ourCredential = credential;
            } else if(newPreviousCredentialId != null && newPreviousCredentialId.equals(credential.getId())) {
                newPreviousCredentialIndex = i;
            }
            i++;
        }

        if (ourCredentialIndex == -1) {
            logger.warnf("Not found credential with id [%s] of user [%s]", id, user.getUsername());
            return false;
        }

        if (newPreviousCredentialId != null && newPreviousCredentialIndex == -1) {
            logger.warnf("Can't move up credential with id [%s] of user [%s]", id, user.getUsername());
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
        for (CredentialEntity credential : newList) {
            expectedPriority += PRIORITY_DIFFERENCE;
            if (credential.getPriority() != expectedPriority) {
                credential.setPriority(expectedPriority);

                logger.tracef("Priority of credential [%s] of user [%s] changed to [%d]", credential.getId(), user.getUsername(), expectedPriority);
            }
        }
        return true;
    }

}
