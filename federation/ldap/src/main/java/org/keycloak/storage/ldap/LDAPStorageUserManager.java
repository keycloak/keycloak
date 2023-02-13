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

package org.keycloak.storage.ldap;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPTransaction;

/**
 * Track which LDAP users were already enlisted during this transaction
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPStorageUserManager {

    private final Map<String, ManagedUserEntry> managedUsers = new HashMap<>();
    private final LDAPStorageProvider provider;

    public LDAPStorageUserManager(LDAPStorageProvider provider) {
        this.provider = provider;
    }

    public UserModel getManagedProxiedUser(String userId) {
        ManagedUserEntry entry = managedUsers.get(userId);
        return entry==null ? null : entry.getManagedProxiedUser();
    }

    public LDAPObject getManagedLDAPUser(String userId) {
        ManagedUserEntry entry = managedUsers.get(userId);
        return entry==null ? null : entry.getLdapUser();
    }

    public LDAPTransaction getTransaction(String userId) {
        ManagedUserEntry entry = managedUsers.get(userId);
        if (entry == null) {
            throw new IllegalStateException("Shouldn't happen to not have entry for userId: " + userId);
        }

        return entry.getLdapTransaction();

    }

    public void setManagedProxiedUser(UserModel proxiedUser, LDAPObject ldapObject) {
        String userId = proxiedUser.getId();
        ManagedUserEntry entry = managedUsers.get(userId);
        if (entry != null) {
            throw new IllegalStateException("Don't expect to have entry for user " + userId);
        }

        LDAPTransaction ldapTransaction = new LDAPTransaction(provider, ldapObject);
        ManagedUserEntry newEntry = new ManagedUserEntry(proxiedUser, ldapObject, ldapTransaction);
        managedUsers.put(userId, newEntry);
    }

    public void removeManagedUserEntry(String userId) {
        managedUsers.remove(userId);
    }



    private static class ManagedUserEntry {

        private final UserModel managedProxiedUser;
        private final LDAPObject ldapUser;
        private final LDAPTransaction ldapTransaction;

        public ManagedUserEntry(UserModel managedProxiedUser, LDAPObject ldapUser, LDAPTransaction ldapTransaction) {
            this.managedProxiedUser = managedProxiedUser;
            this.ldapUser = ldapUser;
            this.ldapTransaction = ldapTransaction;
        }

        public UserModel getManagedProxiedUser() {
            return managedProxiedUser;
        }

        public LDAPObject getLdapUser() {
            return ldapUser;
        }

        public LDAPTransaction getLdapTransaction() {
            return ldapTransaction;
        }
    }
}
