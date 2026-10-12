/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.userstorage;

import java.util.List;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Verifies the set of mappers created by default for a new LDAP provider.
 */
@KeycloakIntegrationTest
public class LdapDefaultMappersTest extends AbstractUserStorageRestTest {

    @ParameterizedTest(name = "rdn={0}, username={1}, editMode={2}")
    @CsvSource({
            // RDN, username, edit mode, expect write-only "full name" for cn, expect "username-cn"
            "uid, uid,            WRITABLE,  true,  false",
            "uid, uid,            READ_ONLY, false, false",
            "uid, uid,            UNSYNCED,  false, false",
            "uid, cn,             WRITABLE,  false, false",
            "cn,  cn,             WRITABLE,  false, false",
            "cn,  cn,             READ_ONLY, false, false",
            "cn,  sAMAccountName, WRITABLE,  false, true",
            "cn,  sAMAccountName, READ_ONLY, false, false",
            "cn,  sAMAccountName, UNSYNCED,  false, false",
    })
    public void testDefaultNameMappers(String rdnAttribute, String usernameAttribute, String editMode,
                                       boolean expectFullName, boolean expectUsernameCn) {
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.RDN_LDAP_ATTRIBUTE, rdnAttribute);
        ldapRep.getConfig().putSingle(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, usernameAttribute);
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, editMode);
        String ldapId = createComponent(ldapRep);

        try {
            List<ComponentRepresentation> mappers = managedRealm.admin().components().query(ldapId, LDAPStorageMapper.class.getName());

            List<ComponentRepresentation> firstNameMappers = getMappers(mappers, "first name");
            assertThat(firstNameMappers, hasSize(1));
            ComponentRepresentation firstNameMapper = firstNameMappers.get(0);
            assertThat(firstNameMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE), is(UserModel.FIRST_NAME));
            assertThat(firstNameMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE), is(LDAPConstants.GIVENNAME));

            List<ComponentRepresentation> fullNameMappers = getMappers(mappers, "full name");
            if (expectFullName) {
                assertThat(fullNameMappers, hasSize(1));
                ComponentRepresentation fullNameMapper = fullNameMappers.get(0);
                assertThat(fullNameMapper.getProviderId(), is(FullNameLDAPStorageMapperFactory.PROVIDER_ID));
                assertThat(fullNameMapper.getConfig().getFirst(FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE), equalToIgnoringCase(LDAPConstants.CN));
                assertThat(fullNameMapper.getConfig().getFirst(FullNameLDAPStorageMapper.WRITE_ONLY), is("true"));
                assertThat(fullNameMapper.getConfig().getFirst(FullNameLDAPStorageMapper.READ_ONLY), is("false"));
            } else {
                assertThat(fullNameMappers, empty());
            }

            List<ComponentRepresentation> usernameCnMappers = getMappers(mappers, "username-cn");
            if (expectUsernameCn) {
                assertThat(usernameCnMappers, hasSize(1));
                ComponentRepresentation usernameCnMapper = usernameCnMappers.get(0);
                assertThat(usernameCnMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE), is(UserModel.USERNAME));
                assertThat(usernameCnMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE), equalToIgnoringCase(LDAPConstants.CN));
            } else {
                assertThat(usernameCnMappers, empty());
            }
        } finally {
            removeComponent(ldapId);
        }
    }

    private static List<ComponentRepresentation> getMappers(List<ComponentRepresentation> mappers, String name) {
        return mappers.stream().filter(mapper -> name.equals(mapper.getName())).toList();
    }
}
