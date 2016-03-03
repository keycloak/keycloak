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

package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionPredicate implements Predicate<Map.Entry<String, SessionEntity>>, Serializable {

    private String realm;

    private String client;

    private String userSession;

    private Long expiredRefresh;

    private Boolean requireUserSession = false;

    private Boolean requireNullUserSession = false;

    private ClientSessionPredicate(String realm) {
        this.realm = realm;
    }

    public static ClientSessionPredicate create(String realm) {
        return new ClientSessionPredicate(realm);
    }

    public ClientSessionPredicate client(String client) {
        this.client = client;
        return this;
    }

    public ClientSessionPredicate userSession(String userSession) {
        this.userSession = userSession;
        return this;
    }

    public ClientSessionPredicate expiredRefresh(long expiredRefresh) {
        this.expiredRefresh = expiredRefresh;
        return this;
    }

    public ClientSessionPredicate requireUserSession() {
        requireUserSession = true;
        return this;
    }

    public ClientSessionPredicate requireNullUserSession() {
        requireNullUserSession = true;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntity> entry) {
        SessionEntity e = entry.getValue();

        if (!realm.equals(e.getRealm())) {
            return false;
        }

        if (!(e instanceof ClientSessionEntity)) {
            return false;
        }

        ClientSessionEntity entity = (ClientSessionEntity) e;

        if (client != null && !entity.getClient().equals(client)) {
            return false;
        }

        if (userSession != null && !userSession.equals(entity.getUserSession())) {
            return false;
        }

        if (requireUserSession && entity.getUserSession() == null) {
            return false;
        }

        if (requireNullUserSession && entity.getUserSession() != null) {
            return false;
        }

        if (expiredRefresh != null && entity.getTimestamp() > expiredRefresh) {
            return false;
        }

        return true;
    }

}
