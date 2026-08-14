/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events.hooks;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class EventHooksRealmResource {

    private final KeycloakSession session;
    private final RealmModel realm;

    public EventHooksRealmResource(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    @Path("{targetId}/{endpoint}")
    public Object getTargetEndpoint(@PathParam("targetId") String targetId, @PathParam("endpoint") String endpoint) {
        EventHookTargetModel target = getTargetOrThrow(targetId);
        EventHookTargetProviderFactory providerFactory = getTargetProviderFactory(target.getType());
        Object resource = providerFactory.getTargetEndpointResource(session, realm, target, endpoint);
        if (resource != null) {
            return resource;
        }

        throw new NotFoundException("Event hook target endpoint not found");
    }

    private EventHookTargetModel getTargetOrThrow(String targetId) {
        EventHookTargetModel target = session.getProvider(EventHookStoreProvider.class).getTarget(realm.getId(), targetId);
        if (target == null) {
            throw new NotFoundException("Event hook target not found");
        }
        return target;
    }

    private EventHookTargetProviderFactory getTargetProviderFactory(String type) {
        EventHookTargetProviderFactory providerFactory = (EventHookTargetProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(EventHookTargetProvider.class, type);
        if (providerFactory == null) {
            throw new NotFoundException("Event hook target endpoint not found");
        }
        return providerFactory;
    }
}
