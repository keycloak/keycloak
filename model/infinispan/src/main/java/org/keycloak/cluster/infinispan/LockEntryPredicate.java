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

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(LockEntryPredicate.ExternalizerImpl.class)
public class LockEntryPredicate implements Predicate<Map.Entry<String, Serializable>> {

    private final Set<String> removedNodesAddresses;

    public LockEntryPredicate(Set<String> removedNodesAddresses) {
        this.removedNodesAddresses = removedNodesAddresses;
    }

    @Override
    public boolean test(Map.Entry<String, Serializable> entry) {
        if (!(entry.getValue() instanceof LockEntry)) {
            return false;
        }

        LockEntry lock = (LockEntry) entry.getValue();

        return removedNodesAddresses.contains(lock.getNode());
    }

    public static class ExternalizerImpl implements Externalizer<LockEntryPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, LockEntryPredicate obj) throws IOException {
            output.writeByte(VERSION_1);
            KeycloakMarshallUtil.writeCollection(obj.removedNodesAddresses, KeycloakMarshallUtil.STRING_EXT, output);
        }

        @Override
        public LockEntryPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public LockEntryPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            return new LockEntryPredicate(
                    KeycloakMarshallUtil.readCollection(input, KeycloakMarshallUtil.STRING_EXT, ConcurrentHashMap::newKeySet)
            );
        }
    }

}
