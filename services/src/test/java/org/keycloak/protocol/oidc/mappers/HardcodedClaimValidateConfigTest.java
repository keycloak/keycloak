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
package org.keycloak.protocol.oidc.mappers;

import java.util.Map;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperConfigException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class HardcodedClaimValidateConfigTest {

    private final HardcodedClaim mapper = new HardcodedClaim();

    @Test
    public void resourceAccessClaimIsRejected() {
        assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(null, null, null,
                        HardcodedClaim.create("test", "resource_access.realm-management.roles", "[]", "JSON", true, false, false)));
    }

    @Test
    public void resourceAccessTopLevelIsRejected() {
        assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(null, null, null,
                        HardcodedClaim.create("test", "resource_access", "[]", "JSON", true, false, false)));
    }

    @Test
    public void realmAccessClaimIsRejected() {
        assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(null, null, null,
                        HardcodedClaim.create("test", "realm_access.roles", "[]", "JSON", true, false, false)));
    }

    @Test
    public void realmAccessTopLevelIsRejected() {
        assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(null, null, null,
                        HardcodedClaim.create("test", "realm_access", "[]", "JSON", true, false, false)));
    }

    @Test
    public void normalClaimNameIsAllowed() throws ProtocolMapperConfigException {
        mapper.validateConfig(null, null, null,
                HardcodedClaim.create("test", "email", "user@example.com", "String", true, false, false));
        mapper.validateConfig(null, null, null,
                HardcodedClaim.create("test", "given_name", "Alice", "String", true, false, false));
        mapper.validateConfig(null, null, null,
                HardcodedClaim.create("test", "custom.claim", "value", "String", true, false, false));
    }

    @Test
    public void nullClaimNameIsAllowed() throws ProtocolMapperConfigException {
        mapper.validateConfig(null, null, null,
                HardcodedClaim.create("test", null, "value", "String", true, false, false));
    }

    @Test
    public void emptyClaimNameIsAllowed() throws ProtocolMapperConfigException {
        mapper.validateConfig(null, null, null,
                HardcodedClaim.create("test", "", "value", "String", true, false, false));
    }

    @Test
    public void nullConfigAllowsConfiguration() throws ProtocolMapperConfigException {
        ProtocolMapperModel mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(null);

        mapper.validateConfig(null, null, null, mapperModel);
    }

    @Test
    public void reservedClaimNamesAreRejectedCaseInsensitively() {
        assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(null, null, null,
                        mapperModelWithClaimName("REALM_ACCESS.roles")));
        assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(null, null, null,
                        mapperModelWithClaimName("Resource_Access.test-client.roles")));
    }

    @Test
    public void reservedClaimErrorDoesNotEchoClaimName() {
        String claimName = "realm_access.\tforged";
        ProtocolMapperConfigException ex = assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(null, null, null, mapperModelWithClaimName(claimName)));

        assertFalse(ex.getMessage().contains(claimName));
    }

    private ProtocolMapperModel mapperModelWithClaimName(String claimName) {
        ProtocolMapperModel mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(Map.of(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, claimName));
        return mapperModel;
    }
}
