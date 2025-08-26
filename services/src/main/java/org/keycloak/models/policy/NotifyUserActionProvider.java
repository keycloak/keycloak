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

package org.keycloak.models.policy;

import java.util.List;
import org.jboss.logging.Logger;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class NotifyUserActionProvider implements ResourceActionProvider {

    private final KeycloakSession session;
    private final ComponentModel actionModel;
    private final Logger log = Logger.getLogger(NotifyUserActionProvider.class);

    public NotifyUserActionProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.actionModel = model;
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
                log.debugv("Disabling user {0} ({1})", user.getUsername(), user.getId());
                user.setSingleAttribute(getMessageKey(), getMessage());
            }
        }
    }

    private String getMessageKey() {
        return actionModel.getConfig().getFirstOrDefault("message_key", "message");
    }

    private String getMessage() {
        return actionModel.getConfig().getFirstOrDefault(getMessageKey(), "sent");
    }

    @Override
    public boolean isRunnable() {
        return actionModel.get("after") != null;
    }
}
