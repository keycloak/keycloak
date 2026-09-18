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
package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

public class ClientNodeRegistrationContext implements ClientModelContext {

    private final ClientModel client;
    private final String nodeHost;
    private final ClientPolicyEvent event;

    public ClientNodeRegistrationContext(ClientModel client,
                                         String nodeHost,
                                         ClientPolicyEvent event) {
        this.client = client;
        this.nodeHost = nodeHost;
        this.event = event;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return event;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    public String getNodeHost() {
        return nodeHost;
    }
}
