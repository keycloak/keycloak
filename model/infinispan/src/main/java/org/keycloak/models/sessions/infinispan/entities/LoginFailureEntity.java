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

import java.util.Objects;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ProtoTypeId(Marshalling.LOGIN_FAILURE_ENTITY)
@Indexed
public class LoginFailureEntity extends SessionEntity {

    private final String userId;
    private int failedLoginNotBefore;
    private int numFailures;

    private int numTemporaryLockouts;
    private long lastFailure;
    private String lastIPFailure;

    public LoginFailureEntity(String realmId, String userId) {
        super(Objects.requireNonNull(realmId));
        this.userId = Objects.requireNonNull(userId);
    }

    @ProtoFactory
    LoginFailureEntity(String realmId, String userId, int failedLoginNotBefore, int numFailures, int numTemporaryLockouts, long lastFailure, String lastIPFailure) {
        super(realmId);
        this.userId = userId;
        this.failedLoginNotBefore = failedLoginNotBefore;
        this.numFailures = numFailures;
        this.numTemporaryLockouts = numTemporaryLockouts;
        this.lastFailure = lastFailure;
        this.lastIPFailure = lastIPFailure;
    }

    @ProtoField(2)
    public String getUserId() {
        return userId;
    }

    @ProtoField(3)
    public int getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(int failedLoginNotBefore) {
        if(failedLoginNotBefore>this.failedLoginNotBefore) {
            this.failedLoginNotBefore = failedLoginNotBefore;
        }
    }

    @ProtoField(4)
    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    @ProtoField(5)
    public int getNumTemporaryLockouts() {
        return numTemporaryLockouts;
    }

    public void setNumTemporaryLockouts(int numTemporaryLockouts) {
        this.numTemporaryLockouts = numTemporaryLockouts;
    }

    @ProtoField(6)
    public long getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(long lastFailure) {
        if(lastFailure>this.lastFailure) {
            this.lastFailure = lastFailure;
        }
    }

    @ProtoField(7)
    public String getLastIPFailure() {
        return lastIPFailure;
    }

    public void setLastIPFailure(String lastIPFailure) {
        this.lastIPFailure = lastIPFailure;
    }

    public void clearFailures() {
        this.failedLoginNotBefore = 0;
        this.numFailures = 0;
        this.numTemporaryLockouts = 0;
        this.lastFailure = 0;
        this.lastIPFailure = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginFailureEntity that)) return false;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(getRealmId(), that.getRealmId());
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

}
