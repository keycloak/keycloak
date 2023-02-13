/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@SerializeWith(LoginFailureEntity.ExternalizerImpl.class)
public class LoginFailureEntity extends SessionEntity {

    private String userId;
    private int failedLoginNotBefore;
    private int numFailures;
    private long lastFailure;
    private String lastIPFailure;

    public LoginFailureEntity() {
    }

    private LoginFailureEntity(String realmId, String userId, int failedLoginNotBefore, int numFailures, long lastFailure, String lastIPFailure) {
        super(realmId);
        this.userId = userId;
        this.failedLoginNotBefore = failedLoginNotBefore;
        this.numFailures = numFailures;
        this.lastFailure = lastFailure;
        this.lastIPFailure = lastIPFailure;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(int failedLoginNotBefore) {
        this.failedLoginNotBefore = failedLoginNotBefore;
    }

    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    public long getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(long lastFailure) {
        this.lastFailure = lastFailure;
    }

    public String getLastIPFailure() {
        return lastIPFailure;
    }

    public void setLastIPFailure(String lastIPFailure) {
        this.lastIPFailure = lastIPFailure;
    }

    public void clearFailures() {
        this.failedLoginNotBefore = 0;
        this.numFailures = 0;
        this.lastFailure = 0;
        this.lastIPFailure = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginFailureEntity)) return false;

        LoginFailureEntity that = (LoginFailureEntity) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (getRealmId() != null ? !getRealmId().equals(that.getRealmId()) : that.getRealmId() != null) return false;


        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = getRealmId() != null ? getRealmId().hashCode() : 0;
        hashCode = hashCode * 13 + (userId != null ? userId.hashCode() : 0);
        return hashCode;
    }

    @Override
    public String toString() {
        return String.format("LoginFailureEntity [ userId=%s, realm=%s, numFailures=%d ]", userId, getRealmId(), numFailures);
    }

    public static class ExternalizerImpl implements Externalizer<LoginFailureEntity> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, LoginFailureEntity value) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(value.getRealmId(), output);
            MarshallUtil.marshallString(value.userId, output);
            output.writeInt(value.failedLoginNotBefore);
            output.writeInt(value.numFailures);
            output.writeLong(value.lastFailure);
            MarshallUtil.marshallString(value.lastIPFailure, output);
        }

        @Override
        public LoginFailureEntity readObject(ObjectInput input) throws IOException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public LoginFailureEntity readObjectVersion1(ObjectInput input) throws IOException {
            return new LoginFailureEntity(
              MarshallUtil.unmarshallString(input),
              MarshallUtil.unmarshallString(input),
              input.readInt(),
              input.readInt(),
              input.readLong(),
              MarshallUtil.unmarshallString(input)
            );
        }
    }
}
