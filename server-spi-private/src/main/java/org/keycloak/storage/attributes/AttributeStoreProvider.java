/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes;

import jakarta.ws.rs.ProcessingException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Provider for retrieving attributes from external data stores. Data from these stores can be synced using {@link AttributeFederationProvider}
 * added to tokens during generation with the {@link AttributeStoreMapper}.
 */
public interface AttributeStoreProvider extends Provider {
    /**
     * Fetches attributes from an external data store for the provided user in the given realm. This function must return
     * a JSON-like map.
     *
     * @param realm The realm the request is taking place in
     * @param user The user to fetch attributes from the external data store for
     * @return A JSON-like map
     * @throws ProcessingException Thrown if attributes cannot be fetched from the external data store
     */
    Map<String, Object> getAttributes(KeycloakSession session, RealmModel realm, UserModel user) throws ProcessingException;
}
