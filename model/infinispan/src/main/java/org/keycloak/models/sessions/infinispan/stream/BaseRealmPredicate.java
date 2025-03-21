/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.stream;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.infinispan.protostream.annotations.ProtoField;

abstract class BaseRealmPredicate<K, V> implements Predicate<Map.Entry<K, V>> {

    @ProtoField(1)
    final String realmId;

    BaseRealmPredicate(String realmId) {
        this.realmId = Objects.requireNonNull(realmId);
    }

    @Override
    public boolean test(Map.Entry<K, V> entry) {
        return realmId.equals(realmIdFrom(entry.getValue()));
    }

    abstract String realmIdFrom(V value);
}
