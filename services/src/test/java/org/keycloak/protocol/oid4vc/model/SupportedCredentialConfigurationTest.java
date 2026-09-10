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
package org.keycloak.protocol.oid4vc.model;

import org.keycloak.VCFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SupportedCredentialConfigurationTest {

    @Test
    public void shouldDeriveVctForSdJwt() {
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration()
                .setFormat(VCFormat.SD_JWT_VC)
                .setVct("https://credentials.example.com/identity");

        assertEquals("https://credentials.example.com/identity", config.deriveType().getValue());
    }

    @Test
    public void shouldDeriveDocTypeForMdoc() {
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration()
                .setFormat(VCFormat.MSO_MDOC)
                .setDocType("org.iso.18013.5.1.mDL");

        assertEquals("org.iso.18013.5.1.mDL", config.deriveType().getValue());
    }

    @Test
    public void shouldDeriveNullForMdocWithoutDocType() {
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration()
                .setFormat(VCFormat.MSO_MDOC);

        assertNull(config.deriveType());
    }

    @Test
    public void shouldDeriveNullForOtherFormats() {
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration()
                .setFormat(VCFormat.JWT_VC);

        assertNull(config.deriveType());
    }
}
