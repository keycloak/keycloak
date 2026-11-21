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

package org.keycloak.broker.saml;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SAMLIdentityProviderFactoryTest {

    private static final String METADATA = """
            <md:EntityDescriptor
                xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
                xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
                entityID="https://idp.example.test">
              <md:IDPSSODescriptor
                  protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
                <md:KeyDescriptor use="signing">
                  <ds:KeyInfo>
                    <ds:KeyName>signing-key-1</ds:KeyName>
                  </ds:KeyInfo>
                </md:KeyDescriptor>
                <md:SingleSignOnService
                    Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
                    Location="https://idp.example.test/sso"/>
              </md:IDPSSODescriptor>
            </md:EntityDescriptor>
            """;

    @Test
    void skipsSigningKeyWithoutCertificate() {
        Map<String, String> config = assertDoesNotThrow(
                () -> parseMetadata(METADATA));

        assertNull(config.get(
                SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY));
        assertEquals("false", config.get(
                SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
    }

    @Test
    void skipsEncryptionKeyWithoutCertificate() {
        String metadata = METADATA.replace(
                "use=\"signing\"", "use=\"encryption\"");

        assertDoesNotThrow(() -> parseMetadata(metadata));
    }

    @Test
    void skipsKeyWithUnspecifiedUsageWithoutCertificate() {
        String metadata = METADATA.replace(
                " use=\"signing\"", "");

        assertDoesNotThrow(() -> parseMetadata(metadata));
    }

    @Test
    void importsSigningCertificateWhenPresent() {
        String metadata = METADATA.replace(
                "<ds:KeyName>signing-key-1</ds:KeyName>",
                "<ds:X509Data><ds:X509Certificate>AQID</ds:X509Certificate></ds:X509Data>");

        Map<String, String> config = parseMetadata(metadata);

        assertEquals("AQID", config.get(
                SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY));
        assertEquals("true", config.get(
                SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
    }

    private Map<String, String> parseMetadata(String metadata) {
        return new SAMLIdentityProviderFactory()
                .parseConfig(null, metadata);
    }
}
