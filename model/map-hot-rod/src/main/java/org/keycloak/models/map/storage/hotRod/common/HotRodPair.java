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

package org.keycloak.models.map.storage.hotRod.common;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Objects;

public class HotRodPair<T, V> {

    @ProtoField(number = 1)
    public WrappedMessage key;
    @ProtoField(number = 2)
    public WrappedMessage value;

    public HotRodPair() {}

    public HotRodPair(T first, V second) {
        this.key = new WrappedMessage(first);
        this.value = new WrappedMessage(second);
    }

    public T getKey() {
        return key == null ? null : (T) key.getValue();
    }

    public V getValue() {
        return value == null ? null : (V) value.getValue();
    }

    public void setKey(T first) {
        this.key = new WrappedMessage(first);
    }

    public void setValue(V second) {
        this.value = new WrappedMessage(second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HotRodPair<?, ?> that = (HotRodPair<?, ?>) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
