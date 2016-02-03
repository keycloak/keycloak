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

import java.util.List;

import javax.naming.AuthenticationException;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;

/**
 * Sufficient if mapper implementation is stateless and doesn't need to "close" any state
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPFederationMapperBridge implements LDAPFederationMapper {

    private final AbstractLDAPFederationMapperFactory factory;

    public LDAPFederationMapperBridge(AbstractLDAPFederationMapperFactory factory) {
        this.factory = factory;
    }

    // Sync groups from LDAP to Keycloak DB
    @Override
    public UserFederationSyncResult syncDataFromFederationProviderToKeycloak(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm) {
        return getDelegate(mapperModel, federationProvider, realm).syncDataFromFederationProviderToKeycloak();
    }

    @Override
    public UserFederationSyncResult syncDataFromKeycloakToFederationProvider(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm) {
        return getDelegate(mapperModel, federationProvider, realm).syncDataFromKeycloakToFederationProvider();
    }

    @Override
    public void onImportUserFromLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        getDelegate(mapperModel, ldapProvider, realm).onImportUserFromLDAP(ldapUser, user, isCreate);
    }

    @Override
    public void onRegisterUserToLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        getDelegate(mapperModel, ldapProvider, realm).onRegisterUserToLDAP(ldapUser, localUser);
    }

    @Override
    public UserModel proxy(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return getDelegate(mapperModel, ldapProvider, realm).proxy(ldapUser, delegate);
    }

    @Override
    public void beforeLDAPQuery(UserFederationMapperModel mapperModel, LDAPQuery query) {
        // Improve if needed
        getDelegate(mapperModel, query.getLdapProvider(), null).beforeLDAPQuery(query);
    }


    @Override
    public List<UserModel> getGroupMembers(UserFederationMapperModel mapperModel, UserFederationProvider ldapProvider, RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return getDelegate(mapperModel, ldapProvider, realm).getGroupMembers(group, firstResult, maxResults);
    }

    @Override
    public boolean onAuthenticationFailure(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel user, AuthenticationException ldapException, RealmModel realm) {
        return getDelegate(mapperModel, ldapProvider, realm).onAuthenticationFailure(ldapUser, user, ldapException);
    }

    private AbstractLDAPFederationMapper getDelegate(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, RealmModel realm) {
        LDAPFederationProvider ldapProvider = (LDAPFederationProvider) federationProvider;
        return factory.createMapper(mapperModel, ldapProvider, realm);
    }

    @Override
    public void close() {

    }
}
