/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;

/**
 * Event for notifying the store about the need to reconfigure user providers
 * synchronization.
 */
public class StoreSyncEvent implements ProviderEvent {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final boolean removed;

    public StoreSyncEvent(KeycloakSession session, RealmModel realm, boolean removed) {
        this.session = session;
        this.realm = realm;
        this.removed = removed;
    }

    public static void fire(KeycloakSession session, RealmModel realm, boolean removed) {
        session.getKeycloakSessionFactory().publish(new StoreSyncEvent(session, realm, removed));
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public boolean getRemoved() {
        return removed;
    }

}