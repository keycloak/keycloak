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

import java.util.Collections;
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
import org.keycloak.mappers.UserFederationMapper;

/**
 * Stateful per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPFederationMapper {

    protected final UserFederationMapperModel mapperModel;
    protected final LDAPFederationProvider ldapProvider;
    protected final RealmModel realm;

    public AbstractLDAPFederationMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm) {
        this.mapperModel = mapperModel;
        this.ldapProvider = ldapProvider;
        this.realm = realm;
    }

    /**
     * @see UserFederationMapper#syncDataFromFederationProviderToKeycloak(UserFederationMapperModel, UserFederationProvider, KeycloakSession, RealmModel)
     */
    public UserFederationSyncResult syncDataFromFederationProviderToKeycloak() {
        return new UserFederationSyncResult();
    }

    /**
     * @see UserFederationMapper#syncDataFromKeycloakToFederationProvider(UserFederationMapperModel, UserFederationProvider, KeycloakSession, RealmModel)
     */
    public UserFederationSyncResult syncDataFromKeycloakToFederationProvider() {
        return new UserFederationSyncResult();
    }

    /**
     * @see LDAPFederationMapper#beforeLDAPQuery(UserFederationMapperModel, LDAPQuery)
     */
    public abstract void beforeLDAPQuery(LDAPQuery query);

    /**
     * @see LDAPFederationMapper#proxy(UserFederationMapperModel, LDAPFederationProvider, LDAPObject, UserModel, RealmModel)
     */
    public abstract UserModel proxy(LDAPObject ldapUser, UserModel delegate);

    /**
     * @see LDAPFederationMapper#onRegisterUserToLDAP(UserFederationMapperModel, LDAPFederationProvider, LDAPObject, UserModel, RealmModel)
     */
    public abstract void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser);

    /**
     * @see LDAPFederationMapper#onImportUserFromLDAP(UserFederationMapperModel, LDAPFederationProvider, LDAPObject, UserModel, RealmModel, boolean)
     */
    public abstract void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, boolean isCreate);

    public List<UserModel> getGroupMembers(GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    public boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, AuthenticationException ldapException) {
        return false;
    }


    public static boolean parseBooleanParameter(UserFederationMapperModel mapperModel, String paramName) {
        String paramm = mapperModel.getConfig().get(paramName);
        return Boolean.parseBoolean(paramm);
    }

    public LDAPFederationProvider getLdapProvider() {
        return ldapProvider;
    }

    public RealmModel getRealm() {
        return realm;
    }
}
