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

package org.keycloak.models.workflow;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;

public class DeleteUserStepProvider implements WorkflowStepProvider {

    private final KeycloakSession session;
    private final Logger log = Logger.getLogger(DeleteUserStepProvider.class);

    public DeleteUserStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(List<String> ids) {
        RealmModel realm = session.getContext().getRealm();

        for (String id : ids) {
            UserModel user = session.users().getUserById(realm, id);

            if (user == null) {
                continue;
            }

            log.debugv("Deleting user {0} ({1})", user.getUsername(), user.getId());
            session.users().removeUser(realm, user);
        }
    }
}
