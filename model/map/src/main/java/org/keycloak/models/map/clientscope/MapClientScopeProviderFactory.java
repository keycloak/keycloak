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
package org.keycloak.models.map.clientscope;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.InvalidationHandler;

import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.CLIENT_SCOPE_AFTER_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.CLIENT_SCOPE_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_BEFORE_REMOVE;

public class MapClientScopeProviderFactory extends AbstractMapProviderFactory<MapClientScopeProvider, MapClientScopeEntity, ClientScopeModel> implements ClientScopeProviderFactory<MapClientScopeProvider>, InvalidationHandler {

    public MapClientScopeProviderFactory() {
        super(ClientScopeModel.class, MapClientScopeProvider.class);
    }

    @Override
    public MapClientScopeProvider createNew(KeycloakSession session) {
        return new MapClientScopeProvider(session, getStorage(session));
    }

    @Override
    public String getHelpText() {
        return "Client scope provider";
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        if (type == REALM_BEFORE_REMOVE) {
            create(session).preRemove((RealmModel) params[0]);
        } else if (type == CLIENT_SCOPE_BEFORE_REMOVE) {
            ((RealmModel) params[0]).removeDefaultClientScope((ClientScopeModel) params[1]);
        } else if (type == CLIENT_SCOPE_AFTER_REMOVE) {
            session.getKeycloakSessionFactory().publish(new ClientScopeModel.ClientScopeRemovedEvent() {
                @Override public ClientScopeModel getClientScope() { return (ClientScopeModel) params[0]; }
                @Override public KeycloakSession getKeycloakSession() { return session; }
            });
        }
    }
}
