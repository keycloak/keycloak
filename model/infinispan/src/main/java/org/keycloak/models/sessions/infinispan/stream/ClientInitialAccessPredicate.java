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

import org.keycloak.models.sessions.infinispan.entities.ClientInitialAccessEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessPredicate implements Predicate<Map.Entry<String, SessionEntity>>, Serializable {

    public ClientInitialAccessPredicate(String realm) {
        this.realm = realm;
    }

    private String realm;

    private Integer expired;

    public static ClientInitialAccessPredicate create(String realm) {
        return new ClientInitialAccessPredicate(realm);
    }

    public ClientInitialAccessPredicate expired(int time) {
        this.expired = time;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntity> entry) {
        SessionEntity e = entry.getValue();

        if (!realm.equals(e.getRealm())) {
            return false;
        }

        if (!(e instanceof ClientInitialAccessEntity)) {
            return false;
        }

        ClientInitialAccessEntity entity = (ClientInitialAccessEntity) e;

        if (expired != null) {
            if (entity.getRemainingCount() <= 0) {
                return true;
            } else if (entity.getExpiration() > 0 && (entity.getTimestamp() + entity.getExpiration()) < expired) {
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

}
