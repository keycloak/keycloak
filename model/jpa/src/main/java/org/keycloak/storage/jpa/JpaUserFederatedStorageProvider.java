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

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaUserCredentialStore;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.jpa.entity.BrokerLinkEntity;
import org.keycloak.storage.jpa.entity.FederatedUser;
import org.keycloak.storage.jpa.entity.FederatedUserAttributeEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentClientScopeEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentEntity;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;
import org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity;
import org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity;
import org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity.Key;
import org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.LockModeType;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserFederatedStorageProvider implements
        UserFederatedStorageProvider.Streams,
        UserCredentialStore.Streams {

    protected static final Logger logger = Logger.getLogger(JpaUserFederatedStorageProvider.class);

    private final KeycloakSession session;
    protected EntityManager em;

    public JpaUserFederatedStorageProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public void close() {

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
    public void setAttribute(RealmModel realm, String userId, String name, List<String> values) {
        createIndex(realm, userId);
        deleteAttribute(realm, userId, name);
        em.flush();
        for (String value : values) {
            persistAttributeValue(realm, userId, name, value);
        }
    }

    private void deleteAttribute(RealmModel realm, String userId, String name) {
        em.createNamedQuery("deleteUserFederatedAttributesByUserAndName")
                .setParameter("userId", userId)
                .setParameter("realmId", realm.getId())
                .setParameter("name", name)
                .executeUpdate();
    }

    private void persistAttributeValue(RealmModel realm, String userId, String name, String value) {
        FederatedUserAttributeEntity attr = new FederatedUserAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setUserId(userId);
        attr.setRealmId(realm.getId());
        attr.setStorageProviderId(new StorageId(userId).getProviderId());
        em.persist(attr);
    }

    @Override
    public void setSingleAttribute(RealmModel realm, String userId, String name, String value) {
        createIndex(realm, userId);
        deleteAttribute(realm, userId, name);
        em.flush();
        persistAttributeValue(realm, userId, name, value);
    }

    @Override
    public void removeAttribute(RealmModel realm, String userId, String name) {
        //         createIndex(realm, user); don't need to create an index for removal
        deleteAttribute(realm, userId, name);
        em.flush();
    }

    @Override
    public MultivaluedHashMap<String, String> getAttributes(RealmModel realm, String userId) {
        TypedQuery<FederatedUserAttributeEntity> query = em.createNamedQuery("getFederatedAttributesByUser", FederatedUserAttributeEntity.class);
        List<FederatedUserAttributeEntity> list = query
                .setParameter("userId", userId)
                .setParameter("realmId", realm.getId())
                .getResultList();
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>();
        for (FederatedUserAttributeEntity entity : list) {
            result.add(entity.getName(), entity.getValue());

        }
        return result;
    }

    @Override
    public Stream<String> getUsersByUserAttributeStream(RealmModel realm, String name, String value) {
        TypedQuery<String> query = em.createNamedQuery("getFederatedAttributesByNameAndValue", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("name", name)
                .setParameter("value", value);
        return closing(query.getResultStream());
    }

    @Override
    public String getUserByFederatedIdentity(FederatedIdentityModel link, RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("findUserByBrokerLinkAndRealm", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("identityProvider", link.getIdentityProvider())
                .setParameter("brokerUserId", link.getUserId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for identityProvider=" + link.getIdentityProvider() +
                    ", userId=" + link.getUserId() + ", results=" + results);
        } else {
            return results.get(0);
        }
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel link) {
        createIndex(realm, userId);
        BrokerLinkEntity entity = new BrokerLinkEntity();
        entity.setRealmId(realm.getId());
        entity.setUserId(userId);
        entity.setBrokerUserId(link.getUserId());
        entity.setIdentityProvider(link.getIdentityProvider());
        entity.setToken(link.getToken());
        entity.setBrokerUserName(link.getUserName());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        em.persist(entity);

    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, String userId, String socialProvider) {
        BrokerLinkEntity entity = getBrokerLinkEntity(realm, userId, socialProvider);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }

    @Override 
    public void preRemove(RealmModel realm, IdentityProviderModel provider) {
        em.createNamedQuery("deleteBrokerLinkByIdentityProvider")
                .setParameter("realmId", realm.getId())
                .setParameter("providerAlias", provider.getAlias());
    }

    private BrokerLinkEntity getBrokerLinkEntity(RealmModel realm, String userId, String socialProvider) {
        TypedQuery<BrokerLinkEntity> query = em.createNamedQuery("findBrokerLinkByUserAndProvider", BrokerLinkEntity.class)
                .setParameter("userId", userId)
                .setParameter("realmId", realm.getId())
                .setParameter("identityProvider", socialProvider);
        List<BrokerLinkEntity> results = query.getResultList();
        return results.size() > 0 ? results.get(0) : null;
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel model) {
        createIndex(realm, userId);
        BrokerLinkEntity entity = getBrokerLinkEntity(realm, userId, model.getIdentityProvider());
        if (entity == null) return;
        entity.setBrokerUserName(model.getUserName());
        entity.setBrokerUserId(model.getUserId());
        entity.setToken(model.getToken());
        em.persist(entity);
        em.flush();

    }

    @Override
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(String userId, RealmModel realm) {
        TypedQuery<BrokerLinkEntity> query = em.createNamedQuery("findBrokerLinkByUser", BrokerLinkEntity.class)
                .setParameter("userId", userId);
        return closing(query.getResultStream().map(entity -> new FederatedIdentityModel(entity.getIdentityProvider(),
                entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken())).distinct());
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(String userId, String socialProvider, RealmModel realm) {
        BrokerLinkEntity entity = getBrokerLinkEntity(realm, userId, socialProvider);
        if (entity == null) return null;
        return new FederatedIdentityModel(entity.getIdentityProvider(), entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken());
    }

    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        createIndex(realm, userId);
        String clientId = consent.getClient().getId();

        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId, LockModeType.NONE);
        if (consentEntity != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + userId + "]");
        }

        consentEntity = new FederatedUserConsentEntity();
        consentEntity.setId(KeycloakModelUtils.generateId());
        consentEntity.setUserId(userId);
        StorageId clientStorageId = new StorageId(clientId);
        if (clientStorageId.isLocal()) {
            consentEntity.setClientId(clientId);
        } else {
            consentEntity.setClientStorageProvider(clientStorageId.getProviderId());
            consentEntity.setExternalClientId(clientStorageId.getExternalId());
        }
        consentEntity.setRealmId(realm.getId());
        consentEntity.setStorageProviderId(new StorageId(userId).getProviderId());
        long currentTime = Time.currentTimeMillis();
        consentEntity.setCreatedDate(currentTime);
        consentEntity.setLastUpdatedDate(currentTime);
        em.persist(consentEntity);
        em.flush();

        updateGrantedConsentEntity(consentEntity, consent);

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId) {
        FederatedUserConsentEntity entity = getGrantedConsentEntity(userId, clientInternalId, LockModeType.NONE);
        return toConsentModel(realm, entity);
    }

    @Override
    public Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        TypedQuery<FederatedUserConsentEntity> query = em.createNamedQuery("userFederatedConsentsByUser", FederatedUserConsentEntity.class);
        query.setParameter("userId", userId);
        return closing(query.getResultStream().map(entity -> toConsentModel(realm, entity)));
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        createIndex(realm, userId);
        String clientId = consent.getClient().getId();

        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId, LockModeType.PESSIMISTIC_WRITE);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + userId + "]");
        }

        updateGrantedConsentEntity(consentEntity, consent);

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientInternalId, LockModeType.PESSIMISTIC_WRITE);
        if (consentEntity == null) return false;

        em.remove(consentEntity);
        em.flush();
        return true;
    }

    private FederatedUserConsentEntity getGrantedConsentEntity(String userId, String clientId, LockModeType lockMode) {
        StorageId clientStorageId = new StorageId(clientId);
        String queryName = clientStorageId.isLocal() ?  "userFederatedConsentByUserAndClient" : "userFederatedConsentByUserAndExternalClient";
        TypedQuery<FederatedUserConsentEntity> query = em.createNamedQuery(queryName, FederatedUserConsentEntity.class);
        query.setLockMode(lockMode);
        query.setParameter("userId", userId);
        if (clientStorageId.isLocal()) {
            query.setParameter("clientId", clientId);
        } else {
            query.setParameter("clientStorageProvider", clientStorageId.getProviderId());
            query.setParameter("externalClientId", clientStorageId.getExternalId());
        }
        List<FederatedUserConsentEntity> results = query.getResultList();
        if (results.size() > 1) {
            throw new ModelException("More results found for user [" + userId + "] and client [" + clientId + "]");
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            return null;
        }
    }


    private UserConsentModel toConsentModel(RealmModel realm, FederatedUserConsentEntity entity) {
        if (entity == null) {
            return null;
        }

        StorageId clientStorageId = null;
        if ( entity.getClientId() == null) {
            clientStorageId = new StorageId(entity.getClientStorageProvider(), entity.getExternalClientId());
        } else {
            clientStorageId = new StorageId(entity.getClientId());
        }

        ClientModel client = realm.getClientById(clientStorageId.getId());
        UserConsentModel model = new UserConsentModel(client);
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());

        Collection<FederatedUserConsentClientScopeEntity> grantedClientScopeEntities = entity.getGrantedClientScopes();
        if (grantedClientScopeEntities != null) {
            for (FederatedUserConsentClientScopeEntity grantedClientScope : grantedClientScopeEntities) {
                ClientScopeModel grantedClientScopeModel = realm.getClientScopeById(grantedClientScope.getScopeId());
                if (grantedClientScopeModel == null) {
                    grantedClientScopeModel = realm.getClientById(grantedClientScope.getScopeId());
                }
                if (grantedClientScopeModel != null) {
                    model.addGrantedClientScope(grantedClientScopeModel);
                }
            }
        }

        return model;
    }

    // Update roles and protocolMappers to given consentEntity from the consentModel
    private void updateGrantedConsentEntity(FederatedUserConsentEntity consentEntity, UserConsentModel consentModel) {
        Collection<FederatedUserConsentClientScopeEntity> grantedClientScopeEntities = consentEntity.getGrantedClientScopes();
        Collection<FederatedUserConsentClientScopeEntity> scopesToRemove = new HashSet<>(grantedClientScopeEntities);

        for (ClientScopeModel clientScope : consentModel.getGrantedClientScopes()) {
            FederatedUserConsentClientScopeEntity grantedClientScopeEntity = new FederatedUserConsentClientScopeEntity();
            grantedClientScopeEntity.setUserConsent(consentEntity);
            grantedClientScopeEntity.setScopeId(clientScope.getId());

            // Check if it's already there
            if (!grantedClientScopeEntities.contains(grantedClientScopeEntity)) {
                em.persist(grantedClientScopeEntity);
                em.flush();
                grantedClientScopeEntities.add(grantedClientScopeEntity);
            } else {
                scopesToRemove.remove(grantedClientScopeEntity);
            }
        }
        // Those mappers were no longer on consentModel and will be removed
        for (FederatedUserConsentClientScopeEntity toRemove : scopesToRemove) {
            grantedClientScopeEntities.remove(toRemove);
            em.remove(toRemove);
        }

        consentEntity.setLastUpdatedDate(Time.currentTimeMillis());

        em.flush();
    }


    @Override
    public void setNotBeforeForUser(RealmModel realm, String userId, int notBefore) {
        // Track it as attribute for now
        String notBeforeStr = String.valueOf(notBefore);
        setSingleAttribute(realm, userId, "fedNotBefore", notBeforeStr);
    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, String userId) {
        MultivaluedHashMap<String, String> attrs = getAttributes(realm, userId);
        String notBeforeStr = attrs.getFirst("fedNotBefore");

        return notBeforeStr==null ? 0 : Integer.parseInt(notBeforeStr);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, String userId) {
        TypedQuery<FederatedUserGroupMembershipEntity> query = em.createNamedQuery("feduserGroupMembership", FederatedUserGroupMembershipEntity.class);
        query.setParameter("userId", userId);
        return closing(query.getResultStream().map(FederatedUserGroupMembershipEntity::getGroupId).map(realm::getGroupById));
    }

    @Override
    public void joinGroup(RealmModel realm, String userId, GroupModel group) {
        createIndex(realm, userId);
        FederatedUserGroupMembershipEntity entity = new FederatedUserGroupMembershipEntity();
        entity.setUserId(userId);
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        entity.setGroupId(group.getId());
        entity.setRealmId(realm.getId());
        em.persist(entity);

    }


    @Override
    public void leaveGroup(RealmModel realm, String userId, GroupModel group) {
        if (userId == null || group == null) return;

        TypedQuery<FederatedUserGroupMembershipEntity> query1 = em.createNamedQuery("feduserMemberOf", FederatedUserGroupMembershipEntity.class);
        query1.setParameter("userId", userId);
        query1.setParameter("groupId", group.getId());
        TypedQuery<FederatedUserGroupMembershipEntity> query = query1;
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<FederatedUserGroupMembershipEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (FederatedUserGroupMembershipEntity entity : results) {
            em.remove(entity);
        }
        em.flush();

    }

    @Override
    public Stream<String> getMembershipStream(RealmModel realm, GroupModel group, Integer firstResult, Integer max) {
        TypedQuery<String> query = em.createNamedQuery("fedgroupMembership", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("groupId", group.getId());

        return closing(paginateQuery(query, firstResult, max).getResultStream());
    }

    @Override
    public Stream<String> getRequiredActionsStream(RealmModel realm, String userId) {
        return this.getRequiredActionEntitiesStream(realm, userId, LockModeType.NONE).
                map(FederatedUserRequiredActionEntity::getAction).distinct();
    }

    private Stream<FederatedUserRequiredActionEntity> getRequiredActionEntitiesStream(RealmModel realm, String userId, LockModeType lockMode) {
        TypedQuery<FederatedUserRequiredActionEntity> query = em.createNamedQuery("getFederatedUserRequiredActionsByUser", FederatedUserRequiredActionEntity.class)
                .setParameter("userId", userId)
                .setParameter("realmId", realm.getId());
        query.setLockMode(lockMode);
        return closing(query.getResultStream());
    }

    @Override
    public void addRequiredAction(RealmModel realm, String userId, String action) {
        Key key = new FederatedUserRequiredActionEntity.Key(userId, action);
        if (em.find(FederatedUserRequiredActionEntity.class, key) == null) {
            createIndex(realm, userId);
            FederatedUserRequiredActionEntity entity = new FederatedUserRequiredActionEntity();
            entity.setUserId(userId);
            entity.setRealmId(realm.getId());
            entity.setStorageProviderId(new StorageId(userId).getProviderId());
            entity.setAction(action);
            em.persist(entity);
        }
    }

    @Override
    public void removeRequiredAction(RealmModel realm, String userId, String action) {
        this.getRequiredActionEntitiesStream(realm, userId, LockModeType.PESSIMISTIC_WRITE).
                filter(entity -> Objects.equals(entity.getAction(), action)).collect(Collectors.toList()).forEach(em::remove);
        em.flush();
    }

    @Override
    public void grantRole(RealmModel realm, String userId, RoleModel role) {
        createIndex(realm, userId);
        FederatedUserRoleMappingEntity entity = new FederatedUserRoleMappingEntity();
        entity.setUserId(userId);
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        entity.setRealmId(realm.getId());
        entity.setRoleId(role.getId());
        em.persist(entity);

    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream(RealmModel realm, String userId) {
        TypedQuery<FederatedUserRoleMappingEntity> query = em.createNamedQuery("feduserRoleMappings", FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", userId);
        return closing(query.getResultStream().map(FederatedUserRoleMappingEntity::getRoleId).map(realm::getRoleById));
    }

    @Override
    public void deleteRoleMapping(RealmModel realm, String userId, RoleModel role) {
        TypedQuery<FederatedUserRoleMappingEntity> query = em.createNamedQuery("feduserRoleMappings", FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", userId);
        List<FederatedUserRoleMappingEntity> results = query.getResultList();
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        for (FederatedUserRoleMappingEntity entity : results) {
            if (entity.getRoleId().equals(role.getId())) em.remove(entity);

        }
        em.flush();
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
        cred.setUserLabel(entity.getUserLabel());
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
    public Stream<String> getStoredUsersStream(RealmModel realm, Integer first, Integer max) {
        TypedQuery<String> query = em.createNamedQuery("getFederatedUserIds", String.class)
                .setParameter("realmId", realm.getId());
        return closing(paginateQuery(query, first, max).getResultStream());
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        updateCredential(realm, user.getId(), cred);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        return createCredential(realm, user.getId(), cred);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        return removeStoredCredential(realm, user.getId(), id);
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return getStoredCredentialById(realm, user.getId(), id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, UserModel user) {
        return getStoredCredentialsStream(realm, user.getId());
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, UserModel user, String type) {
        return getStoredCredentialsByTypeStream(realm, user.getId(), type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return getStoredCredentialByNameAndType(realm, user.getId(), name, type);
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId) {
        // 1 - Create new list and move everything to it.
        List<FederatedUserCredentialEntity> newList = this.getStoredCredentialEntitiesStream(user.getId()).collect(Collectors.toList());

        // 2 - Find indexes of our and newPrevious credential
        int ourCredentialIndex = -1;
        int newPreviousCredentialIndex = -1;
        FederatedUserCredentialEntity ourCredential = null;
        int i = 0;
        for (FederatedUserCredentialEntity credential : newList) {
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
        for (FederatedUserCredentialEntity credential : newList) {
            expectedPriority += JpaUserCredentialStore.PRIORITY_DIFFERENCE;
            if (credential.getPriority() != expectedPriority) {
                credential.setPriority(expectedPriority);

                logger.tracef("Priority of credential [%s] of user [%s] changed to [%d]", credential.getId(), user.getUsername(), expectedPriority);
            }
        }
        return true;
    }

    @Override
    public int getStoredUsersCount(RealmModel realm) {
        Object count = em.createNamedQuery("getFederatedUserCount")
                .setParameter("realmId", realm.getId())
                .getSingleResult();
        return ((Number)count).intValue();
    }

    @Override
    public void preRemove(RealmModel realm) {
        int num = em.createNamedQuery("deleteFederatedUserConsentClientScopesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserConsentsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserRoleMappingsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserRequiredActionsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteBrokerLinkByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserCredentialsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserFederatedAttributesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserGroupMembershipByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUsersByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        em.createNamedQuery("deleteFederatedUserRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        em.createNamedQuery("deleteFederatedUserGroupMembershipsByGroup").setParameter("groupId", group.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        StorageId clientStorageId = new StorageId(client.getId());
        if (clientStorageId.isLocal()) {
            em.createNamedQuery("deleteFederatedUserConsentClientScopesByClient").setParameter("clientId", client.getId()).executeUpdate();
            em.createNamedQuery("deleteFederatedUserConsentsByClient").setParameter("clientId", client.getId()).executeUpdate();
        } else {
            em.createNamedQuery("deleteFederatedUserConsentClientScopesByExternalClient")
                    .setParameter("clientStorageProvider", clientStorageId.getProviderId())
                    .setParameter("externalClientId",clientStorageId.getExternalId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserConsentsByExternalClient")
                    .setParameter("clientStorageProvider", clientStorageId.getProviderId())
                    .setParameter("externalClientId",clientStorageId.getExternalId())
                    .executeUpdate();

        }
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        // No op
    }

    @Override
    public void preRemove(ClientScopeModel clientScope) {
        em.createNamedQuery("deleteFederatedUserConsentClientScopesByClientScope")
                .setParameter("scopeId", clientScope.getId())
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, UserModel user) {
        em.createNamedQuery("deleteBrokerLinkByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteUserFederatedAttributesByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentClientScopesByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserCredentialByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserGroupMembershipsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRequiredActionsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRoleMappingsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();

    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel model) {
        if (model.getProviderType().equals(UserStorageProvider.class.getName())) {

            em.createNamedQuery("deleteBrokerLinkByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedAttributesByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserConsentClientScopesByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserConsentsByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserCredentialsByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserGroupMembershipByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserRequiredActionsByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserRoleMappingsByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUsersByStorageProvider")
                    .setParameter("storageProviderId", model.getId())
                    .executeUpdate();
        } else if (model.getProviderType().equals(ClientStorageProvider.class.getName())) {
            em.createNamedQuery("deleteFederatedUserConsentClientScopesByClientStorageProvider")
                    .setParameter("clientStorageProvider",  model.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteFederatedUserConsentsByClientStorageProvider")
                    .setParameter("clientStorageProvider",  model.getId())
                    .executeUpdate();

        }

    }
}
