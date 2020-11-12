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
package org.keycloak.transaction;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.ExceptionConverter;
import org.keycloak.provider.ProviderFactory;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JtaTransactionWrapper implements KeycloakTransaction {
    private static final Logger logger = Logger.getLogger(JtaTransactionWrapper.class);
    protected TransactionManager tm;
    protected Transaction ut;
    protected Transaction suspended;
    protected Exception ended;
    protected KeycloakSessionFactory factory;

    public JtaTransactionWrapper(KeycloakSessionFactory factory, TransactionManager tm) {
        this.tm = tm;
        this.factory = factory;
        try {

            suspended = tm.suspend();
            logger.debug("new JtaTransactionWrapper");
            logger.debugv("was existing? {0}", suspended != null);
            tm.begin();
            ut = tm.getTransaction();
            //ended = new Exception();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleException(Throwable e) {
        if (e instanceof RollbackException) {
            e = e.getCause() != null ? e.getCause() : e;
        }

        for (ProviderFactory factory : this.factory.getProviderFactories(ExceptionConverter.class)) {
            ExceptionConverter converter = (ExceptionConverter)factory;
            Throwable throwable = converter.convert(e);
            if (throwable == null) continue;
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                throw new RuntimeException(throwable);
            }
        }

        if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
        } else {
            throw new RuntimeException(e);
        }



    }

    @Override
    public void begin() {
    }

    @Override
    public void commit() {
        try {
            logger.debug("JtaTransactionWrapper  commit");
            tm.commit();
        } catch (Exception e) {
            handleException(e);
        } finally {
            end();
        }
    }

    @Override
    public void rollback() {
        try {
            logger.debug("JtaTransactionWrapper rollback");
            tm.rollback();
        } catch (Exception e) {
            handleException(e);
        } finally {
            end();
        }

    }

    @Override
    public void setRollbackOnly() {
        try {
            tm.setRollbackOnly();
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    public boolean getRollbackOnly() {
        try {
            return tm.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }

    @Override
    public boolean isActive() {
        try {
            return tm.getStatus() == Status.STATUS_ACTIVE;
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }
    /*

    @Override
    protected void finalize() throws Throwable {
        if (ended != null) {
            logger.error("TX didn't close at position", ended);
        }

    }
    */

    protected void end() {
        ended = null;
        logger.debug("JtaTransactionWrapper end");
        if (suspended != null) {
            try {
                logger.debug("JtaTransactionWrapper resuming suspended");
                tm.resume(suspended);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
