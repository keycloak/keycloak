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

package org.keycloak.models.map.common;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoField;

public class HotRodPair<T, V> {

    @ProtoField(number = 1)
    public WrappedMessage firstWrapped;
    @ProtoField(number = 2)
    public WrappedMessage secondWrapped;

    public HotRodPair() {}

    public HotRodPair(T first, V second) {
        this.firstWrapped = new WrappedMessage(first);
        this.secondWrapped = new WrappedMessage(second);
    }

    public T getFirst() {
        return firstWrapped == null ? null : (T) firstWrapped.getValue();
    }

    public V getSecond() {
        return secondWrapped == null ? null : (V) secondWrapped.getValue();
    }

    public void setFirst(T first) {
        this.firstWrapped = new WrappedMessage(first);
    }

    public void setSecond(V second) {
        this.secondWrapped = new WrappedMessage(second);
    }
}
