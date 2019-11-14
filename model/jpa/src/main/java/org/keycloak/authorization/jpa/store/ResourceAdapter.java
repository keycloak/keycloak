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
package org.keycloak.authorization.jpa.store;

import org.keycloak.authorization.jpa.entities.ResourceAttributeEntity;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.AbstractAuthorizationModel;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdapter extends AbstractAuthorizationModel implements Resource, JpaModel<ResourceEntity> {

    private ResourceEntity entity;
    private EntityManager em;
    private StoreFactory storeFactory;

    public ResourceAdapter(ResourceEntity entity, EntityManager em, StoreFactory storeFactory) {
        super(storeFactory);
        this.entity = entity;
        this.em = em;
        this.storeFactory = storeFactory;
    }

    @Override
    public ResourceEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public String getDisplayName() {
        return entity.getDisplayName();
    }

    @Override
    public void setDisplayName(String name) {
        throwExceptionIfReadonly();
        entity.setDisplayName(name);
    }

    @Override
    public Set<String> getUris() {
        return entity.getUris();
    }

    @Override
    public void updateUris(Set<String> uri) {
        throwExceptionIfReadonly();
        entity.setUris(uri);
    }

    @Override
    public void setName(String name) {
        throwExceptionIfReadonly();
        entity.setName(name);

    }

    @Override
    public String getType() {
        return entity.getType();
    }

    @Override
    public void setType(String type) {
        throwExceptionIfReadonly();
        entity.setType(type);

    }

    @Override
    public List<Scope> getScopes() {
        List<Scope> scopes = new LinkedList<>();
        for (ScopeEntity scope : entity.getScopes()) {
            scopes.add(storeFactory.getScopeStore().findById(scope.getId(), entity.getResourceServer().getId()));
        }

        return Collections.unmodifiableList(scopes);
    }

    @Override
    public String getIconUri() {
        return entity.getIconUri();
    }

    @Override
    public void setIconUri(String iconUri) {
        throwExceptionIfReadonly();
        entity.setIconUri(iconUri);

    }

    @Override
    public ResourceServer getResourceServer() {
        ResourceServer temp = storeFactory.getResourceServerStore().findById(entity.getResourceServer().getId());
        return temp;
    }

    @Override
    public String getOwner() {
        return entity.getOwner();
    }

    @Override
    public boolean isOwnerManagedAccess() {
        return entity.isOwnerManagedAccess();
    }

    @Override
    public void setOwnerManagedAccess(boolean ownerManagedAccess) {
        throwExceptionIfReadonly();
        entity.setOwnerManagedAccess(ownerManagedAccess);
    }

    @Override
    public void updateScopes(Set<Scope> toUpdate) {
        throwExceptionIfReadonly();
        Set<String> ids = new HashSet<>();
        for (Scope scope : toUpdate) {
            ids.add(scope.getId());
        }
        Iterator<ScopeEntity> it = entity.getScopes().iterator();
        while (it.hasNext()) {
            ScopeEntity next = it.next();
            if (!ids.contains(next.getId())) it.remove();
            else ids.remove(next.getId());
        }
        for (String addId : ids) {
            entity.getScopes().add(em.getReference(ScopeEntity.class, addId));
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>();
        for (ResourceAttributeEntity attr : entity.getAttributes()) {
            result.add(attr.getName(), attr.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public String getSingleAttribute(String name) {
        List<String> values = getAttributes().getOrDefault(name, Collections.emptyList());

        if (values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    @Override
    public List<String> getAttribute(String name) {
        List<String> values = getAttributes().getOrDefault(name, Collections.emptyList());

        if (values.isEmpty()) {
            return null;
        }

        return Collections.unmodifiableList(values);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        removeAttribute(name);

        for (String value : values) {
            ResourceAttributeEntity attr = new ResourceAttributeEntity();
            attr.setId(KeycloakModelUtils.generateId());
            attr.setName(name);
            attr.setValue(value);
            attr.setResource(entity);
            em.persist(attr);
            entity.getAttributes().add(attr);
        }
    }

    @Override
    public void removeAttribute(String name) {
        throwExceptionIfReadonly();
        Query query = em.createNamedQuery("deleteResourceAttributesByNameAndResource");

        query.setParameter("name", name);
        query.setParameter("resourceId", entity.getId());

        query.executeUpdate();

        List<ResourceAttributeEntity> toRemove = new ArrayList<>();

        for (ResourceAttributeEntity attr : entity.getAttributes()) {
            if (attr.getName().equals(name)) {
                toRemove.add(attr);
            }
        }

        entity.getAttributes().removeAll(toRemove);
    }

    @Override
    public boolean isFetched(String association) {
        return em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(this, association);
    }


    public static ResourceEntity toEntity(EntityManager em, Resource resource) {
        if (resource instanceof ResourceAdapter) {
            return ((ResourceAdapter)resource).getEntity();
        } else {
            return em.getReference(ResourceEntity.class, resource.getId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Resource)) return false;

        Resource that = (Resource) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }


}
