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
package org.keycloak.models.map.storage.jpa;

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakTransaction;

/**
 * Wraps an {@link EntityTransaction} as a {@link KeycloakTransaction} so it can be enlisted in {@link org.keycloak.models.KeycloakTransactionManager}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaTransactionWrapper implements KeycloakTransaction {

    private static final Logger logger = Logger.getLogger(JpaTransactionWrapper.class);

    private final EntityTransaction transaction;

    public JpaTransactionWrapper(EntityTransaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void begin() {
        logger.tracef("tx %d: begin", hashCode());
        this.transaction.begin();
    }

    @Override
    public void commit() {
        try {
            logger.tracef("tx %d: commit", hashCode());
            this.transaction.commit();
        } catch(PersistenceException pe) {
            throw PersistenceExceptionConverter.convert(pe.getCause() != null ? pe.getCause() : pe);
        }
    }

    @Override
    public void rollback() {
        logger.tracef("tx %d: rollback", hashCode());
        this.transaction.rollback();
    }

    @Override
    public void setRollbackOnly() {
        this.transaction.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return this.transaction.getRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return this.transaction.isActive();
    }
}
