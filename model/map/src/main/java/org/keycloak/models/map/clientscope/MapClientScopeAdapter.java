/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.clientscope;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

public abstract class MapClientScopeAdapter<K> extends AbstractClientScopeModel<MapClientScopeEntity<K>> implements ClientScopeModel {

    public MapClientScopeAdapter(KeycloakSession session, RealmModel realm, MapClientScopeEntity<K> entity) {
        super(session, realm, entity);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(KeycloakModelUtils.convertClientScopeName(name));
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
    }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        entity.setProtocol(protocol);
    }

    @Override
    public void setAttribute(String name, String value) {
        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        List<String> attribute = entity.getAttribute(name);
        if (attribute.isEmpty()) return null;
        return attribute.get(0);
    }

    @Override
    public Map<String, String> getAttributes() {
        return entity.getAttributes().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, 
            entry -> {
                if (entry.getValue().isEmpty()) return null;
                return entry.getValue().get(0);
            })
        );
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return entity.getProtocolMappers().distinct();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (model == null) {
            return null;
        }

        ProtocolMapperModel pm = new ProtocolMapperModel();
        pm.setId(KeycloakModelUtils.generateId());
        pm.setName(model.getName());
        pm.setProtocol(model.getProtocol());
        pm.setProtocolMapper(model.getProtocolMapper());

        if (model.getConfig() != null) {
            pm.setConfig(new HashMap<>(model.getConfig()));
        } else {
            pm.setConfig(new HashMap<>());
        }

        return entity.addProtocolMapper(pm);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        final String id = mapping == null ? null : mapping.getId();
        if (id != null) {
            entity.removeProtocolMapper(id);
        }
    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        final String id = mapping == null ? null : mapping.getId();
        if (id != null) {
            entity.updateProtocolMapper(id, mapping);
        }
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return entity.getProtocolMapperById(id);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return entity.getProtocolMappers()
            .filter(pm -> Objects.equals(pm.getProtocol(), protocol) && Objects.equals(pm.getName(), name))
            .findAny()
            .orElse(null);
    }

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        return this.entity.getScopeMappings()
                .map(realm::getRoleById)
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        return getScopeMappingsStream().filter(r -> RoleUtils.isRealmRole(r, realm));
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        final String id = role == null ? null : role.getId();
        if (id != null) {
            this.entity.addScopeMapping(id);
        }
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        final String id = role == null ? null : role.getId();
        if (id != null) {
            this.entity.deleteScopeMapping(id);
        }
    }

    @Override
    public boolean hasScope(RoleModel role) {
        return RoleUtils.hasRole(getScopeMappingsStream(), role);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
