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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPTransaction;

/**
 * Track which LDAP users were already enlisted during this transaction
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPStorageUserManager {

    private final Map<String, LDAPObject> managedLDAPObjects = new HashMap<>();
    private final Map<String, ManagedUserEntry> managedUsers = new HashMap<>();
    // userIds whose proxy was (re)built since the last persistence-context clear - lets invalidateManagedProxiedUsers
    // touch only those entries instead of rescanning the whole (ever-growing) managedUsers map on every clear
    private final Set<String> proxiedSinceLastClear = new HashSet<>();
    private final LDAPStorageProvider provider;

    public LDAPStorageUserManager(LDAPStorageProvider provider) {
        this.provider = provider;
    }

    public UserModel getManagedProxiedUser(String userId) {
        ManagedUserEntry entry = managedUsers.get(userId);
        return entry==null ? null : entry.getManagedProxiedUser();
    }

    public LDAPObject getManagedLDAPObject(String userId) {
        ManagedUserEntry entry = managedUsers.get(userId);
        if (entry != null) {
            return entry.getLdapUser();
        }
        else {
            return managedLDAPObjects.get(userId);
        }
    }

    public void setManagedLDAPObject(String userId, LDAPObject ldapObject) {
        LDAPObject object = managedLDAPObjects.get(userId);
        if (object != null) {
            throw new IllegalStateException("Don't expect to have ldap object for user " + userId);
        }
        managedLDAPObjects.put(userId, ldapObject);
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
            if (entry.getManagedProxiedUser() == null) {
                // entry survived a persistence-context clear (see invalidateManagedProxiedUsers) - refresh the
                // proxy, keeping the already-enlisted LDAPTransaction and ldapUser in place
                entry.setManagedProxiedUser(proxiedUser);
                proxiedSinceLastClear.add(userId);
                return;
            }
            throw new IllegalStateException("Don't expect to have entry for user " + userId);
        }

        LDAPTransaction ldapTransaction = new LDAPTransaction(provider, ldapObject);
        ManagedUserEntry newEntry = new ManagedUserEntry(proxiedUser, ldapObject, ldapTransaction);
        managedUsers.put(userId, newEntry);
        proxiedSinceLastClear.add(userId);
    }

    public void removeManagedUserEntry(String userId) {
        managedUsers.remove(userId);
        proxiedSinceLastClear.remove(userId);
    }

    /**
     * Invalidate the cached proxied {@link UserModel} for every user whose proxy was (re)built since the last
     * clear, without dropping the entries themselves - the enlisted {@link LDAPTransaction} and resolved
     * {@link LDAPObject} must survive. Call this whenever the underlying persistence context is cleared
     * mid-transaction, so the next access rebuilds the proxy instead of handing back one backed by a now-detached
     * entity.
     * <p>
     * Only {@link #proxiedSinceLastClear} is scanned - not the whole (ever-growing) {@code managedUsers} map -
     * so invalidation cost stays proportional to the batch size instead of the total number of users enlisted
     * so far in the transaction (eg. across all batches of a large export).
     */
    public void invalidateManagedProxiedUsers() {
        for (String userId : proxiedSinceLastClear) {
            ManagedUserEntry entry = managedUsers.get(userId);
            if (entry != null) {
                entry.setManagedProxiedUser(null);
            }
        }
        proxiedSinceLastClear.clear();
    }



    private static class ManagedUserEntry {

        private UserModel managedProxiedUser;
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

        public void setManagedProxiedUser(UserModel managedProxiedUser) {
            this.managedProxiedUser = managedProxiedUser;
        }

        public LDAPObject getLdapUser() {
            return ldapUser;
        }

        public LDAPTransaction getLdapTransaction() {
            return ldapTransaction;
        }
    }
}
