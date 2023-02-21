/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.util;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;

import java.util.function.Function;

public class TransactionController {
    private final KeycloakSession session;

    public TransactionController(KeycloakSessionFactory sessionFactory) {
        session = sessionFactory.create();
    }

    public void begin() {
        getTransactionManager().begin();
    }

    public void commit() {
        getTransactionManager().commit();
    }

    public void rollback() {
        getTransactionManager().rollback();
    }

    public <R> R runStep(Function<KeycloakSession, R> task) {
        return task.apply(session);
    }

    private KeycloakTransactionManager getTransactionManager() {
        return session.getTransactionManager();
    }
}
