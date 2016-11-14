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

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.user.SynchronizationResult;

import javax.naming.AuthenticationException;
import java.util.List;

/**
 * Sufficient if mapper implementation is stateless and doesn't need to "close" any state
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPStorageMapperBridge implements LDAPStorageMapper {

    private final AbstractLDAPStorageMapperFactory factory;

    public LDAPStorageMapperBridge(AbstractLDAPStorageMapperFactory factory) {
        this.factory = factory;
    }

    // Sync groups from LDAP to Keycloak DB
    @Override
    public SynchronizationResult syncDataFromFederationProviderToKeycloak(ComponentModel mapperModel, LDAPStorageProvider federationProvider, KeycloakSession session, RealmModel realm) {
        return getDelegate(mapperModel, federationProvider, realm).syncDataFromFederationProviderToKeycloak();
    }

    @Override
    public SynchronizationResult syncDataFromKeycloakToFederationProvider(ComponentModel mapperModel, LDAPStorageProvider federationProvider, KeycloakSession session, RealmModel realm) {
        return getDelegate(mapperModel, federationProvider, realm).syncDataFromKeycloakToFederationProvider();
    }

    @Override
    public void onImportUserFromLDAP(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        getDelegate(mapperModel, ldapProvider, realm).onImportUserFromLDAP(ldapUser, user, isCreate);
    }

    @Override
    public void onRegisterUserToLDAP(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        getDelegate(mapperModel, ldapProvider, realm).onRegisterUserToLDAP(ldapUser, localUser);
    }

    @Override
    public UserModel proxy(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return getDelegate(mapperModel, ldapProvider, realm).proxy(ldapUser, delegate);
    }

    @Override
    public void beforeLDAPQuery(ComponentModel mapperModel, LDAPQuery query) {
        // Improve if needed
        getDelegate(mapperModel, query.getLdapProvider(), null).beforeLDAPQuery(query);
    }


    @Override
    public List<UserModel> getGroupMembers(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return getDelegate(mapperModel, ldapProvider, realm).getGroupMembers(group, firstResult, maxResults);
    }

    @Override
    public boolean onAuthenticationFailure(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, LDAPObject ldapUser, UserModel user, AuthenticationException ldapException, RealmModel realm) {
        return getDelegate(mapperModel, ldapProvider, realm).onAuthenticationFailure(ldapUser, user, ldapException);
    }

    private AbstractLDAPStorageMapper getDelegate(ComponentModel mapperModel, LDAPStorageProvider federationProvider, RealmModel realm) {
        LDAPStorageProvider ldapProvider = (LDAPStorageProvider) federationProvider;
        return factory.createMapper(mapperModel, ldapProvider, realm);
    }

    @Override
    public void close() {

    }
}
