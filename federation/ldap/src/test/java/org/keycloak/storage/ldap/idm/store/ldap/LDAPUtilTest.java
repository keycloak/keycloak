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

package org.keycloak.storage.ldap.idm.store.ldap;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;

import org.junit.Assert;
import org.junit.Test;

public class LDAPUtilTest {

    @Test
    public void testEncodeDecodeGUID() {
        String displayGUID = "2f419d1c-6495-479f-b340-9cb419eb9ae7";
        byte[] bytes = LDAPUtil.encodeObjectGUID(displayGUID);
        String decodeObjectGUID = LDAPUtil.decodeObjectGUID(bytes);
        Assert.assertEquals(displayGUID, decodeObjectGUID);
    }

    @Test
    public void testEncodeEDirectoryGUID() {
        String guid = "bcdf4a91-ccb1-ae49-a18f-bcdf4a91ccff";
        byte[] bytes = LDAPUtil.encodeObjectEDirectoryGUID(guid);
        String decodeObjectGUID = LDAPUtil.decodeGuid(bytes);
        Assert.assertEquals(guid, decodeObjectGUID);
    }

    @Test
    public void testCheckLdapConnectionUrlsMatch() {
        Assert.assertTrue(LDAPUtil.checkLdapConnectionUrlsMatch(null, null));
        Assert.assertFalse(LDAPUtil.checkLdapConnectionUrlsMatch("ldap://localhost:10389", null));
        Assert.assertFalse(LDAPUtil.checkLdapConnectionUrlsMatch(null, "ldap://localhost:10389"));
        Assert.assertTrue(LDAPUtil.checkLdapConnectionUrlsMatch("ldap://localhost:10389", "ldap://localhost:10389"));
        Assert.assertTrue(LDAPUtil.checkLdapConnectionUrlsMatch("ldap://localhost:10389 ", " ldap://localhost:10389"));
        Assert.assertTrue(LDAPUtil.checkLdapConnectionUrlsMatch("ldap://host1:389 ldap://host2:389", "ldap://host1:389 ldap://host2:389"));
        Assert.assertFalse(LDAPUtil.checkLdapConnectionUrlsMatch("ldap://localhost:10389", "ldap://anotherhost:10389"));
        Assert.assertFalse(LDAPUtil.checkLdapConnectionUrlsMatch("ldap://host1:389", "ldap://host1:389 ldap://host2:389"));
    }

    @Test
    public void testLegacyDecodeBase64ToUuidPreservesBehavior() {
        String base64Value = "VHEfWDrSw0qrl9NE7ReM3A==";
        String expectedGuid = "581f7154-d23a-4ac3-ab97-d344ed178cdc";

        // When provider is Active Directory (isObjectGUID() == true)
        MultivaluedHashMap<String, String> adCfg = new MultivaluedHashMap<>();
        adCfg.add(LDAPConstants.UUID_LDAP_ATTRIBUTE, LDAPConstants.OBJECT_GUID);
        LDAPConfig adConfig = new LDAPConfig(adCfg);
        Assert.assertTrue(adConfig.isObjectGUID());
        Assert.assertEquals(expectedGuid, LDAPUtil.decodeBase64ToUuid(base64Value, adConfig));

        // When provider is NOT Active Directory (isObjectGUID() == false)
        MultivaluedHashMap<String, String> nonAdCfg = new MultivaluedHashMap<>();
        nonAdCfg.add(LDAPConstants.UUID_LDAP_ATTRIBUTE, LDAPConstants.ENTRY_UUID);
        LDAPConfig nonAdConfig = new LDAPConfig(nonAdCfg);
        Assert.assertFalse(nonAdConfig.isObjectGUID());
        // Legacy 2-argument overload must strictly preserve original behavior and return raw base64Value
        Assert.assertEquals(base64Value, LDAPUtil.decodeBase64ToUuid(base64Value, nonAdConfig));
    }

    @Test
    public void testDecodeBase64ToUuidWithObjectGUIDAttributeWhenNotAdVendor() {
        // Reproduces OpenLDAP proxy to Samba AD scenario where provider is not configured as AD (isObjectGUID() == false),
        // but the mapped attribute is objectGUID.
        String base64Value = "VHEfWDrSw0qrl9NE7ReM3A==";
        String expectedGuid = "581f7154-d23a-4ac3-ab97-d344ed178cdc";

        MultivaluedHashMap<String, String> nonAdCfg = new MultivaluedHashMap<>();
        nonAdCfg.add(LDAPConstants.UUID_LDAP_ATTRIBUTE, LDAPConstants.ENTRY_UUID);
        LDAPConfig nonAdConfig = new LDAPConfig(nonAdCfg);
        Assert.assertFalse(nonAdConfig.isObjectGUID());

        // 3-argument overload with mapped attribute name "objectGUID"
        String decoded = LDAPUtil.decodeBase64ToUuid(base64Value, nonAdConfig, "objectGUID");
        Assert.assertEquals(expectedGuid, decoded);

        // Case-insensitivity check
        String decodedLower = LDAPUtil.decodeBase64ToUuid(base64Value, nonAdConfig, "objectguid");
        Assert.assertEquals(expectedGuid, decodedLower);
    }

    @Test
    public void testDecodeBase64ToUuidInvalidLengthReturnsBase64() {
        MultivaluedHashMap<String, String> adCfg = new MultivaluedHashMap<>();
        adCfg.add(LDAPConstants.UUID_LDAP_ATTRIBUTE, LDAPConstants.OBJECT_GUID);
        LDAPConfig adConfig = new LDAPConfig(adCfg);

        // 8 bytes instead of 16 bytes
        byte[] invalidBytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        String invalidBase64 = Base64.getEncoder().encodeToString(invalidBytes);

        // Must not throw ArrayIndexOutOfBoundsException, must return original base64
        Assert.assertEquals(invalidBase64, LDAPUtil.decodeBase64ToUuid(invalidBase64, adConfig));
        Assert.assertEquals(invalidBase64, LDAPUtil.decodeBase64ToUuid(invalidBase64, adConfig, "objectGUID"));
    }

    @Test
    public void testDecodeBase64ToUuidGenericRFC4122() {
        UUID expectedUuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(expectedUuid.getMostSignificantBits());
        bb.putLong(expectedUuid.getLeastSignificantBits());
        String base64Value = Base64.getEncoder().encodeToString(bb.array());

        MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>();
        cfg.add(LDAPConstants.UUID_LDAP_ATTRIBUTE, LDAPConstants.ENTRY_UUID);
        LDAPConfig config = new LDAPConfig(cfg);

        // 3-argument overload with generic attribute name decodes as RFC 4122 UUID
        String decoded = LDAPUtil.decodeBase64ToUuid(base64Value, config, "customBinaryUuid");
        Assert.assertEquals(expectedUuid.toString(), decoded);
    }
}
