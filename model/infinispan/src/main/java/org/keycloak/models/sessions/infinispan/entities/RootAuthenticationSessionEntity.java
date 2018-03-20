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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(RootAuthenticationSessionEntity.ExternalizerImpl.class)
public class RootAuthenticationSessionEntity extends SessionEntity {

    private String id;
    private int timestamp;
    private Map<String, AuthenticationSessionEntity> authenticationSessions = new ConcurrentHashMap<>();

    public RootAuthenticationSessionEntity() {
    }

    protected RootAuthenticationSessionEntity(String realmId, String id, int timestamp, Map<String, AuthenticationSessionEntity> authenticationSessions) {
        super(realmId);
        this.id = id;
        this.timestamp = timestamp;
        this.authenticationSessions = authenticationSessions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, AuthenticationSessionEntity> getAuthenticationSessions() {
        return authenticationSessions;
    }

    public void setAuthenticationSessions(Map<String, AuthenticationSessionEntity> authenticationSessions) {
        this.authenticationSessions = authenticationSessions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RootAuthenticationSessionEntity)) return false;

        RootAuthenticationSessionEntity that = (RootAuthenticationSessionEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("RootAuthenticationSessionEntity [ id=%s, realm=%s ]", getId(), getRealmId());
    }

    public static class ExternalizerImpl implements Externalizer<RootAuthenticationSessionEntity> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, RootAuthenticationSessionEntity value) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(value.getRealmId(), output);

            MarshallUtil.marshallString(value.id, output);
            output.writeInt(value.timestamp);

            KeycloakMarshallUtil.writeMap(value.authenticationSessions, KeycloakMarshallUtil.STRING_EXT, AuthenticationSessionEntity.ExternalizerImpl.INSTANCE, output);
        }

        @Override
        public RootAuthenticationSessionEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public RootAuthenticationSessionEntity readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            return new RootAuthenticationSessionEntity(
              MarshallUtil.unmarshallString(input),     // realmId

              MarshallUtil.unmarshallString(input),     // id
              input.readInt(),                          // timestamp

              KeycloakMarshallUtil.readMap(input, KeycloakMarshallUtil.STRING_EXT, AuthenticationSessionEntity.ExternalizerImpl.INSTANCE, size -> new ConcurrentHashMap<>(size)) // authenticationSessions
            );
        }
    }
}
