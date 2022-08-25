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
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Event for notifying legacy store, so it can do migrations on the representation as needed.
 *
 * CAUTION: This event is exceptional as it performs any necessary modificaton of the representation.
 * This will be removed once the legacy store has been removed.
 */
public class LegacyStoreMigrateRepresentationEvent implements ProviderEvent {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final RealmRepresentation rep;
    private final boolean skipUserDependent;

    public LegacyStoreMigrateRepresentationEvent(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        this.session = session;
        this.realm = realm;
        this.rep = rep;
        this.skipUserDependent = skipUserDependent;
    }

    public static void fire(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        session.getKeycloakSessionFactory().publish(new LegacyStoreMigrateRepresentationEvent(session, realm, rep, skipUserDependent));
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public RealmRepresentation getRep() {
        return rep;
    }

    public boolean isSkipUserDependent() {
        return skipUserDependent;
    }
}