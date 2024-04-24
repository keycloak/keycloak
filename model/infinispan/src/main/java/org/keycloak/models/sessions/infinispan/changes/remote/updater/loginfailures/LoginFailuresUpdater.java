/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.sessions.infinispan.changes.remote.updater.loginfailures;

import org.infinispan.client.hotrod.MetadataValue;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.BaseUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Expiration;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Updater;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Implementation of {@link Updater} and {@link UserLoginFailureModel}.
 * <p>
 * It keeps track of the changes made to the entity {@link LoginFailureEntity} and replays on commit.
 */
public class LoginFailuresUpdater extends BaseUpdater<LoginFailureKey, LoginFailureEntity> implements UserLoginFailureModel {

    private final List<Consumer<LoginFailureEntity>> changes;

    private LoginFailuresUpdater(LoginFailureKey key, LoginFailureEntity entity, long version, UpdaterState initialState) {
        super(key, entity, version, initialState);
        changes = new ArrayList<>(4);
    }

    public static LoginFailuresUpdater create(LoginFailureKey key, LoginFailureEntity entity) {
        return new LoginFailuresUpdater(Objects.requireNonNull(key), Objects.requireNonNull(entity), -1, UpdaterState.CREATED);
    }

    public static LoginFailuresUpdater wrap(LoginFailureKey key, MetadataValue<LoginFailureEntity> entity) {
        return new LoginFailuresUpdater(Objects.requireNonNull(key), Objects.requireNonNull(entity.getValue()), entity.getVersion(), UpdaterState.READ_ONLY);
    }

    public static LoginFailuresUpdater delete(LoginFailureKey key) {
        return new LoginFailuresUpdater(Objects.requireNonNull(key), null, -1, UpdaterState.DELETED);
    }

    @Override
    public Expiration computeExpiration(KeycloakSession session) {
        var realm = session.realms().getRealm(getValue().getRealmId());
        return new Expiration(
                SessionTimeouts.getLoginFailuresMaxIdleMs(realm, null, getValue()),
                SessionTimeouts.getLoginFailuresLifespanMs(realm, null, getValue()));
    }

    @Override
    public LoginFailureEntity apply(LoginFailureKey ignored, LoginFailureEntity cachedEntity) {
        assert !isDeleted();
        assert !isReadOnly();
        if (cachedEntity == null) {
            //entity removed
            return null;
        }
        changes.forEach(c -> c.accept(cachedEntity));
        return cachedEntity;
    }


    @Override
    public int getFailedLoginNotBefore() {
        return getValue().getFailedLoginNotBefore();
    }

    @Override
    public long getLastFailure() {
        return getValue().getLastFailure();
    }

    @Override
    public String getLastIPFailure() {
        return getValue().getLastIPFailure();
    }

    @Override
    public int getNumFailures() {
        return getValue().getNumFailures();
    }

    @Override
    public int getNumTemporaryLockouts() {
        return getValue().getNumTemporaryLockouts();
    }

    @Override
    public String getUserId() {
        return getValue().getUserId();
    }

    @Override
    public String getId() {
        return getKey().toString();
    }

    @Override
    public void clearFailures() {
        addAndApplyChange(CLEAR);
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        addAndApplyChange(e -> e.setFailedLoginNotBefore(notBefore));
    }

    @Override
    public void incrementFailures() {
        addAndApplyChange(INCREMENT_FAILURES);
    }

    @Override
    public void incrementTemporaryLockouts() {
        addAndApplyChange(INCREMENT_LOCK_OUTS);
    }

    @Override
    public void setLastFailure(long lastFailure) {
        addAndApplyChange(e -> e.setLastFailure(lastFailure));
    }

    @Override
    public void setLastIPFailure(String ip) {
        addAndApplyChange(e -> e.setLastIPFailure(ip));
    }

    private void addAndApplyChange(Consumer<LoginFailureEntity> change) {
        if (change == CLEAR) {
            changes.clear();
            changes.add(CLEAR);
        } else {
            changes.add(change);
        }
        change.accept(getValue());
        onFieldChanged();
    }

    private static final Consumer<LoginFailureEntity> CLEAR = LoginFailureEntity::clearFailures;
    private static final Consumer<LoginFailureEntity> INCREMENT_FAILURES = e -> e.setNumFailures(e.getNumFailures() + 1);
    private static final Consumer<LoginFailureEntity> INCREMENT_LOCK_OUTS = e -> e.setNumTemporaryLockouts(e.getNumTemporaryLockouts() + 1);
}
