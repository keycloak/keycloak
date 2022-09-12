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

package org.keycloak.services.managers;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.ImportRealmFromRepresentation;

/**
 * Provider to listen for {@link org.keycloak.storage.ImportRealmFromRepresentation} events.
 * If that is no longer needed after further steps around the legacy storage migration, it can be removed.
 *
 * @author Alexander Schwartz
 */
@Deprecated
public class RealmManagerProviderFactory implements ProviderFactory<RealmManagerProviderFactory>, Provider {
    @Override
    public RealmManagerProviderFactory create(KeycloakSession session) {
        throw new ModelException("This shouldn't be instantiated, this should only listen to events");
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof ImportRealmFromRepresentation) {
                ImportRealmFromRepresentation importRealmFromRepresentation = (ImportRealmFromRepresentation) event;
                RealmModel realmModel = new RealmManager(importRealmFromRepresentation.getSession()).importRealm(importRealmFromRepresentation.getRealmRepresentation());
                importRealmFromRepresentation.setRealmModel(realmModel);
            }
        });
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "default";
    }
}
