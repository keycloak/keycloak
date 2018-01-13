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

import java.io.*;
import java.util.Objects;
import java.util.UUID;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;

/**
 *
 * @author hmlnarik
 */
@SerializeWith(value = ActionTokenReducedKey.ExternalizerImpl.class)
public class ActionTokenReducedKey implements Serializable {

    private final String userId;
    private final String actionId;

    /**
     * Nonce that must match.
     */
    private final UUID actionVerificationNonce;

    public ActionTokenReducedKey(String userId, String actionId, UUID actionVerificationNonce) {
        this.userId = userId;
        this.actionId = actionId;
        this.actionVerificationNonce = actionVerificationNonce;
    }

    public String getUserId() {
        return userId;
    }

    public String getActionId() {
        return actionId;
    }

    public UUID getActionVerificationNonce() {
        return actionVerificationNonce;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.userId);
        hash = 71 * hash + Objects.hashCode(this.actionId);
        hash = 71 * hash + Objects.hashCode(this.actionVerificationNonce);
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
        final ActionTokenReducedKey other = (ActionTokenReducedKey) obj;
        return Objects.equals(this.userId, other.getUserId())
          && Objects.equals(this.actionId, other.getActionId())
          && Objects.equals(this.actionVerificationNonce, other.getActionVerificationNonce());
    }

    @Override
    public String toString() {
        return "userId=" + userId + ", actionId=" + actionId + ", actionVerificationNonce=" + actionVerificationNonce;
    }

    public static class ExternalizerImpl implements Externalizer<ActionTokenReducedKey> {

        @Override
        public void writeObject(ObjectOutput output, ActionTokenReducedKey t) throws IOException {
            output.writeUTF(t.userId);
            output.writeUTF(t.actionId);
            output.writeLong(t.actionVerificationNonce.getMostSignificantBits());
            output.writeLong(t.actionVerificationNonce.getLeastSignificantBits());
        }

        @Override
        public ActionTokenReducedKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return new ActionTokenReducedKey(
              input.readUTF(),
              input.readUTF(),
              new UUID(input.readLong(), input.readLong())
            );
        }
    }

}
