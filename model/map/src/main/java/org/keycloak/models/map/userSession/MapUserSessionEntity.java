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
package org.keycloak.models.map.userSession;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.UUID;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionEntity extends AbstractUserSessionEntity<UUID> {
    protected MapUserSessionEntity() {
        super();
    }

    public MapUserSessionEntity(UUID id, String realmId) {
        super(id, realmId);
    }

    public MapUserSessionEntity(UUID id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId,
                                boolean offline) {
        super(id, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId, offline);
    }
}
