/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.mongo;

import com.mongodb.MongoException;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.models.KeycloakTransaction;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakTransaction implements KeycloakTransaction {

    private final MongoStoreInvocationContext invocationContext;

    private boolean started = false;
    private boolean rollbackOnly = false;

    public MongoKeycloakTransaction(MongoStoreInvocationContext invocationContext) {
        this.invocationContext = invocationContext;
    }

    @Override
    public void begin() {
        if (started) {
            throw new IllegalStateException("Transaction already started");
        }
        started = true;
        invocationContext.begin();
    }

    @Override
    public void commit() {
        if (!started) {
            throw new IllegalStateException("Transaction not yet started");
        }
        if (rollbackOnly) {
            throw new IllegalStateException("Can't commit as transaction marked for rollback");
        }

        try {
            invocationContext.commit();
        } catch (MongoException e) {
            throw MongoStoreImpl.convertException(e);
        }
        started = false;
    }

    @Override
    public void rollback() {
        invocationContext.rollback();
        started = false;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public boolean isActive() {
        return started;
    }
}
