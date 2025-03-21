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

package org.keycloak.models.sessions.infinispan;

import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.sessions.infinispan.changes.LoginFailuresUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserLoginFailureAdapter implements UserLoginFailureModel {

    private InfinispanUserLoginFailureProvider provider;
    private LoginFailureKey key;
    private LoginFailureEntity entity;

    public UserLoginFailureAdapter(InfinispanUserLoginFailureProvider provider, LoginFailureKey key, LoginFailureEntity entity) {
        this.provider = provider;
        this.key = key;
        this.entity = entity;
    }

    @Override
    public String getUserId() {
        return entity.getUserId();
    }

    @Override
    public int getFailedLoginNotBefore() {
        return entity.getFailedLoginNotBefore();
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        LoginFailuresUpdateTask task = new LoginFailuresUpdateTask() {

            @Override
            public void runUpdate(LoginFailureEntity entity) {
                entity.setFailedLoginNotBefore(notBefore);
            }

        };

        update(task);
    }

    @Override
    public int getNumFailures() {
        return entity.getNumFailures();
    }

    @Override
    public void incrementFailures() {
        LoginFailuresUpdateTask task = new LoginFailuresUpdateTask() {

            @Override
            public void runUpdate(LoginFailureEntity entity) {
                entity.setNumFailures(entity.getNumFailures() + 1);
            }

        };

        update(task);
    }

    @Override
    public int getNumTemporaryLockouts() {
        return entity.getNumTemporaryLockouts();
    }

    @Override
    public void incrementTemporaryLockouts() {
        LoginFailuresUpdateTask task = new LoginFailuresUpdateTask() {

            @Override
            public void runUpdate(LoginFailureEntity entity) {
                entity.setNumTemporaryLockouts(entity.getNumTemporaryLockouts() + 1);
            }

        };

        update(task);
    }

    @Override
    public void clearFailures() {
        LoginFailuresUpdateTask task = new LoginFailuresUpdateTask() {

            @Override
            public void runUpdate(LoginFailureEntity entity) {
                entity.clearFailures();
            }

        };

        update(task);
    }

    @Override
    public long getLastFailure() {
        return entity.getLastFailure();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        LoginFailuresUpdateTask task = new LoginFailuresUpdateTask() {

            @Override
            public void runUpdate(LoginFailureEntity entity) {
                entity.setLastFailure(lastFailure);
            }

        };

        update(task);
    }

    @Override
    public String getLastIPFailure() {
        return entity.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String ip) {
        LoginFailuresUpdateTask task = new LoginFailuresUpdateTask() {

            @Override
            public void runUpdate(LoginFailureEntity entity) {
                entity.setLastIPFailure(ip);
            }

        };

        update(task);
    }

    void update(LoginFailuresUpdateTask task) {
        provider.getLoginFailuresTx().addTask(key, task);
    }

    @Override
    public String getId() {
        return key.toString();
    }

}
