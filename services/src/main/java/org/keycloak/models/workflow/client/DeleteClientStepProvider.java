/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.workflow.client;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowStepProvider;

import org.jboss.logging.Logger;

public class DeleteClientStepProvider implements WorkflowStepProvider {

    private final KeycloakSession session;
    private final Logger log = Logger.getLogger(DeleteClientStepProvider.class);

    public DeleteClientStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.clients().getClientById(realm, context.getResourceId());

        if (client == null) {
            return;
        }

        log.debugv("Deleting client {0} ({1})", client.getName(), client.getId());
        session.clients().removeClient(realm, client.getId());
    }
}
