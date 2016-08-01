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

package org.keycloak.federation.ldap.mappers;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class TxAwareLDAPUserModelDelegate extends UserModelDelegate {

    public static final Logger logger = Logger.getLogger(TxAwareLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;
    protected LDAPObject ldapUser;
    private final LDAPTransaction transaction;

    public TxAwareLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider, LDAPObject ldapUser) {
        super(delegate);
        this.provider = provider;
        this.ldapUser = ldapUser;
        this.transaction = findOrCreateTransaction();
    }

    public LDAPTransaction getTransaction() {
        return transaction;
    }

    // Try to find transaction in any delegate. We want to enlist just single transaction per all delegates
    private LDAPTransaction findOrCreateTransaction() {
        UserModelDelegate delegate = this;
        while (true) {
            UserModel deleg = delegate.getDelegate();
            if (!(deleg instanceof UserModelDelegate)) {
                return new LDAPTransaction();
            } else {
                delegate = (UserModelDelegate) deleg;
            }

            if (delegate instanceof TxAwareLDAPUserModelDelegate) {
                TxAwareLDAPUserModelDelegate txDelegate = (TxAwareLDAPUserModelDelegate) delegate;
                return txDelegate.getTransaction();
            }
        }
    }

    protected void ensureTransactionStarted() {
        if (transaction.state == TransactionState.NOT_STARTED) {
            if (logger.isTraceEnabled()) {
                logger.trace("Starting and enlisting transaction for object " + ldapUser.getDn().toString());
            }

            this.provider.getSession().getTransactionManager().enlistAfterCompletion(transaction);
        }
    }



    protected class LDAPTransaction implements KeycloakTransaction {

        protected TransactionState state = TransactionState.NOT_STARTED;

        @Override
        public void begin() {
            if (state != TransactionState.NOT_STARTED) {
                throw new IllegalStateException("Transaction already started");
            }

            state = TransactionState.STARTED;
        }

        @Override
        public void commit() {
            if (state != TransactionState.STARTED) {
                throw new IllegalStateException("Transaction in illegal state for commit: " + state);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Transaction commit! Updating LDAP attributes for object " + ldapUser.getDn().toString() + ", attributes: " + ldapUser.getAttributes());
            }

            provider.getLdapIdentityStore().update(ldapUser);
            state = TransactionState.FINISHED;
        }

        @Override
        public void rollback() {
            if (state != TransactionState.STARTED && state != TransactionState.ROLLBACK_ONLY) {
                throw new IllegalStateException("Transaction in illegal state for rollback: " + state);
            }

            logger.warn("Transaction rollback! Ignoring LDAP updates for object " + ldapUser.getDn().toString());
            state = TransactionState.FINISHED;
        }

        @Override
        public void setRollbackOnly() {
            state = TransactionState.ROLLBACK_ONLY;
        }

        @Override
        public boolean getRollbackOnly() {
            return state == TransactionState.ROLLBACK_ONLY;
        }

        @Override
        public boolean isActive() {
            return state == TransactionState.STARTED || state == TransactionState.ROLLBACK_ONLY;
        }
    }

    protected enum TransactionState {
        NOT_STARTED, STARTED, ROLLBACK_ONLY, FINISHED
    }

}
