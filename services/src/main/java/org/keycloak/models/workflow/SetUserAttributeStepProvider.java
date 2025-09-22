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

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_AFTER;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_PRIORITY;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map.Entry;

public class SetUserAttributeStepProvider implements WorkflowStepProvider {

    private final KeycloakSession session;
    private final ComponentModel stepModel;
    private final Logger log = Logger.getLogger(SetUserAttributeStepProvider.class);

    public SetUserAttributeStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(List<String> userIds) {
        RealmModel realm = session.getContext().getRealm();

        for (String id : userIds) {
            UserModel user = session.users().getUserById(realm, id);

            if (user != null) {
                for (Entry<String, List<String>> entry : stepModel.getConfig().entrySet()) {
                    String key = entry.getKey();

                    if (!key.startsWith(CONFIG_AFTER) && !key.startsWith(CONFIG_PRIORITY)) {
                        log.debugv("Setting attribute {0} to user {1})", key, user.getId());
                        user.setAttribute(key, entry.getValue());
                    }
                }
            }
        }
    }
}
