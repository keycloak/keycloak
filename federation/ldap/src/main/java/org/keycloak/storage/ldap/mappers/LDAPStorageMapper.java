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

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.user.SynchronizationResult;

import javax.naming.AuthenticationException;
import java.util.List;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface LDAPStorageMapper extends Provider {

    /**
     * Sync data from federated storage to Keycloak. It's useful just if mapper needs some data preloaded from federated storage (For example
     * load roles from federated provider and sync them to Keycloak database)
     *
     * Applicable just if sync is supported
     *
     */
    SynchronizationResult syncDataFromFederationProviderToKeycloak(RealmModel realm);

    /**
     * Sync data from Keycloak back to federated storage
     *
     **/
    SynchronizationResult syncDataFromKeycloakToFederationProvider(RealmModel realm);

    /**
     * Return empty list if doesn't support storing of groups
     */
    List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults);

    /**
     * Return empty list if doesn't support storing of roles
     * @param realm
     * @param role
     * @param firstResult
     * @param maxResults
     * @return
     */
    List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults);

    /**
     * Called when importing user from LDAP to local keycloak DB.
     *
     * @param ldapUser
     * @param user
     * @param realm
     * @param isCreate true if we importing new user from LDAP. False if user already exists in Keycloak, but we are upgrading (syncing) it from LDAP
     */
    void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate);


    /**
     * Called when register new user to LDAP - just after user was created in Keycloak DB
     *
     * @param ldapUser
     * @param localUser
     * @param realm
     */
    void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm);


    /**
     * Called when invoke proxy on LDAP federation provider
     *
     * @param ldapUser
     * @param delegate
     * @param realm
     * @return
     */
    UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm);


    /**
     * Called before LDAP Identity query for retrieve LDAP users was executed. It allows to change query somehow (add returning attributes from LDAP, change conditions etc)
     *
     * @param query
     */
    void beforeLDAPQuery(LDAPQuery query);

    /**
     * Called when LDAP authentication of specified user fails. If any mapper returns true from this method, AuthenticationException won't be rethrown!
     *
     * @param user
     * @param ldapUser
     * @param ldapException
     * @return true if mapper processed the AuthenticationException and did some actions based on that. In that case, AuthenticationException won't be rethrown!
     */
    boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, AuthenticationException ldapException, RealmModel realm);

    /**
     * Gets the ldap provider associated to the mapper.
     *
     * @return
     */
    public LDAPStorageProvider getLdapProvider();
}
