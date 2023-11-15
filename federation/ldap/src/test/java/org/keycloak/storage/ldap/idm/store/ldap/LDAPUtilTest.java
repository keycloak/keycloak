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
}
