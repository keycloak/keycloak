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

import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.models.ActionTokenValueModel;

import java.io.*;
import java.util.*;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author hmlnarik
 */
@SerializeWith(ActionTokenValueEntity.ActionTokenValueEntityExternalizer.class)
public class ActionTokenValueEntity implements ActionTokenValueModel {

    @SerializeWith(Key.KeyExternalizer.class)
    public static class Key implements Serializable, ActionTokenKeyModel {
        private final String userId;
        private final String actionId;

        public Key(String userId, String actionId) {
            this.userId = userId;
            this.actionId = actionId;
        }

        @Override
        public String getUserId() {
            return userId;
        }

        @Override
        public String getActionId() {
            return actionId;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.userId);
            hash = 71 * hash + Objects.hashCode(this.actionId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ActionTokenKeyModel other = (ActionTokenKeyModel) obj;

            return Objects.equals(this.userId, other.getUserId())
              && Objects.equals(this.actionId, other.getActionId());
        }

        private static class KeyExternalizer implements Externalizer<Key> {

            @Override
            public void writeObject(ObjectOutput output, Key t) throws IOException {
                output.writeUTF(t.userId);
                output.writeUTF(t.actionId);
            }

            @Override
            public Key readObject(ObjectInput input) throws IOException, ClassNotFoundException {
                return new Key(input.readUTF(), input.readUTF());
            }
        }
    }

    /**
     * Token that must match.
     */
    private final UUID actionVerificationNonce;

    /**
     * Expiration time in seconds in timezone of Keycloak.
     */
    private final int expirationInSecs;

    private final Map<String, String> notes;

    public ActionTokenValueEntity(ActionTokenValueModel actionToken) {
        this.expirationInSecs = actionToken.getExpiration();
        this.actionVerificationNonce = actionToken.getActionVerificationNonce();
        this.notes = new HashMap<>(actionToken.getNotes());
    }

    public ActionTokenValueEntity(UUID actionVerificationNonce, int expirationInSecs) {
        this(actionVerificationNonce, expirationInSecs, Collections.EMPTY_MAP);
    }

    public ActionTokenValueEntity(UUID actionVerificationNonce, int expirationInSecs, Map<String, String> notes) {
        this.actionVerificationNonce = actionVerificationNonce;
        this.expirationInSecs = expirationInSecs;
        this.notes = notes == null ? Collections.EMPTY_MAP : new HashMap<>(notes);
    }

    @Override
    public UUID getActionVerificationNonce() {
        return actionVerificationNonce;
    }

    @Override
    public int getExpiration() {
        return expirationInSecs;
    }

    @Override
    public Map<String, String> getNotes() {
        return Collections.unmodifiableMap(notes);
    }

    @Override
    public String getNote(String name) {
        return notes.get(name);
    }

    public static class ActionTokenValueEntityExternalizer implements Externalizer<ActionTokenValueEntity> {

        private static final int VERSION = 1;

        @Override
        public void writeObject(ObjectOutput output, ActionTokenValueEntity t) throws IOException {
            output.writeByte(VERSION);
            output.writeLong(t.actionVerificationNonce.getMostSignificantBits());
            output.writeLong(t.actionVerificationNonce.getLeastSignificantBits());
            output.writeInt(t.expirationInSecs);

            output.writeBoolean(! t.notes.isEmpty());
            if (! t.notes.isEmpty()) {
                output.writeObject(t.notes);
            }
        }

        @Override
        public ActionTokenValueEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            byte version = input.readByte();
            
            if (version != 1) {
                throw new IOException("Invalid version: " + version);
            }
            UUID actionVerificationNonce = new UUID(input.readLong(), input.readLong());;
            int expirationInSecs = input.readInt();
            boolean notesEmpty = input.readBoolean();

            Map<String, String> notes = notesEmpty ? Collections.EMPTY_MAP : (Map<String, String>) input.readObject();
            
            return new ActionTokenValueEntity(actionVerificationNonce, expirationInSecs, notes);
        }

    }
}
