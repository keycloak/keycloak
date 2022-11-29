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
package org.keycloak.testsuite.model.storage.tree.sample;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.common.delegate.HasEntityFieldDelegate;

/**
 *
 * @author hmlnarik
 */
public class Dict<E> extends UpdatableEntity.Impl implements EntityFieldDelegate<E> {

    public static final String CLIENT_FIELD_LOGO = "LOGO";
    public static final String CLIENT_FIELD_ENABLED = "ENABLED";
    public static final String CLIENT_FIELD_NAME = "NAME";

    private static final Set<String> CLIENT_ALLOWED_KEYS = new HashSet<>(Arrays.asList(CLIENT_FIELD_NAME, CLIENT_FIELD_ENABLED, CLIENT_FIELD_LOGO));

    public static MapClientEntity clientDelegate() {
        // To be replaced by dynamic mapper config
        Map<String, String> fieldName2key = new HashMap<>();
        fieldName2key.put(MapClientEntityFields.ID.getName(), CLIENT_FIELD_NAME);
        fieldName2key.put(MapClientEntityFields.CLIENT_ID.getName(), CLIENT_FIELD_NAME);
        fieldName2key.put(MapClientEntityFields.ENABLED.getName(), CLIENT_FIELD_ENABLED);

        Map<String, String> attributeName2key = new HashMap<>();
        attributeName2key.put("logo", CLIENT_FIELD_LOGO);

        Dict<MapClientEntity> dict = new Dict<>(CLIENT_ALLOWED_KEYS, fieldName2key, attributeName2key);
        return DeepCloner.DUMB_CLONER.entityFieldDelegate(MapClientEntity.class, dict);
    }

    @SuppressWarnings("unchecked")
    public static <E> Dict<E> asDict(E entity) {
        return (entity instanceof HasEntityFieldDelegate && ((HasEntityFieldDelegate<?>) entity).getEntityFieldDelegate() instanceof Dict)
          ? (Dict<E>) ((HasEntityFieldDelegate<E>) entity).getEntityFieldDelegate()
          : null;
    }

    private final Set<String> allowedKeys;
    private final Map<String, Object> contents = new HashMap<>();
    private final Map<String, String> fieldName2key;
    private final Map<String, String> attributeName2key;

    public Dict(Set<String> allowedKeys, Map<String, String> fieldName2key, Map<String, String> attributeName2key) {
        this.allowedKeys = allowedKeys;
        this.fieldName2key = fieldName2key;
        this.attributeName2key = attributeName2key;
    }

    @Override
    public <EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object get(EF field) {
        if ("Attributes".equals(field.getName())) {
            return attributeName2key.entrySet().stream()
              .filter(me -> get(me.getValue()) != null)
              .collect(Collectors.toMap(me -> me.getKey(), me -> Collections.singletonList(get(me.getValue()))));
        }
        String key = fieldName2key.get(field.getName());
        if (key != null) {
            return get(key);
        }
        return null;
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void set(EF field, T value) {
        String key = fieldName2key.get(field.getName());
        if (key != null) {
            put(key, value);
        }
    }

    @Override
    public <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapGet(EF field, K key) {
        if ("Attributes".equals(field.getName()) && attributeName2key.containsKey(key)) {
            Object v = get(attributeName2key.get(key));
            return v == null ? null : Collections.singletonList(get(attributeName2key.get(key)));
        }
        return null;
    }

    @Override
    public <K, T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void mapPut(EF field, K key, T value) {
        if ("Attributes".equals(field.getName()) && attributeName2key.containsKey(key) && (value instanceof List)) {
            List<?> l = (List<?>) value;
            if (l.isEmpty()) {
                remove(attributeName2key.get(key));
            } else {
                put(attributeName2key.get(key), l.get(0));
            }
        }
    }

    @Override
    public <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapRemove(EF field, K key) {
        if ("Attributes".equals(field.getName()) && attributeName2key.containsKey(key)) {
            Object o = remove(attributeName2key.get(key));
            return o == null ? null : Collections.singletonList(o);
        }
        return null;
    }

    protected boolean isKeyAllowed(String key) {
        return allowedKeys.contains(key);
    }

    public Object get(String key) {
        return isKeyAllowed(key) ? contents.get(key) : null;
    }

    public void put(String key, Object value) {
        if (isKeyAllowed(key)) {
            updated |= ! Objects.equals(contents.put(key, value), value);
        }
    }

    public Object remove(String key) {
        key = key == null ? null : key.toUpperCase();
        if (isKeyAllowed(key)) {
            Object res = contents.remove(key);
            updated |= res != null;
            return res;
        }
        return null;
    }
}
