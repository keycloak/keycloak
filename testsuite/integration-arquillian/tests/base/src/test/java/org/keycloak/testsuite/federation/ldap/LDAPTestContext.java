/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.ldap;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.testsuite.util.LDAPTestUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPTestContext {

    private final RealmModel realm;
    private final UserStorageProviderModel ldapModel;
    private final LDAPStorageProvider ldapProvider;

    public static LDAPTestContext init(KeycloakSession session) {
        RealmModel testRealm = session.realms().getRealm(AbstractLDAPTest.TEST_REALM_NAME);
        ComponentModel ldapCompModel = LDAPTestUtils.getLdapProviderModel(testRealm);
        UserStorageProviderModel ldapModel = new UserStorageProviderModel(ldapCompModel);
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
        return new LDAPTestContext(testRealm, ldapModel, ldapProvider);
    }

    private LDAPTestContext(RealmModel realm, UserStorageProviderModel ldapModel, LDAPStorageProvider ldapProvider) {
        this.realm = realm;
        this.ldapModel = ldapModel;
        this.ldapProvider = ldapProvider;
    }


    public RealmModel getRealm() {
        return realm;
    }

    public UserStorageProviderModel getLdapModel() {
        return ldapModel;
    }

    public LDAPStorageProvider getLdapProvider() {
        return ldapProvider;
    }
}
