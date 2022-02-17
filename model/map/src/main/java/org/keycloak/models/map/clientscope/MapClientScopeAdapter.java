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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperUtils;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

public class MapClientScopeAdapter extends AbstractClientScopeModel<MapClientScopeEntity> implements ClientScopeModel {

    private static final Logger LOG = Logger.getLogger(MapClientScopeAdapter.class);
    private final MapProtocolMapperUtils pmUtils;

    public MapClientScopeAdapter(KeycloakSession session, RealmModel realm, MapClientScopeEntity entity) {
        super(session, realm, entity);
        pmUtils = MapProtocolMapperUtils.instanceFor(safeGetProtocol());
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
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public void setAttribute(String name, String value) {
//  TODO: https://github.com/keycloak/keycloak/issues/9741
//        boolean valueUndefined = value == null || "".equals(value.trim());
//
//        if (valueUndefined) {
//            removeAttribute(name);
//            return;
//        }

        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        List<String> attribute = entity.getAttribute(name);
        if (attribute == null || attribute.isEmpty()) return null;
        return attribute.get(0);
    }

    @Override
    public Map<String, String> getAttributes() {
        final Map<String, List<String>> attributes = entity.getAttributes();
        final Map<String, List<String>> a = attributes == null ? Collections.emptyMap() : attributes;
        return a.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            entry -> {
                if (entry.getValue().isEmpty()) {
                    return null;
                } else if (entry.getValue().size() > 1) {
                    // This could be caused by an inconsistency in the storage, a programming error,
                    // or a downgrade from a future version of Keycloak that already supports multi-valued attributes.
                    // The caller will not see the other values, and when this entity is later updated, the additional values will be lost.
                    LOG.warnf("ClientScope '%s' realm '%s' has attribute '%s' with %d values, retrieving only the first", getName(), getRealm().getName(), entry.getKey(),
                            entry.getValue().size());
                }
                return entry.getValue().get(0);
            })
        );
    }

    /*************** Protocol mappers ****************/

    private String safeGetProtocol() {
        return entity.getProtocol() == null ? "openid-connect" : entity.getProtocol();
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        final Set<MapProtocolMapperEntity> protocolMappers = entity.getProtocolMappers();
        return protocolMappers == null ? Stream.empty() : protocolMappers.stream().distinct().map(pmUtils::toModel);
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (model == null) {
            return null;
        }

        MapProtocolMapperEntity pm = MapProtocolMapperUtils.fromModel(model);
        if (pm.getId() == null) {
            String id = KeycloakModelUtils.generateId();
            pm.setId(id);
        }
        if (model.getConfig() == null) {
            pm.setConfig(new HashMap<>());
        }

        entity.addProtocolMapper(pm);
        return pmUtils.toModel(pm);
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
            entity.getProtocolMapper(id).ifPresent((pmEntity) -> {
                entity.removeProtocolMapper(id);
                addProtocolMapper(mapping);
            });
        }
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return entity.getProtocolMapper(id).map(pmUtils::toModel).orElse(null);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        final Set<MapProtocolMapperEntity> protocolMappers = entity.getProtocolMappers();
        if (! Objects.equals(protocol, safeGetProtocol())) {
            return null;
        }
        return protocolMappers == null ? null : protocolMappers.stream()
          .filter(pm -> Objects.equals(pm.getName(), name))
          .map(pmUtils::toModel)
          .findAny()
          .orElse(null);
    }

    /*************** Scopes mappings ****************/

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        final Collection<String> scopeMappings = this.entity.getScopeMappings();
        return scopeMappings == null ? Stream.empty() : scopeMappings.stream()
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
            this.entity.removeScopeMapping(id);
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
