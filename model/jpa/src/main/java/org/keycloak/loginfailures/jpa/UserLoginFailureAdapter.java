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

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.models.UserLoginFailureModel;

public class UserLoginFailureAdapter implements UserLoginFailureModel {

    private final EntityManager em;
    private final LoginFailureEntity entity;
    private boolean locked;

    public UserLoginFailureAdapter(EntityManager em, LoginFailureEntity entity) {
        this.em = Objects.requireNonNull(em);
        this.entity = Objects.requireNonNull(entity);
    }

    private void ensureLocked() {
        if (!locked) {
            em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            locked = true;
        }
    }

    @Override
    public String getId() {
        return entity.getRealmId() + ":" + entity.getUserId();
    }

    @Override
    public String getUserId() {
        return entity.getUserId();
    }

    @Override
    public int getFailedLoginNotBefore() {
        return Math.toIntExact(entity.getFailedLoginNotBefore());
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        ensureLocked();
        entity.setFailedLoginNotBefore(notBefore);
    }

    @Override
    public int getNumFailures() {
        return entity.getNumFailures();
    }

    @Override
    public void incrementFailures() {
        ensureLocked();
        entity.setNumFailures(entity.getNumFailures() + 1);
    }

    @Override
    public int getNumTemporaryLockouts() {
        return entity.getNumTemporaryLockouts();
    }

    @Override
    public void incrementTemporaryLockouts() {
        ensureLocked();
        entity.setNumTemporaryLockouts(entity.getNumTemporaryLockouts() + 1);
    }

    @Override
    public void clearFailures() {
        ensureLocked();
        entity.setFailedLoginNotBefore(0);
        entity.setNumFailures(0);
        entity.setNumTemporaryLockouts(0);
        entity.setLastFailure(0);
        entity.setLastIPFailure(null);
    }

    @Override
    public long getLastFailure() {
        return entity.getLastFailure();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        ensureLocked();
        entity.setLastFailure(lastFailure);
    }

    @Override
    public String getLastIPFailure() {
        return entity.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String ip) {
        ensureLocked();
        entity.setLastIPFailure(ip);
    }

    @Override
    public int getNumSecondaryAuthFailures() {
        return entity.getNumSecondaryAuthFailures();
    }

    @Override
    public void incrementSecondaryAuthFailures() {
        ensureLocked();
        entity.setNumSecondaryAuthFailures(entity.getNumSecondaryAuthFailures() + 1);
    }

    @Override
    public void clearPrimaryAndSecondaryAuthFailures() {
        clearFailures();
        entity.setNumSecondaryAuthFailures(0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        UserLoginFailureAdapter that = (UserLoginFailureAdapter) o;
        return entity.equals(that.entity);
    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }
}
