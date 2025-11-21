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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.ROOT_AUTHENTICATION_SESSION_ENTITY)
@Indexed
public class RootAuthenticationSessionEntity extends SessionEntity {

    private final String id;
    private int timestamp;
    private Map<String, AuthenticationSessionEntity> authenticationSessions = new ConcurrentHashMap<>();

    public RootAuthenticationSessionEntity(String id) {
        this.id = id;
    }

    protected RootAuthenticationSessionEntity(String realmId, String id, int timestamp, Map<String, AuthenticationSessionEntity> authenticationSessions) {
        super(realmId);
        this.id = id;
        this.timestamp = timestamp;
        this.authenticationSessions = authenticationSessions;
    }

    @ProtoFactory
    static RootAuthenticationSessionEntity protoFactory(String realmId, String id, int timestamp, Map<String, AuthenticationSessionEntity> authenticationSessions) {
        return new RootAuthenticationSessionEntity(realmId, id, timestamp, authenticationSessions);
    }

    @ProtoField(2)
    public String getId() {
        return id;
    }

    @ProtoField(3)
    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    @ProtoField(value = 4, mapImplementation = ConcurrentHashMap.class)
    public Map<String, AuthenticationSessionEntity> getAuthenticationSessions() {
        return authenticationSessions;
    }

    public void setAuthenticationSessions(Map<String, AuthenticationSessionEntity> authenticationSessions) {
        this.authenticationSessions = authenticationSessions;
    }

    @Override
    public boolean shouldEvaluateRemoval() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof RootAuthenticationSessionEntity that &&
                Objects.equals(id, that.id);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("RootAuthenticationSessionEntity [ id=%s, realm=%s ]", getId(), getRealmId());
    }

}
