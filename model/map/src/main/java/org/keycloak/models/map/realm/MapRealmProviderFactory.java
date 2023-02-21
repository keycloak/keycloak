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
package org.keycloak.models.map.realm;

import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProviderFactory;
import org.keycloak.provider.InvalidationHandler;

import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_AFTER_REMOVE;

public class MapRealmProviderFactory extends AbstractMapProviderFactory<MapRealmProvider, MapRealmEntity, RealmModel> implements RealmProviderFactory<MapRealmProvider>, InvalidationHandler {

    public MapRealmProviderFactory() {
        super(RealmModel.class, MapRealmProvider.class);
    }

    @Override
    public MapRealmProvider createNew(KeycloakSession session) {
        return new MapRealmProvider(session, getStorage(session));
    }

    @Override
    public String getHelpText() {
        return "Realm provider";
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        if (type == REALM_AFTER_REMOVE) {
            session.getKeycloakSessionFactory().publish(new RealmModel.RealmRemovedEvent() {
                @Override public RealmModel getRealm() { return (RealmModel) params[0]; }
                @Override public KeycloakSession getKeycloakSession() { return session; }
            });
        }
    }
}
