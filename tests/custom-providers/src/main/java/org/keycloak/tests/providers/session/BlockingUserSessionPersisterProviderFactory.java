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

package org.keycloak.tests.providers.session;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.session.JpaUserSessionPersisterProviderFactory;
import org.keycloak.models.session.UserSessionPersisterProvider;

public class BlockingUserSessionPersisterProviderFactory extends JpaUserSessionPersisterProviderFactory {

    public static final String ID = "blocking-jpa";

    @Override
    public UserSessionPersisterProvider create(KeycloakSession session) {
        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new BlockingUserSessionPersisterProvider(session, entityManager, DEFAULT_EXPIRATION_BATCH);
    }

    @Override
    public String getId() {
        return ID;
    }
}
