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
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ClientInitialAccessEntity;
import org.keycloak.models.jpa.entities.ClientTemplateEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProvider implements RealmProvider {
    protected static final Logger logger = Logger.getLogger(JpaRealmProvider.class);
    private final KeycloakSession session;
    protected EntityManager em;

    public JpaRealmProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public MigrationModel getMigrationModel() {
        return new MigrationModelAdapter(em);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmEntity realm = new RealmEntity();
        realm.setName(name);
        realm.setId(id);
        em.persist(realm);
        em.flush();
        final RealmModel adapter = new RealmAdapter(session, em, realm);
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmCreationEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return adapter;
            }
        });
        return adapter;
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        RealmAdapter adapter = new RealmAdapter(session, em, realm);
        return adapter;
    }

    @Override
    public List<RealmModel> getRealms() {
        TypedQuery<String> query = em.createNamedQuery("getAllRealmIds", String.class);
        List<String> entities = query.getResultList();
        List<RealmModel> realms = new ArrayList<RealmModel>();
        for (String id : entities) {
            RealmModel realm = session.realms().getRealm(id);
            if (realm != null) realms.add(realm);

        }
        return realms;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmIdByName", String.class);
        query.setParameter("name", name);
        List<String> entities = query.getResultList();
        if (entities.size() == 0) return null;
        if (entities.size() > 1) throw new IllegalStateException("Should not be more than one realm with same name");
        String id = query.getResultList().get(0);

        return session.realms().getRealm(id);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) {
            return false;
        }
        em.refresh(realm);
        final RealmAdapter adapter = new RealmAdapter(session, em, realm);
        session.users().preRemove(adapter);

        realm.getDefaultGroups().clear();
        em.flush();

        int num = em.createNamedQuery("deleteGroupRoleMappingsByRealm")
                .setParameter("realm", realm).executeUpdate();

        TypedQuery<String> query = em.createNamedQuery("getClientIdsByRealm", String.class);
        query.setParameter("realm", realm.getId());
        List<String> clients = query.getResultList();
        for (String client : clients) {
            // No need to go through cache. Clients were already invalidated
            removeClient(client, adapter);
        }

        for (ClientTemplateEntity a : new LinkedList<>(realm.getClientTemplates())) {
            adapter.removeClientTemplate(a.getId());
        }

        for (RoleModel role : adapter.getRoles()) {
            // No need to go through cache. Roles were already invalidated
            removeRole(adapter, role);
        }

        num = em.createNamedQuery("removeClientInitialAccessByRealm")
                .setParameter("realm", realm).executeUpdate();

        em.remove(realm);

        em.flush();
        em.clear();

        session.getKeycloakSessionFactory().publish(new RealmModel.RealmRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return adapter;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
        return addRealmRole(realm, KeycloakModelUtils.generateId(), name);

    }
    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        RoleEntity entity = new RoleEntity();
        entity.setId(id);
        entity.setName(name);
        RealmEntity ref = em.getReference(RealmEntity.class, realm.getId());
        entity.setRealm(ref);
        entity.setRealmId(realm.getId());
        em.persist(entity);
        em.flush();
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;

    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm.getId());
        List<String> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return session.realms().getRoleById(roles.get(0), realm);
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        return addClientRole(realm, client, KeycloakModelUtils.generateId(), name);
    }
    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        ClientEntity clientEntity = em.getReference(ClientEntity.class, client.getId());
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setClient(clientEntity);
        roleEntity.setClientRole(true);
        roleEntity.setRealmId(realm.getId());
        em.persist(roleEntity);
        RoleAdapter adapter = new RoleAdapter(session, realm, em, roleEntity);
        return adapter;
    }

    @Override
    public Set<RoleModel> getRealmRoles(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIds", String.class);
        query.setParameter("realm", realm.getId());
        List<String> roles = query.getResultList();

        if (roles.isEmpty()) return Collections.EMPTY_SET;
        Set<RoleModel> list = new HashSet<RoleModel>();
        for (String id : roles) {
            list.add(session.realms().getRoleById(id, realm));
        }
        return Collections.unmodifiableSet(list);
    }

    @Override
    public RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        TypedQuery<String> query = em.createNamedQuery("getClientRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("client", client.getId());
        List<String> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return session.realms().getRoleById(roles.get(0), realm);
    }


    @Override
    public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        Set<RoleModel> list = new HashSet<RoleModel>();
        TypedQuery<String> query = em.createNamedQuery("getClientRoleIds", String.class);
        query.setParameter("client", client.getId());
        List<String> roles = query.getResultList();
        for (String id : roles) {
            list.add(session.realms().getRoleById(id, realm));
        }
        return list;

    }

    @Override
    public boolean removeRole(RealmModel realm, RoleModel role) {
        session.users().preRemove(realm, role);
        RoleContainerModel container = role.getContainer();
        if (container.getDefaultRoles().contains(role.getName())) {
            container.removeDefaultRoles(role.getName());
        }
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        String compositeRoleTable = JpaUtils.getTableNameForNativeQuery("COMPOSITE_ROLE", em);
        em.createNativeQuery("delete from " + compositeRoleTable + " where CHILD_ROLE = :role").setParameter("role", roleEntity).executeUpdate();
        realm.getClients().forEach(c -> c.deleteScopeMapping(role));
        em.createNamedQuery("deleteTemplateScopeMappingByRole").setParameter("role", roleEntity).executeUpdate();
        int val = em.createNamedQuery("deleteGroupRoleMappingsByRole").setParameter("roleId", roleEntity.getId()).executeUpdate();

        em.flush();
        em.remove(roleEntity);

        session.getKeycloakSessionFactory().publish(new RoleContainerModel.RoleRemovedEvent() {
            @Override
            public RoleModel getRole() {
                return role;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        em.flush();
        return true;

    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.getId().equals(entity.getRealmId())) return null;
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;
    }

    @Override
    public GroupModel getGroupById(String id, RealmModel realm) {
        GroupEntity groupEntity = em.find(GroupEntity.class, id);
        if (groupEntity == null) return null;
        if (!groupEntity.getRealm().getId().equals(realm.getId())) return null;
        GroupAdapter adapter =  new GroupAdapter(realm, em, groupEntity);
        return adapter;
    }

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }
        if (group.getParentId() != null) {
            group.getParent().removeChild(group);
        }
        group.setParent(toParent);
        if (toParent != null) toParent.addChild(group);
        else session.realms().addTopLevelGroup(realm, group);
    }

    @Override
    public List<GroupModel> getGroups(RealmModel realm) {
        RealmEntity ref = em.getReference(RealmEntity.class, realm.getId());

        return ref.getGroups().stream()
                .map(g -> session.realms().getGroupById(g.getId(), realm))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<GroupModel> getTopLevelGroups(RealmModel realm) {
        RealmEntity ref = em.getReference(RealmEntity.class, realm.getId());

        return ref.getGroups().stream()
                .filter(g -> g.getParent() == null)
                .map(g -> session.realms().getGroupById(g.getId(), realm))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        if (group == null) {
            return false;
        }

        GroupModel.GroupRemovedEvent event = new GroupModel.GroupRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public GroupModel getGroup() {
                return group;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
        session.getKeycloakSessionFactory().publish(event);

        session.users().preRemove(realm, group);

        realm.removeDefaultGroup(group);
        for (GroupModel subGroup : group.getSubGroups()) {
            session.realms().removeGroup(realm, subGroup);
        }
        moveGroup(realm, group, null);
        GroupEntity groupEntity = em.find(GroupEntity.class, group.getId());
        if (!groupEntity.getRealm().getId().equals(realm.getId())) {
            return false;
        }
        em.createNamedQuery("deleteGroupRoleMappingsByGroup").setParameter("group", groupEntity).executeUpdate();

        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        realmEntity.getGroups().remove(groupEntity);

        em.remove(groupEntity);
        return true;


    }

    @Override
    public GroupModel createGroup(RealmModel realm, String name) {
        String id = KeycloakModelUtils.generateId();
        return createGroup(realm, id, name);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name) {
        if (id == null) id = KeycloakModelUtils.generateId();
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setName(name);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        groupEntity.setRealm(realmEntity);
        em.persist(groupEntity);
        realmEntity.getGroups().add(groupEntity);

        GroupAdapter adapter = new GroupAdapter(realm, em, groupEntity);
        return adapter;
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        subGroup.setParent(null);
    }



    @Override
    public ClientModel addClient(RealmModel realm, String clientId) {
        return addClient(realm, KeycloakModelUtils.generateId(), clientId);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        if (clientId == null) {
            clientId = id;
        }
        ClientEntity entity = new ClientEntity();
        entity.setId(id);
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);
        RealmEntity realmRef = em.getReference(RealmEntity.class, realm.getId());
        entity.setRealm(realmRef);
        em.persist(entity);
        em.flush();
        final ClientModel resource = new ClientAdapter(realm, em, session, entity);

        em.flush();
        session.getKeycloakSessionFactory().publish(new RealmModel.ClientCreationEvent() {
            @Override
            public ClientModel getCreatedClient() {
                return resource;
            }
        });
        return resource;
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getClientIdsByRealm", String.class);
        query.setParameter("realm", realm.getId());
        List<String> clients = query.getResultList();
        if (clients.isEmpty()) return Collections.EMPTY_LIST;
        List<ClientModel> list = new LinkedList<>();
        for (String id : clients) {
            ClientModel client = session.realms().getClientById(id, realm);
            if (client != null) list.add(client);
        }
        return Collections.unmodifiableList(list);

    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        ClientEntity app = em.find(ClientEntity.class, id);
        // Check if application belongs to this realm
        if (app == null || !realm.getId().equals(app.getRealm().getId())) return null;
        ClientAdapter client = new ClientAdapter(realm, em, session, app);
        return client;

    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("findClientIdByClientId", String.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realm", realm.getId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) return null;
        String id = results.get(0);
        return session.realms().getClientById(id, realm);
    }

    @Override
    public boolean removeClient(String id, RealmModel realm) {
        final ClientModel client = getClientById(id, realm);
        if (client == null) return false;

        session.users().preRemove(realm, client);

        for (RoleModel role : client.getRoles()) {
            // No need to go through cache. Roles were already invalidated
            removeRole(realm, role);
        }

        ClientEntity clientEntity = ((ClientAdapter)client).getEntity();

        session.getKeycloakSessionFactory().publish(new RealmModel.ClientRemovedEvent() {
            @Override
            public ClientModel getClient() {
                return client;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        em.remove(clientEntity);  // i have no idea why, but this needs to come before deleteScopeMapping

        try {
            em.flush();
        } catch (RuntimeException e) {
            logger.errorv("Unable to delete client entity: {0} from realm {1}", client.getClientId(), realm.getName());
            throw e;
        }

        return true;
    }

    @Override
    public ClientTemplateModel getClientTemplateById(String id, RealmModel realm) {
        ClientTemplateEntity app = em.find(ClientTemplateEntity.class, id);

        // Check if application belongs to this realm
        if (app == null || !realm.getId().equals(app.getRealm().getId())) return null;
        ClientTemplateAdapter adapter = new ClientTemplateAdapter(realm, em, session, app);
        return adapter;
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count) {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        ClientInitialAccessEntity entity = new ClientInitialAccessEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealm(realmEntity);

        entity.setCount(count);
        entity.setRemainingCount(count);

        int currentTime = Time.currentTime();
        entity.setTimestamp(currentTime);
        entity.setExpiration(expiration);

        em.persist(entity);

        return entityToModel(entity);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id);
        if (entity == null) {
            return null;
        } else {
            return entityToModel(entity);
        }
    }

    @Override
    public void removeClientInitialAccessModel(RealmModel realm, String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id);
        if (entity != null) {
            em.remove(entity);
            em.flush();
        }
    }

    @Override
    public List<ClientInitialAccessModel> listClientInitialAccess(RealmModel realm) {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        TypedQuery<ClientInitialAccessEntity> query = em.createNamedQuery("findClientInitialAccessByRealm", ClientInitialAccessEntity.class);
        query.setParameter("realm", realmEntity);
        List<ClientInitialAccessEntity> entities = query.getResultList();

        return entities.stream()
                .map(entity -> entityToModel(entity))
                .collect(Collectors.toList());
    }

    @Override
    public void removeExpiredClientInitialAccess() {
        int currentTime = Time.currentTime();

        em.createNamedQuery("removeExpiredClientInitialAccess")
                .setParameter("currentTime", currentTime)
                .executeUpdate();
    }

    @Override
    public void decreaseRemainingCount(RealmModel realm, ClientInitialAccessModel clientInitialAccess) {
        em.createNamedQuery("decreaseClientInitialAccessRemainingCount")
                .setParameter("id", clientInitialAccess.getId())
                .executeUpdate();
    }

    private ClientInitialAccessModel entityToModel(ClientInitialAccessEntity entity) {
        ClientInitialAccessModel model = new ClientInitialAccessModel();
        model.setId(entity.getId());
        model.setCount(entity.getCount());
        model.setRemainingCount(entity.getRemainingCount());
        model.setExpiration(entity.getExpiration());
        model.setTimestamp(entity.getTimestamp());
        return model;
    }
}
