/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.jpa.store;

import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAResourceServerStore implements ResourceServerStore {

    private final EntityManager entityManager;

    public JPAResourceServerStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ResourceServer create(String clientId) {
        ResourceServerEntity entity = new ResourceServerEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setClientId(clientId);

        this.entityManager.persist(entity);

        return entity;
    }

    @Override
    public void delete(String id) {
        this.entityManager.remove(findById(id));
    }

    @Override
    public ResourceServer findById(String id) {
        return entityManager.find(ResourceServerEntity.class, id);
    }

    @Override
    public ResourceServer findByClient(final String clientId) {
        Query query = entityManager.createQuery("from ResourceServerEntity where clientId = :clientId");

        query.setParameter("clientId", clientId);
        List result = query.getResultList();

        if (result.isEmpty()) {
            return null;
        }

        return (ResourceServer) result.get(0);
    }
}
