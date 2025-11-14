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

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageUtil;

import org.jboss.logging.Logger;

public class DeleteUserStepProvider implements WorkflowStepProvider {

    public static final String PROPAGATE_TO_SP = "propagate-to-provider";

    private final KeycloakSession session;
    private final ComponentModel stepModel;
    private final Logger log = Logger.getLogger(DeleteUserStepProvider.class);

    public DeleteUserStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());

        if (user == null) {
            return;
        }

        if (!user.isFederated() || stepModel.get(PROPAGATE_TO_SP, false)) {
          log.debugv("Deleting user {0} ({1})", user.getUsername(), user.getId());
          session.users().removeUser(realm, user);
          return;
        }

        // delete the local user only
        UserStoragePrivateUtil.userLocalStorage(session).removeUser(realm, user);
        log.debugv("Deleting federated user {0} ({1}) from local storage only", user.getUsername(), user.getId());
        UserCache userCache = UserStorageUtil.userCache(session);
        // if cache is enabled, evict the user from cache
        if (userCache != null) {
            userCache.evict(realm, user);
        }
    }
}
