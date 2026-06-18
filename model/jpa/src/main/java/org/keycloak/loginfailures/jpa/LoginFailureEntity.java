/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.loginfailures.jpa;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.keycloak.connections.jpa.AsynchronousCommitAllowed;

/**
 * This holds information about failed logins.
 * <p>
 * This table has no version column for optimistic locking, as it will be accessed via the {@link UserLoginFailureAdapter} only with pessimistic locking,
 * and with idempotent methods.
 */
@NamedQueries({
        @NamedQuery(
                name = "insertLoginFailure",
                query = "insert into LoginFailureEntity (realmId, userId) values (:realmId, :userId)" +
                        " on conflict (realmId, userId) do nothing"
        ),
        @NamedQuery(
                name = "deleteLoginFailureByRealm",
                query = "delete from LoginFailureEntity e where e.realmId = :realmId"
        ),
        @NamedQuery(
                name = "findExpiredLoginFailureUserIdsByRealm",
                query = "select e.userId from LoginFailureEntity e where e.realmId = :realmId and e.lastFailure < :expire"
        ),
        @NamedQuery(
                name = "deleteExpiredLoginFailureByRealmAndUserIds",
                query = "delete from LoginFailureEntity e where e.realmId = :realmId and e.userId in :userIds and e.lastFailure < :expire"
        ),
})
@Entity
@IdClass(LoginFailureKey.class)
@Table(name = "LOGIN_FAILURE")
public class LoginFailureEntity implements AsynchronousCommitAllowed {

    @Id
    @Column(name = "REALM_ID", length = 36)
    private String realmId;

    @Id
    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "FAILED_LOGIN_NOT_BEFORE")
    private long failedLoginNotBefore;

    @Column(name = "NUM_FAILURES")
    private int numFailures;

    @Column(name = "NUM_TEMPORARY_LOCKOUTS")
    private int numTemporaryLockouts;

    @Column(name = "LAST_FAILURE")
    private long lastFailure;

    @Column(name = "LAST_IP_FAILURE")
    private String lastIPFailure;

    @Column(name = "NUM_SECONDARY_AUTH_FAILURES")
    private int numSecondaryAuthFailures;

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(long failedLoginNotBefore) {
        this.failedLoginNotBefore = failedLoginNotBefore;
    }

    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    public int getNumTemporaryLockouts() {
        return numTemporaryLockouts;
    }

    public void setNumTemporaryLockouts(int numTemporaryLockouts) {
        this.numTemporaryLockouts = numTemporaryLockouts;
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

    public int getNumSecondaryAuthFailures() {
        return numSecondaryAuthFailures;
    }

    public void setNumSecondaryAuthFailures(int numSecondaryAuthFailures) {
        this.numSecondaryAuthFailures = numSecondaryAuthFailures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginFailureEntity that)) return false;
        return Objects.equals(realmId, that.realmId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realmId, userId);
    }

    @Override
    public String toString() {
        return String.format("LoginFailureEntity [ userId=%s, realmId=%s, numFailures=%d, numSecondaryAuthFailures=%d ]",
                userId, realmId, numFailures, numSecondaryAuthFailures);
    }
}
