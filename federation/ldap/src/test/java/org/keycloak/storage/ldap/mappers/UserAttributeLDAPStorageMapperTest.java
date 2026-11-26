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

package org.keycloak.storage.ldap.mappers;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;

import org.junit.Assert;
import org.junit.Test;

/**
 * Mapper-level tests for {@link UserAttributeLDAPStorageMapper}.
 */
public class UserAttributeLDAPStorageMapperTest {

    @Test
    public void testBinaryDecoderUuidObjectGuidWhenNotAdVendor() {
        // Setup LDAP configuration where the provider is not configured as Active Directory
        // (e.g. OpenLDAP proxy to Samba AD where uuid.ldap.attribute is entryUUID, so isObjectGUID() == false)
        MultivaluedHashMap<String, String> ldapConfigMap = new MultivaluedHashMap<>();
        ldapConfigMap.add(LDAPConstants.UUID_LDAP_ATTRIBUTE, LDAPConstants.ENTRY_UUID);
        LDAPConfig ldapConfig = new LDAPConfig(ldapConfigMap);
        Assert.assertFalse("Precondition failed: isObjectGUID() must be false to simulate OpenLDAP proxy", ldapConfig.isObjectGUID());

        LDAPIdentityStore ldapIdentityStore = new LDAPIdentityStore(null, ldapConfig);
        LDAPStorageProvider ldapProvider = new LDAPStorageProvider(null, null, new ComponentModel(), ldapIdentityStore);

        // Configure UserAttributeLDAPStorageMapper with objectGUID and binary.attribute.decoder = uuid
        ComponentModel mapperModel = new ComponentModel();
        mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, "customGuid");
        mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, "objectGUID");
        mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE, "true");
        mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true");
        mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.BINARY_ATTRIBUTE_DECODER, UserAttributeLDAPStorageMapper.BINARY_DECODER_UUID);
        mapperModel.getConfig().putSingle(UserAttributeLDAPStorageMapper.READ_ONLY, "true");

        UserAttributeLDAPStorageMapper mapper = new UserAttributeLDAPStorageMapper(mapperModel, ldapProvider);

        // Base64-encoded representation of 16-byte objectGUID from Samba AD:
        // 54 71 1f 58 3a d2 c3 4a ab 97 d3 44 ed 17 8c dc -> VHEfWDrSw0qrl9NE7ReM3A==
        // Expected decoded canonical Active Directory GUID: 581f7154-d23a-4ac3-ab97-d344ed178cdc
        LDAPObject ldapUser = new LDAPObject();
        ldapUser.setSingleAttribute("objectGUID", "VHEfWDrSw0qrl9NE7ReM3A==");

        UserModel delegate = new UserModelDelegate(null) {
            @Override
            public String getFirstAttribute(String name) {
                return null;
            }
        };

        UserModel proxied = mapper.proxy(ldapUser, delegate, null);
        String decodedGuid = proxied.getFirstAttribute("customGuid");

        Assert.assertEquals("581f7154-d23a-4ac3-ab97-d344ed178cdc", decodedGuid);
    }
}
