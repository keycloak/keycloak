/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.requiredactions.util;

import java.util.List;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class RequiredActionsValidator {
    /**
     * Validate provided required actions
     *
     * @param session         the {@code KeycloakSession}
     * @param requiredActions IDs of tested required actions
     */
    public static boolean validRequiredActions(KeycloakSession session, List<String> requiredActions) {
        final KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        for (String action : requiredActions) {
            if (sessionFactory.getProviderFactory(RequiredActionProvider.class, action) == null) {
                return false;
            }
        }
        return true;
    }
}
