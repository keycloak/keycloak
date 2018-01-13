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
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.jpa.entity.BrokerLinkEntity;
import org.keycloak.storage.jpa.entity.FederatedUser;
import org.keycloak.storage.jpa.entity.FederatedUserAttributeEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentProtocolMapperEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentRoleEntity;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialAttributeEntity;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;
import org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity;
import org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity;
import org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserFederatedStorageProvider implements
        UserFederatedStorageProvider,
        UserCredentialStore {

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
    public List<String> getUsersByUserAttribute(RealmModel realm, String name, String value) {
        TypedQuery<String> query = em.createNamedQuery("getFederatedAttributesByNameAndValue", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("name", name)
                .setParameter("value", value);
        return query.getResultList();
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
    public Set<FederatedIdentityModel> getFederatedIdentities(String userId, RealmModel realm) {
        TypedQuery<BrokerLinkEntity> query = em.createNamedQuery("findBrokerLinkByUser", BrokerLinkEntity.class)
                .setParameter("userId", userId);
        List<BrokerLinkEntity> results = query.getResultList();
        Set<FederatedIdentityModel> set = new HashSet<>();
        for (BrokerLinkEntity entity : results) {
            FederatedIdentityModel model = new FederatedIdentityModel(entity.getIdentityProvider(), entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken());
            set.add(model);
        }
        return set;
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

        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId);
        if (consentEntity != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + userId + "]");
        }

        consentEntity = new FederatedUserConsentEntity();
        consentEntity.setId(KeycloakModelUtils.generateId());
        consentEntity.setUserId(userId);
        consentEntity.setClientId(clientId);
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
        FederatedUserConsentEntity entity = getGrantedConsentEntity(userId, clientInternalId);
        return toConsentModel(realm, entity);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, String userId) {
        TypedQuery<FederatedUserConsentEntity> query = em.createNamedQuery("userFederatedConsentsByUser", FederatedUserConsentEntity.class);
        query.setParameter("userId", userId);
        List<FederatedUserConsentEntity> results = query.getResultList();

        List<UserConsentModel> consents = new ArrayList<UserConsentModel>();
        for (FederatedUserConsentEntity entity : results) {
            UserConsentModel model = toConsentModel(realm, entity);
            consents.add(model);
        }
        return consents;
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        createIndex(realm, userId);
        String clientId = consent.getClient().getId();

        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + userId + "]");
        }

        updateGrantedConsentEntity(consentEntity, consent);

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientInternalId);
        if (consentEntity == null) return false;

        em.remove(consentEntity);
        em.flush();
        return true;
    }

    private FederatedUserConsentEntity getGrantedConsentEntity(String userId, String clientId) {
        TypedQuery<FederatedUserConsentEntity> query = em.createNamedQuery("userFederatedConsentByUserAndClient", FederatedUserConsentEntity.class);
        query.setParameter("userId", userId);
        query.setParameter("clientId", clientId);
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

        ClientModel client = realm.getClientById(entity.getClientId());
        if (client == null) {
            throw new ModelException("Client with id " + entity.getClientId() + " is not available");
        }
        UserConsentModel model = new UserConsentModel(client);
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());

        Collection<FederatedUserConsentRoleEntity> grantedRoleEntities = entity.getGrantedRoles();
        if (grantedRoleEntities != null) {
            for (FederatedUserConsentRoleEntity grantedRole : grantedRoleEntities) {
                RoleModel grantedRoleModel = realm.getRoleById(grantedRole.getRoleId());
                if (grantedRoleModel != null) {
                    model.addGrantedRole(grantedRoleModel);
                }
            }
        }

        Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMapperEntities = entity.getGrantedProtocolMappers();
        if (grantedProtocolMapperEntities != null) {
            for (FederatedUserConsentProtocolMapperEntity grantedProtMapper : grantedProtocolMapperEntities) {
                ProtocolMapperModel protocolMapper = client.getProtocolMapperById(grantedProtMapper.getProtocolMapperId());
                model.addGrantedProtocolMapper(protocolMapper);
            }
        }

        return model;
    }

    // Update roles and protocolMappers to given consentEntity from the consentModel
    private void updateGrantedConsentEntity(FederatedUserConsentEntity consentEntity, UserConsentModel consentModel) {
        Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMapperEntities = consentEntity.getGrantedProtocolMappers();
        Collection<FederatedUserConsentProtocolMapperEntity> mappersToRemove = new HashSet<>(grantedProtocolMapperEntities);

        for (ProtocolMapperModel protocolMapper : consentModel.getGrantedProtocolMappers()) {
            FederatedUserConsentProtocolMapperEntity grantedProtocolMapperEntity = new FederatedUserConsentProtocolMapperEntity();
            grantedProtocolMapperEntity.setUserConsent(consentEntity);
            grantedProtocolMapperEntity.setProtocolMapperId(protocolMapper.getId());

            // Check if it's already there
            if (!grantedProtocolMapperEntities.contains(grantedProtocolMapperEntity)) {
                em.persist(grantedProtocolMapperEntity);
                em.flush();
                grantedProtocolMapperEntities.add(grantedProtocolMapperEntity);
            } else {
                mappersToRemove.remove(grantedProtocolMapperEntity);
            }
        }
        // Those mappers were no longer on consentModel and will be removed
        for (FederatedUserConsentProtocolMapperEntity toRemove : mappersToRemove) {
            grantedProtocolMapperEntities.remove(toRemove);
            em.remove(toRemove);
        }

        Collection<FederatedUserConsentRoleEntity> grantedRoleEntities = consentEntity.getGrantedRoles();
        Set<FederatedUserConsentRoleEntity> rolesToRemove = new HashSet<>(grantedRoleEntities);
        for (RoleModel role : consentModel.getGrantedRoles()) {
            FederatedUserConsentRoleEntity consentRoleEntity = new FederatedUserConsentRoleEntity();
            consentRoleEntity.setUserConsent(consentEntity);
            consentRoleEntity.setRoleId(role.getId());

            // Check if it's already there
            if (!grantedRoleEntities.contains(consentRoleEntity)) {
                em.persist(consentRoleEntity);
                em.flush();
                grantedRoleEntities.add(consentRoleEntity);
            } else {
                rolesToRemove.remove(consentRoleEntity);
            }
        }
        // Those roles were no longer on consentModel and will be removed
        for (FederatedUserConsentRoleEntity toRemove : rolesToRemove) {
            grantedRoleEntities.remove(toRemove);
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
    public Set<GroupModel> getGroups(RealmModel realm, String userId) {
        Set<GroupModel> set = new HashSet<>();
        TypedQuery<FederatedUserGroupMembershipEntity> query = em.createNamedQuery("feduserGroupMembership", FederatedUserGroupMembershipEntity.class);
        query.setParameter("userId", userId);
        List<FederatedUserGroupMembershipEntity> results = query.getResultList();
        if (results.size() == 0) return set;
        for (FederatedUserGroupMembershipEntity entity : results) {
            GroupModel group = realm.getGroupById(entity.getGroupId());
            set.add(group);
        }
        return set;
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
        List<FederatedUserGroupMembershipEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (FederatedUserGroupMembershipEntity entity : results) {
            em.remove(entity);
        }
        em.flush();

    }

    @Override
    public List<String> getMembership(RealmModel realm, GroupModel group, int firstResult, int max) {
        TypedQuery<String> query = em.createNamedQuery("fedgroupMembership", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("groupId", group.getId());
        query.setFirstResult(firstResult);
        query.setMaxResults(max);
        return query.getResultList();
    }

    @Override
    public Set<String> getRequiredActions(RealmModel realm, String userId) {
        Set<String> set = new HashSet<>();
        List<FederatedUserRequiredActionEntity> values = getRequiredActionEntities(realm, userId);
        for (FederatedUserRequiredActionEntity entity : values) {
            set.add(entity.getAction());
        }

        return set;

    }

    private List<FederatedUserRequiredActionEntity> getRequiredActionEntities(RealmModel realm, String userId) {
        TypedQuery<FederatedUserRequiredActionEntity> query = em.createNamedQuery("getFederatedUserRequiredActionsByUser", FederatedUserRequiredActionEntity.class)
                .setParameter("userId", userId)
                .setParameter("realmId", realm.getId());
        return query.getResultList();
    }

    @Override
    public void addRequiredAction(RealmModel realm, String userId, String action) {
        createIndex(realm, userId);
        FederatedUserRequiredActionEntity entity = new FederatedUserRequiredActionEntity();
        entity.setUserId(userId);
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        entity.setAction(action);
        em.persist(entity);

    }

    @Override
    public void removeRequiredAction(RealmModel realm, String userId, String action) {
        List<FederatedUserRequiredActionEntity> values = getRequiredActionEntities(realm, userId);
        for (FederatedUserRequiredActionEntity entity : values) {
            if (action.equals(entity.getAction())) em.remove(entity);
        }
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
    public Set<RoleModel> getRoleMappings(RealmModel realm, String userId) {
        Set<RoleModel> set = new HashSet<>();
        TypedQuery<FederatedUserRoleMappingEntity> query = em.createNamedQuery("feduserRoleMappings", FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", userId);
        List<FederatedUserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return set;
        for (FederatedUserRoleMappingEntity entity : results) {
            RoleModel role = realm.getRoleById(entity.getRoleId());
            set.add(role);
        }
        return set;
    }

    @Override
    public void deleteRoleMapping(RealmModel realm, String userId, RoleModel role) {
        TypedQuery<FederatedUserRoleMappingEntity> query = em.createNamedQuery("feduserRoleMappings", FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", userId);
        List<FederatedUserRoleMappingEntity> results = query.getResultList();
        for (FederatedUserRoleMappingEntity entity : results) {
            if (entity.getRoleId().equals(role.getId())) em.remove(entity);

        }
        em.flush();
    }

    @Override
    public void updateCredential(RealmModel realm, String userId, CredentialModel cred) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, cred.getId());
        if (entity == null) return;
        createIndex(realm, userId);
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
            MultivaluedHashMap<String, String> attrs = new MultivaluedHashMap<>();
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
    public CredentialModel createCredential(RealmModel realm, String userId, CredentialModel cred) {
        createIndex(realm, userId);
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
        entity.setUserId(userId);
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        em.persist(entity);
        MultivaluedHashMap<String, String> config = cred.getConfig();
        if (config != null && !config.isEmpty()) {

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
    public boolean removeStoredCredential(RealmModel realm, String userId, String id) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, id);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, String userId, String id) {
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
        model.setHashIterations(entity.getHashIterations());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        model.setConfig(config);
        for (FederatedUserCredentialAttributeEntity attr : entity.getCredentialAttributes()) {
            config.add(attr.getName(), attr.getValue());
        }
        return model;
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, String userId) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByUser", FederatedUserCredentialEntity.class)
                .setParameter("userId", userId);
        List<FederatedUserCredentialEntity> results = query.getResultList();
        List<CredentialModel> rtn = new LinkedList<>();
        for (FederatedUserCredentialEntity entity : results) {
            rtn.add(toModel(entity));
        }
        return rtn;
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, String userId, String type) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByUserAndType", FederatedUserCredentialEntity.class)
                .setParameter("type", type)
                .setParameter("userId", userId);
        List<FederatedUserCredentialEntity> results = query.getResultList();
        List<CredentialModel> rtn = new LinkedList<>();
        for (FederatedUserCredentialEntity entity : results) {
            rtn.add(toModel(entity));
        }
        return rtn;
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, String userId, String name, String type) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByNameAndType", FederatedUserCredentialEntity.class)
                .setParameter("type", type)
                .setParameter("device", name)
                .setParameter("userId", userId);
        List<FederatedUserCredentialEntity> results = query.getResultList();
        if (results.isEmpty()) return null;
        return toModel(results.get(0));
    }

    @Override
    public List<String> getStoredUsers(RealmModel realm, int first, int max) {
        TypedQuery<String> query = em.createNamedQuery("getFederatedUserIds", String.class)
                .setParameter("realmId", realm.getId())
                .setFirstResult(first);
        if (max > 0) query.setMaxResults(max);
        return query.getResultList();
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
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        return getStoredCredentials(realm, user.getId());
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        return getStoredCredentialsByType(realm, user.getId(), type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return getStoredCredentialByNameAndType(realm, user.getId(), name, type);
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
        int num = em.createNamedQuery("deleteFederatedUserConsentRolesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserConsentProtMappersByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserConsentsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserRoleMappingsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserRequiredActionsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteBrokerLinkByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedCredentialAttributeByRealm")
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
        em.createNamedQuery("deleteFederatedUserRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        em.createNamedQuery("deleteFederatedUserGroupMembershipsByGroup").setParameter("groupId", group.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentRolesByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentsByClient").setParameter("clientId", client.getId()).executeUpdate();
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByProtocolMapper")
                .setParameter("protocolMapperId", protocolMapper.getId())
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
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentRolesByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedCredentialAttributeByUser")
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
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) return;

        em.createNamedQuery("deleteBrokerLinkByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedAttributesByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRoleMappingsByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentsByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedCredentialAttributeByStorageProvider")
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

    }
}
