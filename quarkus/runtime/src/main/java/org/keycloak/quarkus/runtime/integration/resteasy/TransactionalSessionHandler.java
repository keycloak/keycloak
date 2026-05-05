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

package org.keycloak.quarkus.runtime.integration.resteasy;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.utils.KeycloakSessionUtil;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import org.jboss.resteasy.reactive.common.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public final class TransactionalSessionHandler implements ServerRestHandler, org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        // This method might be invoked multiple times within a request when resolving sub-resources.

        requestContext.requireCDIRequestScope();

        if (BlockingOperationSupport.isBlockingAllowed()) {
            // ClientProxy.unwrap() resolves a proxy that is lazily initialized on the first method call or on unwrap.
            KeycloakSession currentSession = ClientProxy.unwrap(Arc.container().instance(KeycloakSession.class).get());
            KeycloakTransactionManager transactionManager = currentSession.getTransactionManager();
            if (!transactionManager.isActive()) {
                // This handler is always running in a blocking thread.
                beginTransaction(currentSession);
            }
            KeycloakSessionUtil.setKeycloakSession(currentSession);
        }
    }
}
