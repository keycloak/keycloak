/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;

import static org.keycloak.federation.kerberos.KerberosFederationProvider.KERBEROS_PRINCIPAL;

public class KerberosPrincipalAttributeMapper extends AbstractLDAPStorageMapper {

    public KerberosPrincipalAttributeMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        String kerberosPrincipalAttribute = ldapProvider.getKerberosConfig().getKerberosPrincipalAttribute();

        if (kerberosPrincipalAttribute != null) {
            String localKerberosPrincipal = user.getFirstAttribute(KERBEROS_PRINCIPAL);
            String ldapKerberosPrincipal = ldapUser.getAttributeAsString(kerberosPrincipalAttribute);
            if (ldapKerberosPrincipal != null) {
                // update the Kerberos principal stored in DB as user's attribute if it doesn't match LDAP
                if (!ldapKerberosPrincipal.equals(localKerberosPrincipal)) {
                    user.setSingleAttribute(KERBEROS_PRINCIPAL, ldapKerberosPrincipal);
                }
            }
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {

    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return delegate;
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {

    }
}
