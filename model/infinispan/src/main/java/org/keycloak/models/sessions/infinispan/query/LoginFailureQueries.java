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

package org.keycloak.models.sessions.infinispan.query;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.query.Query;

/**
 * Util class with Infinispan Ickle Queries for {@link LoginFailureEntity}.
 */
public final class LoginFailureQueries {

    private LoginFailureQueries() {
    }

    public static final String LOGIN_FAILURE = Marshalling.protoEntity(LoginFailureEntity.class);

    private static final String BASE_QUERY = "FROM %s as e ".formatted(LOGIN_FAILURE);
    private static final String BY_REALM_ID = BASE_QUERY + "WHERE e.realmId = :realmId";

    /**
     * Returns a projection with the login failure session.
     */
    public static Query<LoginFailureEntity> searchByRealmId(RemoteCache<LoginFailureKey, LoginFailureEntity> cache, String realmId) {
        return cache.<LoginFailureEntity>query(BY_REALM_ID)
                .setParameter("realmId", realmId);
    }
}
