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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.map.common.AbstractEntity;

public class MapClientScopeEntity<K> implements AbstractEntity<K> {

    private final K id;
    private final String realmId;

    private String name;
    private String protocol;
    private String description;

    private final Set<String> scopeMappings = new LinkedHashSet<>();
    private final Map<String, ProtocolMapperModel> protocolMappers = new HashMap<>();
    private final Map<String, List<String>> attributes = new HashMap<>();

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */
    protected boolean updated;

    protected MapClientScopeEntity() {
        this.id = null;
        this.realmId = null;
    }

    public MapClientScopeEntity(K id, String realmId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(realmId, "realmId");

        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isUpdated() {
        return this.updated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated |= ! Objects.equals(this.name, name);
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.updated |= ! Objects.equals(this.description, description);
        this.description = description;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.updated |= ! Objects.equals(this.protocol, protocol);
        this.protocol = protocol;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttribute(String name, List<String> values) {
        this.updated |= ! Objects.equals(this.attributes.put(name, values), values);
    }

    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        Objects.requireNonNull(model.getId(), "protocolMapper.id");
        updated = true;
        this.protocolMappers.put(model.getId(), model);
        return model;
    }

    public Stream<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers.values().stream();
    }

    public void updateProtocolMapper(String id, ProtocolMapperModel mapping) {
        updated = true;
        protocolMappers.put(id, mapping);
    }

    public void removeProtocolMapper(String id) {
        updated |= protocolMappers.remove(id) != null;
    }

    public void setProtocolMappers(Collection<ProtocolMapperModel> protocolMappers) {
        this.updated |= ! Objects.equals(this.protocolMappers, protocolMappers);
        this.protocolMappers.clear();
        this.protocolMappers.putAll(protocolMappers.stream().collect(Collectors.toMap(ProtocolMapperModel::getId, Function.identity())));
    }

    public ProtocolMapperModel getProtocolMapperById(String id) {
        return id == null ? null : protocolMappers.get(id);
    }

    public void removeAttribute(String name) {
        this.updated |= this.attributes.remove(name) != null;
    }

    public List<String> getAttribute(String name) {
        return attributes.getOrDefault(name, Collections.EMPTY_LIST);
    }

    public String getRealmId() {
        return this.realmId;
    }

    public Stream<String> getScopeMappings() {
        return scopeMappings.stream();
    }

    public void addScopeMapping(String id) {
        if (id != null) {
            updated = true;
            scopeMappings.add(id);
        }
    }

    public void deleteScopeMapping(String id) {
        updated |= scopeMappings.remove(id);
    }
}
