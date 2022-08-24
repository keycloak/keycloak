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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import java.util.Objects;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public abstract class AbstractUserSessionModel implements UserSessionModel {
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final MapUserSessionEntity entity;

    public AbstractUserSessionModel(KeycloakSession session, RealmModel realm, MapUserSessionEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(realm, "realm");

        this.session = session;
        this.realm = realm;
        this.entity = entity;
    }
}
