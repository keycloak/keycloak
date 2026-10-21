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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 *
 * @author hmlnarik
 */
@ProtoTypeId(Marshalling.AUTHENTICATED_CLIENT_SESSION_STORE)
public class AuthenticatedClientSessionStore {

    /**
     * Maps client UUID to client session ID.
     */
    private final ConcurrentMap<String, UUID> authenticatedClientSessionIds;

    public AuthenticatedClientSessionStore() {
        authenticatedClientSessionIds = new ConcurrentHashMap<>();
    }

    @ProtoFactory
    AuthenticatedClientSessionStore(ConcurrentMap<String, UUID> authenticatedClientSessionIds) {
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

    @ProtoField(value = 1, mapImplementation = ConcurrentHashMap.class)
    ConcurrentMap<String, UUID> getAuthenticatedClientSessionIds() {
        return authenticatedClientSessionIds;
    }

    @Override
    public String toString() {
        return this.authenticatedClientSessionIds.toString();
    }

}
