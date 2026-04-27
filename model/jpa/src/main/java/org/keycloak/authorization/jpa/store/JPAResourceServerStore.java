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
package org.keycloak.authorization.jpa.store;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.PermissionTicketEntity;
import org.keycloak.authorization.jpa.entities.PolicyEntity;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.storage.StorageId;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAResourceServerStore implements ResourceServerStore {

    private final EntityManager entityManager;
    private final AuthorizationProvider provider;

    public JPAResourceServerStore(EntityManager entityManager, AuthorizationProvider provider) {
        this.entityManager = entityManager;
        this.provider = provider;
    }

    @Override
    public ResourceServer create(ClientModel client) {
        String clientId = client.getId();
        if (!StorageId.isLocalStorage(clientId)) {
            throw new ModelException("Creating resource server from federated ClientModel not supported");
        }
        ResourceServerEntity entity = new ResourceServerEntity();

        entity.setId(clientId);

        this.entityManager.persist(entity);

        return new ResourceServerAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public void delete(ClientModel client) {
        String id = client.getId();
        ResourceServerEntity entity = entityManager.find(ResourceServerEntity.class, id);
        if (entity == null) return;

        // reordering deletions to avoid FK issues, but it is not a fix, but only a workaround
        // proper fix would be to refactor the whole mess with bidirectional relationships with proper cascade-on-delete behavior.
        // this method would become just a one-liner entityManager.remove(entity);
        {
            TypedQuery<String> query = entityManager.createNamedQuery("findPermissionTicketIdByServerId", String.class);

            query.setParameter("serverId", id);

            List<String> result = query.getResultList();
            for (String permissionId : result) {
                entityManager.remove(entityManager.getReference(PermissionTicketEntity.class, permissionId));
            }
        }

        //This didn't work, had to loop through and remove each policy individually
        //entityManager.createNamedQuery("deletePolicyByResourceServer")
        //        .setParameter("serverId", id).executeUpdate();

        {
            TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByServerId", String.class);
            query.setParameter("serverId", id);
            List<String> result = query.getResultList();
            for (String policyId : result) {
                PolicyEntity policyEntity = entityManager.find(PolicyEntity.class, policyId);
                policyEntity.getAssociatedPolicies().clear();
                entityManager.remove(policyEntity);
            }
        }

        //entityManager.createNamedQuery("deleteResourceByResourceServer")
        //        .setParameter("serverId", id).executeUpdate();
        {
            TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByServerId", String.class);

            query.setParameter("serverId", id);

            List<String> result = query.getResultList();
            for (String resourceId : result) {
                entityManager.remove(entityManager.getReference(ResourceEntity.class, resourceId));
            }
        }

        //entityManager.createNamedQuery("deleteScopeByResourceServer")
        //        .setParameter("serverId", id).executeUpdate();
        {
            TypedQuery<String> query = entityManager.createNamedQuery("findScopeIdByResourceServer", String.class);

            query.setParameter("serverId", id);

            List<String> result = query.getResultList();
            for (String scopeId : result) {
                entityManager.remove(entityManager.getReference(ScopeEntity.class, scopeId));
            }
        }

        this.entityManager.remove(entity);
        entityManager.flush();
        entityManager.detach(entity);
    }

    @Override
    public ResourceServer findById(String id) {
        ResourceServerEntity entity = entityManager.find(ResourceServerEntity.class, id);
        if (entity == null) return null;
        return new ResourceServerAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public ResourceServer findByClient(ClientModel client) {
        return findById(client.getId());
    }
}
