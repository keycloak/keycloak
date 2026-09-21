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

package org.keycloak.tests.client.policies;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;

public class SetProtocolMapperProvider implements RunOnServer {

    private final String clientId;
    private final String mapperId;
    private final String providerId;

    public SetProtocolMapperProvider(String clientId, String mapperId, String providerId) {
        this.clientId = clientId;
        this.mapperId = mapperId;
        this.providerId = providerId;
    }

    @Override
    public void run(KeycloakSession session) {
        ClientModel client = session.getContext().getRealm().getClientById(clientId);
        ProtocolMapperModel mapper = client.getProtocolMapperById(mapperId);
        mapper.setProtocolMapper(providerId);
        client.updateProtocolMapper(mapper);
    }
}
