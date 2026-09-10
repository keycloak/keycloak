/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.api.saml.v2.sig;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;

import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SAML2SignatureTest {

    private static final String PROTOCOL_NS = "urn:oasis:names:tc:SAML:2.0:protocol";
    private static final String ASSERTION_NS = "urn:oasis:names:tc:SAML:2.0:assertion";

    private static KeyPair keyPair;

    @BeforeClass
    public static void generateKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
    }

    /**
     *
     * {@link SAML2Signature#validate(Document, org.keycloak.rotation.KeyLocator)}
     * must return {@code true} when the document has a signed nested
     * {@code samlp:Response} inside an unsigned {@code samlp:ArtifactResponse}.
     */
    @Test
    public void validateAcceptsSignedNestedResponseInsideArtifactResponse() throws Exception {
        Document doc = buildArtifactResponseWithSignedNestedResponse();

        SAML2Signature saml2Signature = new SAML2Signature();
        HardcodedKeyLocator locator = new HardcodedKeyLocator(keyPair.getPublic());

        assertThat(saml2Signature.validate(doc, locator), is(true));
    }

    /**
     * Sanity guard: signing the {@code ArtifactResponse} root element (the
     * pre-existing supported path) must continue to validate correctly after the fix.
     */
    @Test
    public void validateAcceptsSignedArtifactResponseRoot() throws Exception {
        String artifactResponseId = "AR_outer_2";
        String xml = "<samlp:ArtifactResponse"
                + " xmlns:samlp=\"" + PROTOCOL_NS + "\""
                + " xmlns:saml=\"" + ASSERTION_NS + "\""
                + " ID=\"" + artifactResponseId + "\""
                + " InResponseTo=\"req2\""
                + " IssueInstant=\"2025-01-01T00:00:00Z\""
                + " Version=\"2.0\">"
                + "<saml:Issuer>test-idp</saml:Issuer>"
                + "</samlp:ArtifactResponse>";

        Document doc = DocumentUtil.getDocument(xml);
        SAML2Signature saml2Signature = new SAML2Signature();
        saml2Signature.setSignatureMethod(SignatureMethod.RSA_SHA256);
        saml2Signature.setDigestMethod(DigestMethod.SHA256);
        saml2Signature.signSAMLDocument(doc, "test-key", keyPair, CanonicalizationMethod.EXCLUSIVE);

        HardcodedKeyLocator locator = new HardcodedKeyLocator(keyPair.getPublic());

        assertThat(saml2Signature.validate(doc, locator), is(true));
    }

    /**
     * Regression test: an ArtifactResponse containing a signed LogoutResponse
     * followed by an unsigned Response must be rejected. Without this guard an
     * attacker could replay a signed protocol element alongside a forged
     * unsigned Response and bypass signature validation.
     */
    @Test
    public void validateRejectsSignedSiblingPayloadWithUnsignedResponse() throws Exception {
        String xml = "<samlp:ArtifactResponse"
                + " xmlns:samlp=\"" + PROTOCOL_NS + "\""
                + " xmlns:saml=\"" + ASSERTION_NS + "\""
                + " ID=\"AR_multi\""
                + " Version=\"2.0\""
                + " IssueInstant=\"2025-01-01T00:00:00Z\">"
                + "<samlp:Status/>"
                + "<samlp:LogoutResponse"
                + " ID=\"LR_signed\""
                + " Version=\"2.0\""
                + " IssueInstant=\"2025-01-01T00:00:00Z\">"
                + "<saml:Issuer>test</saml:Issuer>"
                + "</samlp:LogoutResponse>"
                + "<samlp:Response"
                + " ID=\"R_unsigned\""
                + " Version=\"2.0\""
                + " IssueInstant=\"2025-01-01T00:00:00Z\">"
                + "<saml:Issuer>attacker</saml:Issuer>"
                + "</samlp:Response>"
                + "</samlp:ArtifactResponse>";

        Document doc = DocumentUtil.getDocument(xml);

        Element logoutResponse = (Element) doc.getElementsByTagNameNS(PROTOCOL_NS, "LogoutResponse").item(0);
        logoutResponse.setIdAttribute("ID", true);

        XMLSignatureUtil.sign(
                logoutResponse,
                logoutResponse.getFirstChild(),
                "test-key",
                keyPair,
                DigestMethod.SHA256,
                SignatureMethod.RSA_SHA256,
                "#LR_signed",
                CanonicalizationMethod.EXCLUSIVE);

        doc = DocumentUtil.getDocument(DocumentUtil.getDocumentAsString(doc));

        SAML2Signature saml2Signature = new SAML2Signature();
        HardcodedKeyLocator locator = new HardcodedKeyLocator(keyPair.getPublic());

        assertThat(saml2Signature.validate(doc, locator), is(false));
    }

    /**
     * Builds a document shaped like a real Shibboleth artifact response and
     * signs only the <em>nested</em> {@code samlp:Response} sub-element:

     * This matches the document shape that Shibboleth IdP 5.x produces by
     * default and that triggered the bug.
     */
    private Document buildArtifactResponseWithSignedNestedResponse() throws Exception {

        String xml = "<samlp:ArtifactResponse"
                + " xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
                + " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\""
                + " ID=\"" + "AR_outer_1" + "\""
                + " Version=\"2.0\""
                + " IssueInstant=\"2025-01-01T00:00:00Z\">"
                + "<samlp:Status/>"
                + "<samlp:Response"
                + " ID=\"" + "R_inner_1" + "\""
                + " Version=\"2.0\""
                + " IssueInstant=\"2025-01-01T00:00:00Z\">"
                + "<saml:Issuer>test</saml:Issuer>"
                + "</samlp:Response>"
                + "</samlp:ArtifactResponse>";

        Document doc = DocumentUtil.getDocument(xml);

        Element nestedResponse = (Element) doc.getElementsByTagNameNS(PROTOCOL_NS, "Response").item(0);
        nestedResponse.setIdAttribute("ID", true);

        XMLSignatureUtil.sign(
                nestedResponse,
                nestedResponse.getFirstChild(),
                "test-key",
                keyPair,
                DigestMethod.SHA256,
                SignatureMethod.RSA_SHA256,
                "#" + "R_inner_1",
                CanonicalizationMethod.EXCLUSIVE);

        return DocumentUtil.getDocument(DocumentUtil.getDocumentAsString(doc));
    }
}
