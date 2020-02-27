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

import java.util.HashSet;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPTransaction extends AbstractKeycloakTransaction {

    public static final Logger logger = Logger.getLogger(LDAPTransaction.class);

    private final LDAPStorageProvider ldapProvider;
    private final LDAPObject ldapUser;

    // Tracks the attributes updated in this transaction
    private final Set<String> updatedAttributes = new HashSet<>();

    public LDAPTransaction(LDAPStorageProvider ldapProvider, LDAPObject ldapUser) {
        this.ldapProvider = ldapProvider;
        this.ldapUser = ldapUser;
    }


    @Override
    protected void commitImpl() {
        if (logger.isTraceEnabled()) {
            logger.trace("Transaction commit! Updating LDAP attributes for object " + ldapUser.getDn().toString() + ", attributes: " + ldapUser.getAttributes());
        }

        ldapProvider.getLdapIdentityStore().update(ldapUser);
    }


    @Override
    protected void rollbackImpl() {
        logger.warn("Transaction rollback! Ignoring LDAP updates for object " + ldapUser.getDn().toString());
    }

    /**
     * Add attribute, which will be updated in LDAP in this transaction
     *
     * @param attributeName model attribute name (For example "firstName", "lastName", "street")
     */
    public void addUpdatedAttribute(String attributeName) {
        updatedAttributes.add(attributeName);
    }

    /**
     * @param attributeName model attribute name (For example "firstName", "lastName", "street")
     * @return true if attribute was updated in this transaction
     */
    public boolean isAttributeUpdated(String attributeName) {
        return updatedAttributes.contains(attributeName);
    }

    /**
     * Add required action, which will be updated in LDAP in this transaction
     *
     * @param requiredActionName
     */
    public void addUpdatedRequiredAction(String requiredActionName) {
        updatedAttributes.add("requiredAction(" + requiredActionName + ")");
    }

    /**
     *
     * @param requiredActionName
     * @return true if requiredAction was updated in this transaction
     */
    public boolean isRequiredActionUpdated(String requiredActionName) {
        return updatedAttributes.contains("requiredAction(" + requiredActionName + ")");
    }

}

