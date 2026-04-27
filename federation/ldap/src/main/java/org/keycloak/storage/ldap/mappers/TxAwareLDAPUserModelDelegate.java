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

package org.keycloak.storage.ldap.mappers;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class TxAwareLDAPUserModelDelegate extends UserModelDelegate {

    public static final Logger logger = Logger.getLogger(TxAwareLDAPUserModelDelegate.class);

    protected LDAPStorageProvider provider;
    protected LDAPObject ldapUser;

    public TxAwareLDAPUserModelDelegate(UserModel delegate, LDAPStorageProvider provider, LDAPObject ldapUser) {
        super(delegate);
        this.provider = provider;
        this.ldapUser = ldapUser;
    }

    protected void ensureTransactionStarted() {
        LDAPTransaction transaction = provider.getUserManager().getTransaction(getId());
        if (transaction.getState() == LDAPTransaction.TransactionState.NOT_STARTED) {
            if (logger.isTraceEnabled()) {
                logger.trace("Starting and enlisting transaction for object " + ldapUser.getDn());
            }

            this.provider.getSession().getTransactionManager().enlistAfterCompletion(transaction);
        }
    }

    protected void markUpdatedAttributeInTransaction(String modelAttributeName) {
        ensureTransactionStarted();
        LDAPTransaction transaction = provider.getUserManager().getTransaction(getId());
        transaction.addUpdatedAttribute(modelAttributeName);
    }

    protected void markUpdatedRequiredActionInTransaction(String requiredActionName) {
        ensureTransactionStarted();
        LDAPTransaction transaction = provider.getUserManager().getTransaction(getId());
        transaction.addUpdatedRequiredAction(requiredActionName);
    }

}
