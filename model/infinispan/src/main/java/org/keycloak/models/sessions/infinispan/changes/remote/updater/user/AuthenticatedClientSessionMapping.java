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

package org.keycloak.models.sessions.infinispan.changes.remote.updater.user;

import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;

/**
 * It gives a read-only view of the {@link AuthenticatedClientSessionModel} belonging to a
 * {@link org.keycloak.models.UserSessionModel} though the {@link Map} interface where the key is the Client ID.
 * <p>
 * Any direct modification via the {@link Map} interface will throw an {@link UnsupportedOperationException}. To add a
 * new mapping, use a method like {@link UserSessionProvider#createClientSession(RealmModel, ClientModel, UserSessionModel)} or
 * equivalent. To remove a mapping, use {@link AuthenticatedClientSessionModel#detachFromUserSession()}.
 */
public interface AuthenticatedClientSessionMapping extends Map<String, AuthenticatedClientSessionModel> {

    /**
     * Notifies the associated {@link UserSessionModel} has been restarted.
     * <p>
     * All the {@link AuthenticatedClientSessionModel} must be detached.
     */
    void onUserSessionRestart();

}
