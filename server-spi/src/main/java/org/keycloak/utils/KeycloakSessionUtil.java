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

package org.keycloak.utils;

import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class KeycloakSessionUtil {

    private static final String NO_REALM = "no_realm_found_in_session";

    private KeycloakSessionUtil() {

    }

    /**
     * Get the {@link KeycloakSession} currently associated with the thread.
     *
     * @return the current session
     */
    public static KeycloakSession getKeycloakSession() {
        return Resteasy.getContextData(KeycloakSession.class);
    }

    /**
     * Associate the {@link KeycloakSession} with the current thread.
     * <br>Warning: should not be called directly. Keycloak will manage this.
     *
     * @param session
     * @return the existing {@link KeycloakSession} or null
     */
    public static KeycloakSession setKeycloakSession(KeycloakSession session) {
        return Resteasy.pushContext(KeycloakSession.class, session);
    }

    public static String getRealmNameFromContext(KeycloakSession session) {
        if(session == null) {
            return NO_REALM;
        }

        KeycloakContext context = session.getContext();
        if(context == null) {
            return NO_REALM;
        }

        RealmModel realm = context.getRealm();
        if (realm == null) {
            return NO_REALM;
        }

        if(realm.getName() != null) {
            return realm.getName();
        } else {
            return NO_REALM;
        }
    }

}
