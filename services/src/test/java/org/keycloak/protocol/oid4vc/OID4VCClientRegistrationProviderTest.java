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

package org.keycloak.protocol.oid4vc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.OID4VCClient;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class OID4VCClientRegistrationProviderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Single Supported Credential with format and single-type.",
                        Map.of(
                                "vc.credential-id.format", Format.JWT_VC.toString(),
                                "vc.credential-id.scope", "VerifiableCredential"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredentialConfiguration()
                                        .setId("credential-id")
                                        .setFormat(Format.JWT_VC)
                                        .setScope("VerifiableCredential")),
                                null, null)
                },
                {
                        "Single Supported Credential with format and multi-type.",
                        Map.of(
                                "vc.credential-id.format", Format.JWT_VC.toString(),
                                "vc.credential-id.scope", "AnotherCredential"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredentialConfiguration()
                                        .setId("credential-id")
                                        .setFormat(Format.JWT_VC)
                                        .setScope("AnotherCredential")),
                                null, null)
                },
                {
                        "Single Supported Credential with format, multi-type and a display object.",
                        Map.of(
                                "vc.credential-id.format", Format.JWT_VC.toString(),
                                "vc.credential-id.scope", "AnotherCredential",
                                "vc.credential-id.display.name", "Another",
                                "vc.credential-id.display.locale", "en"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredentialConfiguration()
                                        .setId("credential-id")
                                        .setFormat(Format.JWT_VC)
                                        .setDisplay(new DisplayObject().setLocale("en").setName("Another"))
                                        .setScope("AnotherCredential")),
                                null, null)
                },
                {
                        "Multiple Supported Credentials.",
                        Map.of(
                                "vc.first-id.format", Format.JWT_VC.toString(),
                                "vc.first-id.scope", "AnotherCredential",
                                "vc.first-id.display.name", "First",
                                "vc.first-id.display.locale", "en",
                                "vc.second-id.format", Format.SD_JWT_VC.toString(),
                                "vc.second-id.scope", "MyType",
                                "vc.second-id.display.name", "Second Credential",
                                "vc.second-id.display.locale", "de"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredentialConfiguration()
                                                .setId("first-id")
                                                .setFormat(Format.JWT_VC)
                                                .setDisplay(new DisplayObject().setLocale("en").setName("First"))
                                                .setScope("AnotherCredential"),
                                        new SupportedCredentialConfiguration()
                                                .setId("second-id")
                                                .setFormat(Format.SD_JWT_VC)
                                                .setDisplay(new DisplayObject().setLocale("de").setName("Second Credential"))
                                                .setScope("MyType")),
                                null, null)
                }
        });
    }

    private Map<String, String> clientAttributes;
    private OID4VCClient oid4VCClient;

    public OID4VCClientRegistrationProviderTest(String name, Map<String, String> clientAttributes, OID4VCClient oid4VCClient) {
        this.clientAttributes = clientAttributes;
        this.oid4VCClient = oid4VCClient;
    }

    @Test
    public void testToClientRepresentation() {
        Map<String, String> translatedAttributes = OID4VCClientRegistrationProvider.toClientRepresentation(oid4VCClient).getAttributes();

        assertEquals("The client should have been translated into the correct clientRepresentation.", clientAttributes.entrySet().size(), translatedAttributes.size());
        clientAttributes.forEach((key, value) ->
                assertEquals("The client should have been translated into the correct clientRepresentation.", clientAttributes.get(key), translatedAttributes.get(key)));
    }

    @Test
    public void testFromClientAttributes() {
        assertEquals("The client should have been correctly build from the client representation",
                oid4VCClient,
                OID4VCClientRegistrationProvider.fromClientAttributes("did:web:test.org", clientAttributes));
    }

}