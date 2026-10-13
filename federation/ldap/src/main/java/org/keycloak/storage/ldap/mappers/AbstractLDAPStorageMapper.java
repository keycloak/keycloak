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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.naming.AuthenticationException;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * Stateful per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPStorageMapper implements LDAPStorageMapper {

    protected final ComponentModel mapperModel;
    protected final LDAPStorageProvider ldapProvider;

    public AbstractLDAPStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        this.mapperModel = mapperModel;
        this.ldapProvider = ldapProvider;
    }

    @Override
    public SynchronizationResult syncDataFromFederationProviderToKeycloak(RealmModel realm) {
        return new SynchronizationResult();
    }

    @Override
    public SynchronizationResult syncDataFromKeycloakToFederationProvider(RealmModel realm) {
        return new SynchronizationResult();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, AuthenticationException ldapException, RealmModel realm) {
        return false;
    }

    @Override
    public Set<String> mandatoryAttributeNames() {
        return null;
    }

    @Override
    public Set<String> getUserAttributes() {
        return Collections.emptySet();
    }

    public static boolean parseBooleanParameter(ComponentModel mapperModel, String paramName) {
        String paramm = mapperModel.getConfig().getFirst(paramName);
        return Boolean.parseBoolean(paramm);
    }

    @Override
    public LDAPStorageProvider getLdapProvider() {
        return ldapProvider;
    }

    @Override
    public void close() {

    }

    protected KeycloakSession getSession() {
        return KeycloakSessionUtil.getKeycloakSession();
    }
}
