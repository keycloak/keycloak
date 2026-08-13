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

import java.util.Objects;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.ExceptionConverter;

import org.jboss.logging.Logger;

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
    protected KeycloakSession session;
    private final RequestContextHelper requestContextHelper;

    public JtaTransactionWrapper(KeycloakSession session, TransactionManager tm) {
        this.tm = tm;
        this.session = session;
        this.requestContextHelper = RequestContextHelper.getContext(session);
        try {

            suspended = tm.suspend();

            tm.begin();
            ut = tm.getTransaction();

            if (logger.isDebugEnabled()) {
                String messageToLog = "new JtaTransactionWrapper. Was existing transaction suspended: " + (suspended != null);
                if (suspended != null) {
                    messageToLog = messageToLog + " Suspended transaction: " + suspended + ". ";
                }
                logMessage(messageToLog);
            }

            //ended = new Exception();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleException(Throwable e) {
        logger.debug(getDetailedMessage("Exception during transaction operation."), e);

        if (e instanceof RollbackException) {
            e = e.getCause() != null ? e.getCause() : e;
        }
        final Throwable finalE = e;

        session.getKeycloakSessionFactory().getProviderFactoriesStream(ExceptionConverter.class)
                .map(factory -> ((ExceptionConverter) factory).convert(finalE))
                .filter(Objects::nonNull)
                .forEach(throwable -> {
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException)throwable;
                    } else {
                        throw new RuntimeException(throwable);
                    }
                });

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
            logMessage("JtaTransactionWrapper  commit.");
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
            logMessage("JtaTransactionWrapper rollback.");
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
        logMessage("JtaTransactionWrapper end.");
        if (suspended != null) {
            try {
                logger.debugf("JtaTransactionWrapper resuming suspended user transaction: %s", suspended);
                tm.resume(suspended);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void logMessage(String messageBase) {
        if (logger.isTraceEnabled()) {
            String msg = getDetailedMessage(messageBase);

            // Log the detailed messages in "debug" level for backwards compatibility, but just if "Trace" level is enabled
            logger.debug(msg);
        } else if (logger.isDebugEnabled()) {
            logger.debug(messageBase + " Request Context: " + requestContextHelper.getContextInfo());
        }
    }

    private String getDetailedMessage(String messageBase) {
        String msg = messageBase + " Request context: " + requestContextHelper.getDetailedContextInfo();
        if (ut != null) {
            msg = msg + ", Transaction: " + ut;
        }
        return msg;
    }
}
