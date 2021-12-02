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

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientScopeAttributeEntity;
import org.keycloak.models.jpa.entities.ClientScopeEntity;
import org.keycloak.models.jpa.entities.ProtocolMapperEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientScopeAdapter implements ClientScopeModel, JpaModel<ClientScopeEntity> {

    protected KeycloakSession session;
    protected RealmModel realm;
    protected EntityManager em;
    protected ClientScopeEntity entity;

    public ClientScopeAdapter(RealmModel realm, EntityManager em, KeycloakSession session, ClientScopeEntity entity) {
        this.session = session;
        this.realm = realm;
        this.em = em;
        this.entity = entity;
    }

    public ClientScopeEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        name = KeycloakModelUtils.convertClientScopeName(name);
        entity.setName(name);
    }

    @Override
    public String getDescription() { return entity.getDescription(); }

    @Override
    public void setDescription(String description) { entity.setDescription(description); }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        entity.setProtocol(protocol);

    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return this.entity.getProtocolMappers().stream()
                .map(entity -> {
                    ProtocolMapperModel mapping = new ProtocolMapperModel();
                    mapping.setId(entity.getId());
                    mapping.setName(entity.getName());
                    mapping.setProtocol(entity.getProtocol());
                    mapping.setProtocolMapper(entity.getProtocolMapper());
                    Map<String, String> config = new HashMap<>();
                    if (entity.getConfig() != null) {
                        config.putAll(entity.getConfig());
                    }
                    mapping.setConfig(config);
                    return mapping;
                })
                .distinct();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (getProtocolMapperByName(model.getProtocol(), model.getName()) != null) {
            throw new ModelDuplicateException("Protocol mapper name must be unique per protocol");
        }
        String id = model.getId() != null ? model.getId() : KeycloakModelUtils.generateId();
        ProtocolMapperEntity entity = new ProtocolMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setProtocol(model.getProtocol());
        entity.setProtocolMapper(model.getProtocolMapper());
        entity.setClientScope(this.entity);
        entity.setConfig(model.getConfig());

        em.persist(entity);
        this.entity.getProtocolMappers().add(entity);
        return entityToModel(entity);
    }

    protected ProtocolMapperEntity getProtocolMapperEntity(String id) {
        for (ProtocolMapperEntity entity : this.entity.getProtocolMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected ProtocolMapperEntity getProtocolMapperEntityByName(String protocol, String name) {
        for (ProtocolMapperEntity entity : this.entity.getProtocolMappers()) {
            if (entity.getProtocol().equals(protocol) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity toDelete = getProtocolMapperEntity(mapping.getId());
        if (toDelete != null) {
            session.users().preRemove(mapping);

            this.entity.getProtocolMappers().remove(toDelete);
            em.remove(toDelete);
        }

    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity entity = getProtocolMapperEntity(mapping.getId());
        entity.setProtocolMapper(mapping.getProtocolMapper());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapping.getConfig());
        }
        em.flush();

    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        ProtocolMapperEntity entity = getProtocolMapperEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        ProtocolMapperEntity entity = getProtocolMapperEntityByName(protocol, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected ProtocolMapperModel entityToModel(ProtocolMapperEntity entity) {
        ProtocolMapperModel mapping = new ProtocolMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setProtocol(entity.getProtocol());
        mapping.setProtocolMapper(entity.getProtocolMapper());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        return getScopeMappingsStream().filter(r -> RoleUtils.isRealmRole(r, realm));
    }

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        return entity.getScopeMappingIds().stream()
                .map(realm::getRoleById)
                .filter(Objects::nonNull);
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        entity.getScopeMappingIds().add(role.getId());
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        entity.getScopeMappingIds().remove(role.getId());
    }

    @Override
    public boolean hasScope(RoleModel role) {
        return RoleUtils.hasRole(getScopeMappingsStream(), role);
    }

    @Override
    public void setAttribute(String name, String value) {
        for (ClientScopeAttributeEntity attr : entity.getAttributes()) {
            if (attr.getName().equals(name)) {
                attr.setValue(value);
                return;
            }
        }

        ClientScopeAttributeEntity attr = new ClientScopeAttributeEntity();
        attr.setName(name);
        attr.setValue(value);
        attr.setClientScope(entity);
        em.persist(attr);
        entity.getAttributes().add(attr);

    }

    @Override
    public void removeAttribute(String name) {
        Iterator<ClientScopeAttributeEntity> it = entity.getAttributes().iterator();
        while (it.hasNext()) {
            ClientScopeAttributeEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getAttribute(String name) {
        return getAttributes().get(name);
    }

    public static ClientScopeEntity toClientScopeEntity(ClientScopeModel model, EntityManager em) {
        if (model instanceof ClientScopeAdapter) {
            return ((ClientScopeAdapter)model).getEntity();
        }
        return em.getReference(ClientScopeEntity.class, model.getId());
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attrs = new HashMap<>();
        for (ClientScopeAttributeEntity attr : entity.getAttributes()) {
            attrs.put(attr.getName(), attr.getValue());
        }
        return attrs;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClientScopeModel)) return false;

        ClientScopeModel that = (ClientScopeModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }

}
