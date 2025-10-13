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

package org.keycloak.component;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.keycloak.common.util.MultivaluedHashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stored configuration of a User Storage provider instance.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class ComponentModel {

    private String id;
    private String name;
    private String providerId;
    private String providerType;
    private String parentId;
    private String subType;
    private MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
    private transient ConcurrentHashMap<String, Object> notes = new ConcurrentHashMap<>();

    public ComponentModel() {}

    public ComponentModel(ComponentModel copy) {
        this.id = copy.id;
        this.name = copy.name;
        this.providerId = copy.providerId;
        this.providerType = copy.providerType;
        this.parentId = copy.parentId;
        this.subType = copy.subType;
        this.config.addAll(copy.config);
    }


    @ProtoField(1)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ProtoField(2)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(MultivaluedHashMap<String, String> config) {
        this.config = config;
    }

    public boolean contains(String key) {
        return config.containsKey(key);
    }

    public String get(String key) {
        return config.getFirst(key);
    }

    public String get(String key, String defaultValue) {
        String s = get(key);
        return s != null ? s : defaultValue;
    }

    public int get(String key, int defaultValue) {
        String s = get(key);
        return s != null ? Integer.parseInt(s) : defaultValue;
    }

    public long get(String key, long defaultValue) {
        String s = get(key);
        return s != null ? Long.parseLong(s) : defaultValue;
    }

    public boolean get(String key, boolean defaultValue) {
        String s = get(key);
        return s != null ? Boolean.valueOf(s) : defaultValue;
    }

    public void put(String key, String value) {
        config.putSingle(key, value);
    }

    public void put(String key, int value) {
        put(key, Integer.toString(value));
    }

    public void put(String key, long value) {
        put(key, Long.toString(value));
    }

    public void put(String key, boolean value) {
        put(key, Boolean.toString(value));
    }

    public boolean hasNote(String key) {
        return notes.containsKey(key);
    }

    public <T> T getNote(String key) {
        return (T) notes.get(key);
    }

    public void setNote(String key, Object object) {
        notes.put(key, object);
    }

    public void removeNote(String key) {
        notes.remove(key);
    }

    @ProtoField(3)
    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    @ProtoField(4)
    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    @ProtoField(5)
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @ProtoField(6)
    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    @ProtoField(7)
    public List<MultiMapEntry> getConfigProto() {
        return config.entrySet().stream().map(MultiMapEntry::new).collect(Collectors.toList());
    }

    public void setConfigProto(List<MultiMapEntry> configProto) {
        if (configProto != null) {
            configProto.forEach(multiMapEntry -> multiMapEntry.insert(config));
        }
    }

    @ProtoTypeId(65538) //see org.keycloak.Marshalling
    public static final class MultiMapEntry {
        private final String key;
        private final List<String> value;

        @ProtoFactory
        public MultiMapEntry(String key, List<String> value) {
            this.key = key;
            this.value = value;
        }

        public MultiMapEntry(Map.Entry<String, List<String>> entry) {
            this(entry.getKey(), entry.getValue());
        }

        @ProtoField(1)
        public String getKey() {
            return key;
        }

        @ProtoField(2)
        public List<String> getValue() {
            return value;
        }

        public void insert(MultivaluedHashMap<String, String> config) {
            config.put(key, value);
        }
    }
}
