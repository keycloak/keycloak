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

package org.keycloak.models.map.storage.jpa.role.delegate;

import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.role.MapRoleEntityDelegate;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleCompositeEntity;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleCompositeEntityKey;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Delegate for the JPA implementation for MapRoleEntityDelegate.
 * It will delegate all access to the composite roles to a separate table.
 *
 * For performance reasons, it caches the composite roles within the session if they have already been retrieved.
 * This relies on the behavior of {@link org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction} that
 * each entity is created only once within each session.
 *
 * @author Alexander Schwartz
 */
public class JpaMapRoleEntityDelegate extends MapRoleEntityDelegate {
    private final EntityManager em;

    private Set<String> compositeRoles;

    public JpaMapRoleEntityDelegate(JpaRoleEntity original, EntityManager em) {
        super(new JpaRoleDelegateProvider(original, em));
        this.em = em;
    }

    @Override
    public Set<String> getCompositeRoles() {
        if (compositeRoles == null) {
            TypedQuery<JpaRoleCompositeEntityKey> query = em.createNamedQuery("selectChildRolesFromCompositeRole", JpaRoleCompositeEntityKey.class);
            query.setParameter("roleId", StringKeyConverter.UUIDKey.INSTANCE.fromString(getId()));
            compositeRoles = query.getResultList().stream().map(JpaRoleCompositeEntityKey::getChildRoleId).collect(Collectors.toSet());
        }
        return compositeRoles;
    }

    @Override
    public void setCompositeRoles(Set<String> compositeRoles) {
        Query query = em.createNamedQuery("deleteAllChildRolesFromCompositeRole");
        query.setParameter("roleId", StringKeyConverter.UUIDKey.INSTANCE.fromString(getId()));
        query.executeUpdate();
        compositeRoles.forEach(this::addCompositeRole);
        this.compositeRoles = compositeRoles;
    }

    @Override
    public void addCompositeRole(String roleId) {
        JpaRoleCompositeEntityKey key = new JpaRoleCompositeEntityKey(getId(), roleId);
        if (compositeRoles != null) {
            if (compositeRoles.contains(roleId)) {
                return;
            }
        } else {
            if (em.find(JpaRoleCompositeEntity.class, key) != null) {
                return;
            }
        }
        JpaRoleCompositeEntity entity = new JpaRoleCompositeEntity(key);
        em.persist(entity);
        if (compositeRoles != null) {
            compositeRoles.add(roleId);
        }
    }

    @Override
    public void removeCompositeRole(String roleId) {
        Query query = em.createNamedQuery("deleteChildRoleFromCompositeRole");
        query.setParameter("roleId", StringKeyConverter.UUIDKey.INSTANCE.fromString(getId()));
        query.setParameter("childRoleId", roleId);
        query.executeUpdate();
        if (compositeRoles != null) {
            compositeRoles.remove(roleId);
        }
    }

}
