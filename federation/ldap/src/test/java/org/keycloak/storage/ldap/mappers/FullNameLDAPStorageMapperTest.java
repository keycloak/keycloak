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

import org.keycloak.component.ComponentModel;
import org.keycloak.models.UserModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @see FullNameLDAPStorageMapper#isUserAttributeReadOnly(String)
 */
public class FullNameLDAPStorageMapperTest {

    @Test
    public void isUserAttributeReadOnly_defaultConfig_returnsFalse() {
        FullNameLDAPStorageMapper mapper = mapperWithReadOnly(null);

        Assertions.assertFalse(mapper.isUserAttributeReadOnly(UserModel.FIRST_NAME));
        Assertions.assertFalse(mapper.isUserAttributeReadOnly(UserModel.LAST_NAME));
    }

    @Test
    public void isUserAttributeReadOnly_readOnlyConfig_returnsTrue() {
        // Without this override, decorateUserProfile()'s notWritableBackToLdap set (LDAPStorageProvider) never
        // includes firstName/lastName for a read-only full-name mapper, wrongly treating an invalid value on
        // either as user-fixable even though this mapper's proxy never writes it back to LDAP.
        FullNameLDAPStorageMapper mapper = mapperWithReadOnly(true);

        Assertions.assertTrue(mapper.isUserAttributeReadOnly(UserModel.FIRST_NAME));
        Assertions.assertTrue(mapper.isUserAttributeReadOnly(UserModel.LAST_NAME));
    }

    private static FullNameLDAPStorageMapper mapperWithReadOnly(Boolean readOnly) {
        ComponentModel mapperModel = new ComponentModel();
        if (readOnly != null) {
            mapperModel.getConfig().putSingle(FullNameLDAPStorageMapper.READ_ONLY, String.valueOf(readOnly));
        }
        return new FullNameLDAPStorageMapper(mapperModel, null);
    }
}
