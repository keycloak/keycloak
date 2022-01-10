/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.loginFailure;

import org.keycloak.models.map.common.AbstractEntity;

import org.keycloak.models.map.common.UpdatableEntity;
import java.util.Objects;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserLoginFailureEntity extends UpdatableEntity.Impl implements AbstractEntity {
    private String id;
    private String realmId;
    private String userId;

    private int failedLoginNotBefore;
    private int numFailures;
    private long lastFailure;
    private String lastIPFailure;

    public MapUserLoginFailureEntity() {}

    public MapUserLoginFailureEntity(String id, String realmId, String userId) {
        this.id = id;
        this.realmId = realmId;
        this.userId = userId;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        if (this.id != null) throw new IllegalStateException("Id cannot be changed");
        this.id = id;
        this.updated |= id != null;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.updated |= !Objects.equals(this.realmId, realmId);
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.updated |= !Objects.equals(this.userId, userId);
        this.userId = userId;
    }

    public int getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(int failedLoginNotBefore) {
        this.updated |= this.failedLoginNotBefore != failedLoginNotBefore;
        this.failedLoginNotBefore = failedLoginNotBefore;
    }

    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.updated |= this.numFailures != numFailures;
        this.numFailures = numFailures;
    }

    public long getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(long lastFailure) {
        this.updated |= this.lastFailure != lastFailure;
        this.lastFailure = lastFailure;
    }

    public String getLastIPFailure() {
        return lastIPFailure;
    }

    public void setLastIPFailure(String lastIPFailure) {
        this.updated |= !Objects.equals(this.lastIPFailure, lastIPFailure);
        this.lastIPFailure = lastIPFailure;
    }

    public void clearFailures() {
        this.updated |= this.failedLoginNotBefore != 0 || this.numFailures != 0 ||
                this.lastFailure != 0l || this.lastIPFailure != null;
        this.failedLoginNotBefore = this.numFailures = 0;
        this.lastFailure = 0l;
        this.lastIPFailure = null;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }
}
