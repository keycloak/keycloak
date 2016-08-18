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
import org.keycloak.models.ClientRegistrationTrustedHostModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.ClientRegistrationTrustedHostEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationTrustedHostAdapter implements ClientRegistrationTrustedHostModel {

    private final KeycloakSession session;
    private final InfinispanUserSessionProvider provider;
    private final Cache<String, SessionEntity> cache;
    private final RealmModel realm;
    private final ClientRegistrationTrustedHostEntity entity;

    public ClientRegistrationTrustedHostAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, Cache<String, SessionEntity> cache, RealmModel realm, ClientRegistrationTrustedHostEntity entity) {
        this.session = session;
        this.provider = provider;
        this.cache = cache;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getHostName() {
        return entity.getHostName();
    }

    @Override
    public int getCount() {
        return entity.getCount();
    }

    @Override
    public void setCount(int count) {
        entity.setCount(count);
        update();
    }

    @Override
    public int getRemainingCount() {
        return entity.getRemainingCount();
    }

    @Override
    public void setRemainingCount(int remainingCount) {
        entity.setRemainingCount(remainingCount);
        update();
    }

    @Override
    public void decreaseRemainingCount() {
        entity.setRemainingCount(entity.getRemainingCount() - 1);
        update();
    }

    void update() {
        provider.getTx().replace(cache, entity.getId(), entity);
    }
}
