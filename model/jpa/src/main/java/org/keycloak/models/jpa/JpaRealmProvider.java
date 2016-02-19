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
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ClientTemplateEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
        final RealmModel model = new RealmAdapter(session, em, realm);
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmCreationEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return model;
            }
        });
        return model;
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        return new RealmAdapter(session, em, realm);
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
        RealmAdapter adapter = new RealmAdapter(session, em, realm);
        session.users().preRemove(adapter);
        int num = em.createNamedQuery("deleteGroupRoleMappingsByRealm")
                .setParameter("realm", realm).executeUpdate();
        num = em.createNamedQuery("deleteGroupAttributesByRealm")
                .setParameter("realm", realm).executeUpdate();
        num = em.createNamedQuery("deleteGroupsByRealm")
                .setParameter("realm", realm).executeUpdate();

        TypedQuery<String> query = em.createNamedQuery("getClientIdsByRealm", String.class);
        query.setParameter("realm", realm.getId());
        List<String> clients = query.getResultList();
        for (String client : clients) {
            session.realms().removeClient(client, adapter);
        }
        /*
        for (ClientEntity a : new LinkedList<>(realm.getClients())) {
            adapter.removeClient(a.getId());
        }
        */
        for (ClientTemplateEntity a : new LinkedList<>(realm.getClientTemplates())) {
            adapter.removeClientTemplate(a.getId());
        }

        em.remove(realm);

        em.flush();
        em.clear();
        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.getId().equals(entity.getRealmId())) return null;
        return new RoleAdapter(session, realm, em, entity);
    }

    @Override
    public GroupModel getGroupById(String id, RealmModel realm) {
        GroupEntity groupEntity = em.find(GroupEntity.class, id);
        if (groupEntity == null) return null;
        if (!groupEntity.getRealm().getId().equals(realm.getId())) return null;
        return new GroupAdapter(realm, em, groupEntity);
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
        return new ClientAdapter(realm, em, session, app);
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
        ClientModel client = getClientById(id, realm);
        if (client == null) return false;

        session.users().preRemove(realm, client);

        for (RoleModel role : client.getRoles()) {
            client.removeRole(role);
        }


        ClientEntity clientEntity = ((ClientAdapter)client).getEntity();
        em.createNamedQuery("deleteScopeMappingByClient").setParameter("client", clientEntity).executeUpdate();
        em.flush();
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
        return new ClientTemplateAdapter(realm, em, session, app);
    }
}
