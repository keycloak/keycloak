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

import org.infinispan.Cache;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserLoginFailureAdapter implements UserLoginFailureModel {

    private InfinispanUserSessionProvider provider;
    private Cache<LoginFailureKey, LoginFailureEntity> cache;
    private LoginFailureKey key;
    private LoginFailureEntity entity;

    public UserLoginFailureAdapter(InfinispanUserSessionProvider provider, Cache<LoginFailureKey, LoginFailureEntity> cache, LoginFailureKey key, LoginFailureEntity entity) {
        this.provider = provider;
        this.cache = cache;
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
        entity.setFailedLoginNotBefore(notBefore);
        update();
    }

    @Override
    public int getNumFailures() {
        return entity.getNumFailures();
    }

    @Override
    public void incrementFailures() {
        entity.setNumFailures(getNumFailures() + 1);
        update();
    }

    @Override
    public void clearFailures() {
        entity.clearFailures();
        update();
    }

    @Override
    public long getLastFailure() {
        return entity.getLastFailure();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        entity.setLastFailure(lastFailure);
        update();
    }

    @Override
    public String getLastIPFailure() {
        return entity.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String ip) {
        entity.setLastIPFailure(ip);
        update();
    }

    void update() {
        provider.getTx().replace(cache, key, entity);
    }

}
