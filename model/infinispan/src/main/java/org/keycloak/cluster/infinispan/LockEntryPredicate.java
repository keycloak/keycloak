/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.cluster.infinispan;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.LOCK_ENTRY_PREDICATE)
public class LockEntryPredicate implements Predicate<Map.Entry<String, Object>> {

    private final Set<String> removedNodesAddresses;

    @ProtoFactory
    public LockEntryPredicate(Set<String> removedNodesAddresses) {
        this.removedNodesAddresses = removedNodesAddresses;
    }

    @ProtoField(value = 1, collectionImplementation = HashSet.class)
    Set<String> getRemovedNodesAddresses() {
        return removedNodesAddresses;
    }

    @Override
    public boolean test(Map.Entry<String, Object> entry) {
        return entry.getValue() instanceof LockEntry lock &&
                removedNodesAddresses.contains(lock.node());

    }
}
