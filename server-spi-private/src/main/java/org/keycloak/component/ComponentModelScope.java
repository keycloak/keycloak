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
package org.keycloak.component;

import java.util.Set;

import org.keycloak.Config.Scope;

/**
 *
 * @author hmlnarik
 */
public class ComponentModelScope implements Scope {

    private final Scope origScope;
    private final ComponentModel componentConfig;
    private final String prefix;

    public ComponentModelScope(Scope origScope, ComponentModel componentConfig) {
        this(origScope, componentConfig, "");
    }

    public ComponentModelScope(Scope origScope, ComponentModel componentConfig, String prefix) {
        this.origScope = origScope;
        this.componentConfig = componentConfig;
        this.prefix = prefix;
    }

    public String getComponentId() {
        return componentConfig.getId();
    }

    public String getComponentName() {
        return componentConfig.getName();
    }

    public <T> T getComponentNote(String key) {
        return componentConfig.getNote(key);
    }

    public String getComponentParentId() {
        return componentConfig.getParentId();
    }

    public String getComponentSubType() {
        return componentConfig.getSubType();
    }

    @Override
    public String get(String key) {
        return get(key, null);
    }

    @Override
    public String get(String key, String defaultValue) {
        final String res = componentConfig.get(prefix + key, null);
        return (res == null) ? origScope.get(key, defaultValue) : res;
    }

    @Override
    public String[] getArray(String key) {
        final String[] res = get(prefix + key, "").split("\\s*,\\s*");
        return (res == null) ? origScope.getArray(key) : res;
    }

    @Override
    public Integer getInt(String key) {
        return getInt(key, null);
    }

    @Override
    public Integer getInt(String key, Integer defaultValue) {
        final String res = componentConfig.get(prefix + key, null);
        return (res == null) ? origScope.getInt(key, defaultValue) : Integer.valueOf(res);
    }

    @Override
    public Long getLong(String key) {
        return getLong(key, null);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        final String res = componentConfig.get(prefix + key, null);
        return (res == null) ? origScope.getLong(key, defaultValue) : Long.valueOf(res);
    }

    @Override
    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        final String res = componentConfig.get(prefix + key, null);
        return (res == null) ? origScope.getBoolean(key, defaultValue) : Boolean.valueOf(res);
    }

    @Override
    public Scope scope(String... scope) {
        return new ComponentModelScope(origScope.scope(scope), componentConfig, String.join(".", scope) + ".");
    }

    @Override
    public Set<String> getPropertyNames() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ComponentModel getComponentModel() {
        return componentConfig;
    }

    @Override
    public Scope root() {
        return this.origScope.root();
    }

}
