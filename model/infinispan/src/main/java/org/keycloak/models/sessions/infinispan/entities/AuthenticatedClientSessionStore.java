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

import org.keycloak.models.sessions.infinispan.util.KeycloakMarshallUtil;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;

/**
 *
 * @author hmlnarik
 */
@SerializeWith(AuthenticatedClientSessionStore.ExternalizerImpl.class)
public class AuthenticatedClientSessionStore {

    /**
     * Maps client UUID to client session ID.
     */
    private final ConcurrentHashMap<String, UUID> authenticatedClientSessionIds;

    public AuthenticatedClientSessionStore() {
        authenticatedClientSessionIds = new ConcurrentHashMap<>();
    }

    private AuthenticatedClientSessionStore(ConcurrentHashMap<String, UUID> authenticatedClientSessionIds) {
        this.authenticatedClientSessionIds = authenticatedClientSessionIds;
    }

    public void clear() {
        authenticatedClientSessionIds.clear();
    }

    public boolean containsKey(String key) {
        return authenticatedClientSessionIds.containsKey(key);
    }

    public void forEach(BiConsumer<? super String, ? super UUID> action) {
        authenticatedClientSessionIds.forEach(action);
    }

    public UUID get(String key) {
        return authenticatedClientSessionIds.get(key);
    }

    public Set<String> keySet() {
        return authenticatedClientSessionIds.keySet();
    }

    public UUID put(String key, UUID value) {
        return authenticatedClientSessionIds.put(key, value);
    }

    public UUID remove(String clientUUID) {
        return authenticatedClientSessionIds.remove(clientUUID);
    }

    public int size() {
        return authenticatedClientSessionIds.size();
    }

    @Override
    public String toString() {
        return this.authenticatedClientSessionIds.toString();
    }

    public static class ExternalizerImpl implements Externalizer<AuthenticatedClientSessionStore> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, AuthenticatedClientSessionStore obj) throws IOException {
            output.writeByte(VERSION_1);

            KeycloakMarshallUtil.writeMap(obj.authenticatedClientSessionIds, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.UUID_EXT, output);
        }

        @Override
        public AuthenticatedClientSessionStore readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public AuthenticatedClientSessionStore readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            AuthenticatedClientSessionStore res = new AuthenticatedClientSessionStore(
              KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, KeycloakMarshallUtil.UUID_EXT, ConcurrentHashMap::new)
            );
            return res;
        }
    }
}
