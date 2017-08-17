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

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginFailureEntity extends SessionEntity {

    private String userId;
    private int failedLoginNotBefore;
    private int numFailures;
    private long lastFailure;
    private String lastIPFailure;

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
        if (getRealm() != null ? !getRealm().equals(that.getRealm()) : that.getRealm() != null) return false;


        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = getRealm() != null ? getRealm().hashCode() : 0;
        hashCode = hashCode * 13 + (userId != null ? userId.hashCode() : 0);
        return hashCode;
    }

    @Override
    public String toString() {
        return String.format("LoginFailureEntity [ userId=%s, realm=%s, numFailures=%d ]", userId, getRealm(), numFailures);
    }
}
