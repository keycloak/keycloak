/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.sessions.infinispan.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.SingleUseObjectValueModel;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author hmlnarik
 */
@ProtoTypeId(Marshalling.SINGLE_USE_OBJECT_VALUE_ENTITY)
public class SingleUseObjectValueEntity implements SingleUseObjectValueModel {

    private final Map<String, String> notes;

    @ProtoFactory
    public SingleUseObjectValueEntity(Map<String, String> notes) {
        if (notes == null || notes.isEmpty()) {
            this.notes = Map.of();
            return;
        }
        var copy = new HashMap<>(notes);
        // protostream does not support null values for primitive types as string
        copy.values().removeIf(Objects::isNull);
        this.notes = Map.copyOf(copy);
    }

    @ProtoField(value = 1, mapImplementation = HashMap.class)
    @Override
    public Map<String, String> getNotes() {
        return notes;
    }

    @Override
    public String getNote(String name) {
        return notes.get(name);
    }

    @Override
    public String toString() {
        return String.format("SingleUseObjectValueEntity [ notes=%s ]", notes.toString());
    }

}
