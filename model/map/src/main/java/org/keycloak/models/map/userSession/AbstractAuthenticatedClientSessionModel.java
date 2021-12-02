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

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;

import java.util.Objects;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public abstract class AbstractAuthenticatedClientSessionModel implements AuthenticatedClientSessionModel {
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected ClientModel client;
    protected UserSessionModel userSession;
    protected final MapAuthenticatedClientSessionEntity entity;

    public AbstractAuthenticatedClientSessionModel(KeycloakSession session, RealmModel realm, ClientModel client,
                                                   UserSessionModel userSession, MapAuthenticatedClientSessionEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(realm, "realm");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(userSession, "userSession");

        this.session = session;
        this.realm = realm;
        this.client = client;
        this.userSession = userSession;
        this.entity = entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticatedClientSessionModel)) return false;

        AuthenticatedClientSessionModel that = (AuthenticatedClientSessionModel) o;
        return Objects.equals(that.getId(), getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
