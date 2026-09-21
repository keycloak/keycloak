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
package org.keycloak.protocol.saml.installation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.protocol.saml.IDPMetadataDescriptor;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

public class ModAuthMellonClientInstallationTest {

    private static final URI ENDPOINT = URI.create("https://keycloak.example.org/realms/test/protocol/saml");

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testXmlFilesInZipArePrettyPrinted() throws Exception {
        String idpDescriptor = IDPMetadataDescriptor.getIDPDescriptor(ENDPOINT, ENDPOINT, ENDPOINT, ENDPOINT,
                "https://keycloak.example.org/realms/test", true, Collections.emptyList());
        String spDescriptor = spDescriptor();

        Map<String, String> entries = unzip(ModAuthMellonClientInstallation.createZip("my-client", idpDescriptor, spDescriptor, null, null));

        assertThat(entries, hasKey("my-client/idp-metadata.xml"));
        assertThat(entries, hasKey("my-client/sp-metadata.xml"));
        for (String name : new String[] {"my-client/idp-metadata.xml", "my-client/sp-metadata.xml"}) {
            String xml = entries.get(name);
            assertThat(name + " should span multiple lines", xml.trim().split("\n").length, greaterThan(5));
            Document document = DocumentUtil.getDocument(xml);
            assertThat(document.getDocumentElement().getLocalName(), is("EntityDescriptor"));
            assertThat(document.getDocumentElement().getNamespaceURI(), is(JBossSAMLURIConstants.METADATA_NSURI.get()));
        }
        assertThat(entries.get("my-client/sp-metadata.xml"), containsString("https://sp.example.org/saml"));
    }

    @Test
    public void testSignedIdpMetadataIsKeptValid() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        KeyWrapper key = new KeyWrapper();
        key.setKid("kid");
        key.setPrivateKey(keyPair.getPrivate());
        key.setPublicKey(keyPair.getPublic());
        key.setCertificate(CertificateUtils.generateV1SelfSignedCertificate(keyPair, "test"));

        String idpDescriptor = IDPMetadataDescriptor.getIDPDescriptor(key, SignatureAlgorithm.RSA_SHA256, ENDPOINT, ENDPOINT, ENDPOINT, ENDPOINT,
                "https://keycloak.example.org/realms/test", true, Collections.emptyList(), null);
        assertThat(idpDescriptor, containsString("Signature"));

        Map<String, String> entries = unzip(ModAuthMellonClientInstallation.createZip("my-client", idpDescriptor, spDescriptor(), null, null));

        // reformatting a signed document would break its signature, so it has to be written as-is
        assertThat(entries.get("my-client/idp-metadata.xml"), is(idpDescriptor));
        Document document = DocumentUtil.getDocument(entries.get("my-client/idp-metadata.xml"));
        document.getDocumentElement().setIdAttribute("ID", true);
        assertThat(XMLSignatureUtil.validate(document, new HardcodedKeyLocator(keyPair.getPublic())), is(true));
    }

    private static String spDescriptor() throws Exception {
        return SAMLMetadataUtil.writeEntityDescriptorType(SPMetadataDescriptor.buildSPDescriptor(
                JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri(), JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri(),
                URI.create("https://sp.example.org/saml"), URI.create("https://sp.example.org/saml/logout"),
                false, false, false, "my-client", JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get(),
                Collections.emptyList(), Collections.emptyList()));
    }

    private static Map<String, String> unzip(byte[] zip) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
