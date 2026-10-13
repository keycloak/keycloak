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
 * Event to trigger that will complete the import for a given realm representation.
 * <p />
 * This event was created as the import of a JSON via the UI/REST API can be called using a JSON representation that contains
 * only the name of the realm and if it is enabled.
 * <p />
 * In the future, this might not be needed if this is done when the legacy store migration is complete and the functionality
 * is bundled within the map storage.
 *
 * @author Alexander Schwartz
 */
@Deprecated
public class ImportRealmFromRepresentationEvent implements ProviderEvent {
    private final KeycloakSession session;
    private final RealmRepresentation realmRepresentation;

    private RealmModel realmModel;

    public ImportRealmFromRepresentationEvent(KeycloakSession session, RealmRepresentation realmRepresentation) {
        this.session = session;
        this.realmRepresentation = realmRepresentation;
    }

    public static RealmModel fire(KeycloakSession session, RealmRepresentation rep) {
        ImportRealmFromRepresentationEvent event = new ImportRealmFromRepresentationEvent(session, rep);
        session.getKeycloakSessionFactory().publish(event);
        return event.getRealmModel();
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmRepresentation getRealmRepresentation() {
        return realmRepresentation;
    }

    public void setRealmModel(RealmModel realmModel) {
        this.realmModel = realmModel;
    }

    public RealmModel getRealmModel() {
        return realmModel;
    }
}
