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
package org.keycloak.saml.processing.core.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;

import org.keycloak.saml.common.util.DocumentUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link XMLSignatureUtil}.
 *
 * Reproduces: https://github.com/keycloak/keycloak/issues/46302
 */
public class XMLSignatureUtilTest {

    private static final String SIMPLE_XML = "<root><child>value</child></root>";

    private KeyPair keyPair;

    @Before
    public void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
    }

    @After
    public void tearDown() {
        // includeKeyInfoInSignature is a static field — reset to default so other tests are not affected
        XMLSignatureUtil.setIncludeKeyInfoInSignature(true);
    }

    @Test
    public void testSignWithKeyInfoIncluded() throws Exception {
        Document doc = DocumentUtil.getDocument(SIMPLE_XML);
        Document signed = XMLSignatureUtil.sign(doc, "my-key", keyPair,
                DigestMethod.SHA256, SignatureMethod.RSA_SHA256, "",
                CanonicalizationMethod.EXCLUSIVE);

        assertNotNull("Signature element must be present", getSignatureElement(signed));
        assertNotNull("KeyInfo must be present when includeKeyInfoInSignature=true",
                getKeyInfoElement(signed));
    }

    @Test
    public void testSignWithKeyInfoExcludedAndNonNullKeyName() throws Exception {
        // Reproduces: when includeKeyInfoInSignature=false and keyName is non-null,
        // sign() should succeed and produce NO <KeyInfo> element.
        // Currently it produces a stub <KeyInfo><KeyName/></KeyInfo> instead.
        XMLSignatureUtil.setIncludeKeyInfoInSignature(false);

        Document doc = DocumentUtil.getDocument(SIMPLE_XML);
        Document signed = XMLSignatureUtil.sign(doc, "my-key", keyPair,
                DigestMethod.SHA256, SignatureMethod.RSA_SHA256, "",
                CanonicalizationMethod.EXCLUSIVE);

        assertNotNull("Signature element must be present", getSignatureElement(signed));
        assertNull("KeyInfo must be absent when includeKeyInfoInSignature=false",
                getKeyInfoElement(signed));
    }

    @Test
    public void testSignWithKeyInfoExcludedAndNullKeyName() throws Exception {
        // Reproduces: when includeKeyInfoInSignature=false and keyName is null,
        // sign() should succeed and produce NO <KeyInfo> element.
        // Currently it throws IllegalArgumentException due to empty KeyInfo items list.
        XMLSignatureUtil.setIncludeKeyInfoInSignature(false);

        Document doc = DocumentUtil.getDocument(SIMPLE_XML);
        Document signed = XMLSignatureUtil.sign(doc, null, keyPair,
                DigestMethod.SHA256, SignatureMethod.RSA_SHA256, "",
                CanonicalizationMethod.EXCLUSIVE);

        assertNotNull("Signature element must be present", getSignatureElement(signed));
        assertNull("KeyInfo must be absent when includeKeyInfoInSignature=false",
                getKeyInfoElement(signed));
    }

    private static org.w3c.dom.Node getSignatureElement(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
        return nodes.getLength() > 0 ? nodes.item(0) : null;
    }

    private static org.w3c.dom.Node getKeyInfoElement(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "KeyInfo");
        return nodes.getLength() > 0 ? nodes.item(0) : null;
    }
}
