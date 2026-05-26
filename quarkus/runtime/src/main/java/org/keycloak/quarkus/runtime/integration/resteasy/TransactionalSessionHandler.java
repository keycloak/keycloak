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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestMulti;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.handlers.InvocationHandler;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

public final class TransactionalSessionHandler extends InvocationHandler implements org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler {
    
    public static final Class<?>[] ASYNC_TYPES = new Class<?>[] {
        CompletionStage.class,
        CompletableFuture.class,
        Uni.class,
        Multi.class,
        RestMulti.class,
        Publisher.class,
        org.reactivestreams.Publisher.class,
        RestResponse.class
    }; 
    
    public TransactionalSessionHandler(EndpointInvoker invoker) {
        super(invoker);
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
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
        }

        super.handle(requestContext);
        
        // ensure the initial transaction is completed, as the thread will be used for other work
        if ((requestContext.getAsyncResponse() != null || Stream.of(ASYNC_TYPES)
                .anyMatch(requestContext.getResteasyReactiveResourceInfo().getMethod().getReturnType()::equals))) {
            KeycloakSession currentSession = ClientProxy.unwrap(Arc.container().instance(KeycloakSession.class).get());
            KeycloakTransactionManager transactionManager = currentSession.getTransactionManager();
            if (transactionManager.isActive()) { 
                transactionManager.commit();
            }
        }
    }
}
