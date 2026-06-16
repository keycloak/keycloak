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

package org.keycloak.quarkus.runtime.integration.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.inject.Inject;

import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler;

import io.quarkus.arc.Unremovable;
import org.jboss.logging.Logger;

@ApplicationScoped
@Unremovable
public class KeycloakBeanProducer implements TransactionalSessionHandler {
    
    private static final Logger logger = Logger.getLogger(KeycloakBeanProducer.class);

    @Inject
    QuarkusKeycloakSessionFactory factory;

    @RequestScoped
    public KeycloakSession getKeycloakSession() {
        // This is triggered lazily on the first method call on the session.
        // Do not start the transaction here as it could still be inside the event loop when used with a (prematching) filter.
        // JTA transactions must only be used in a blocking thread, so defer this until later.
        return factory.create();
    }

    void dispose(@Disposes KeycloakSession session) {
        if (!session.isClosed()) {
            logger.warn("Proactive closing of the session was missed - refinements are needed to TransactionSessionHandler related logic");
        }
        close(session);
    }
}
