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

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Consumers {

    private Consumers() {
    }

    public static UserSessionModelsConsumer userSessionModels(InfinispanUserSessionProvider provider, RealmModel realm, boolean offline) {
        return new UserSessionModelsConsumer(provider, realm, offline);
    }

    public static class UserSessionModelsConsumer implements Consumer<Map.Entry<String, SessionEntity>> {

        private InfinispanUserSessionProvider provider;
        private RealmModel realm;
        private boolean offline;
        private List<UserSessionModel> sessions = new LinkedList<>();

        private UserSessionModelsConsumer(InfinispanUserSessionProvider provider, RealmModel realm, boolean offline) {
            this.provider = provider;
            this.realm = realm;
            this.offline = offline;
        }

        @Override
        public void accept(Map.Entry<String, SessionEntity> entry) {
            SessionEntity e = entry.getValue();
            sessions.add(provider.wrap(realm, (UserSessionEntity) e, offline));
        }

        public List<UserSessionModel> getSessions() {
            return sessions;
        }

    }
}
