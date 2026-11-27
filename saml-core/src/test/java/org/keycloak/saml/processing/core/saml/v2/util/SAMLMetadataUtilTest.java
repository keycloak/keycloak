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
package org.keycloak.saml.processing.core.saml.v2.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.crypto.dsig.spec.XPathFilterParameterSpec;

import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

public class SAMLMetadataUtilTest {

    private static final String METADATA_NS = "urn:oasis:names:tc:SAML:2.0:metadata";
    private static final String TRUSTED_ENTITY = "https://idp.example.org/trusted";
    private static final String OTHER_ENTITY = "https://idp.example.org/other";
    private static final String NOT_SIGNED = "The SAML metadata is not signed";
    private static final String NOT_VALID = "The signature of the SAML metadata is not valid for the trusted keys";
    private static final XMLSignatureFactory FAC = XMLSignatureFactory.getInstance("DOM");

    private static KeyPair trustedKey;
    private static KeyPair otherKey;
    private static KeyLocator trustedKeys;

    @BeforeClass
    public static void generateKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        trustedKey = gen.generateKeyPair();
        otherKey = gen.generateKeyPair();
        trustedKeys = new HardcodedKeyLocator(trustedKey.getPublic());
    }

    @Test
    public void signedEntityDescriptorIsParsed() throws Exception {
        Document doc = DocumentUtil.getDocument(entity("_trusted", TRUSTED_ENTITY));
        sign(doc.getDocumentElement(), trustedKey);

        assertParsed(doc, TRUSTED_ENTITY);
    }

    @Test
    public void entityDescriptorSignedAsWholeDocumentIsParsed() throws Exception {
        Document doc = DocumentUtil.getDocument(entity(null, TRUSTED_ENTITY));
        XMLSignatureUtil.sign(doc, "test-key", trustedKey, DigestMethod.SHA256, SignatureMethod.RSA_SHA256, "",
                CanonicalizationMethod.EXCLUSIVE);

        assertParsed(doc, TRUSTED_ENTITY);
    }

    @Test
    public void signedEntitiesDescriptorIsParsed() throws Exception {
        Document doc = DocumentUtil.getDocument(entities("_entities", entity("_trusted", TRUSTED_ENTITY), entity("_other", OTHER_ENTITY)));
        sign(doc.getDocumentElement(), trustedKey);

        assertParsed(doc, TRUSTED_ENTITY);
    }

    @Test
    public void signedFirstEntityDescriptorOfEntitiesDescriptorIsParsed() throws Exception {
        Document doc = DocumentUtil.getDocument(entities("_entities", entity("_trusted", TRUSTED_ENTITY), entity("_other", OTHER_ENTITY)));
        sign(entityElement(doc, 0), trustedKey);
        // the signature of the root element is not trusted, but the one of the entity is
        sign(doc.getDocumentElement(), otherKey);

        assertParsed(doc, TRUSTED_ENTITY);
    }

    @Test
    public void unsignedMetadataIsRejected() throws Exception {
        assertRejected(DocumentUtil.getDocument(entity("_trusted", TRUSTED_ENTITY)), trustedKeys, NOT_SIGNED);
        assertRejected(DocumentUtil.getDocument(entities("_entities", entity("_trusted", TRUSTED_ENTITY))), trustedKeys, NOT_SIGNED);
    }

    @Test
    public void metadataSignedWithAnotherKeyIsRejected() throws Exception {
        Document doc = DocumentUtil.getDocument(entity("_other", OTHER_ENTITY));
        // the signature is valid for the key that is included in its KeyInfo
        sign(doc.getDocumentElement(), otherKey);

        assertRejected(doc, trustedKeys, NOT_VALID);
        assertRejected(doc, new HardcodedKeyLocator(Collections.emptyList()), NOT_VALID);
    }

    @Test
    public void modifiedMetadataIsRejected() throws Exception {
        Document doc = DocumentUtil.getDocument(entity("_trusted", TRUSTED_ENTITY));
        sign(doc.getDocumentElement(), trustedKey);
        keyName(doc.getDocumentElement()).setTextContent(OTHER_ENTITY);

        assertRejected(doc, trustedKeys, NOT_VALID);
    }

    @Test
    public void signatureOfAnotherEntityDescriptorIsRejected() throws Exception {
        // the entity that is used is the first one, and the signed one is the second
        Document doc = DocumentUtil.getDocument(entities("_entities", entity("_other", OTHER_ENTITY), entity("_trusted", TRUSTED_ENTITY)));
        sign(entityElement(doc, 1), trustedKey);

        assertRejected(doc, trustedKeys, NOT_SIGNED);
    }

    @Test
    public void signatureMovedFromAnotherEntityDescriptorIsRejected() throws Exception {
        // both entities have the same ID and the signature of the second is copied into the first
        Document doc = DocumentUtil.getDocument(entities("_entities", entity("_trusted", OTHER_ENTITY), entity("_trusted", TRUSTED_ENTITY)));
        Element trusted = entityElement(doc, 1);
        sign(trusted, trustedKey);
        Element other = entityElement(doc, 0);
        other.insertBefore(signature(trusted).cloneNode(true), other.getFirstChild());

        assertRejected(doc, trustedKeys, NOT_VALID);
    }

    @Test
    public void signatureThatIsNotEnvelopedInTheEntityDescriptorIsRejected() throws Exception {
        // the signature of the entity is moved into the root element
        Document doc = DocumentUtil.getDocument(entities("_entities", entity("_trusted", TRUSTED_ENTITY)));
        Element trusted = entityElement(doc, 0);
        sign(trusted, trustedKey);
        doc.getDocumentElement().insertBefore(signature(trusted), trusted);

        assertRejected(doc, trustedKeys, NOT_VALID);
    }

    @Test
    public void signatureOfTheWholeDocumentInTheEntityDescriptorIsRejected() throws Exception {
        // the signature of the whole document is moved from the root element into the entity
        Document doc = DocumentUtil.getDocument(entities("_entities", entity("_trusted", TRUSTED_ENTITY)));
        XMLSignatureUtil.sign(doc, "test-key", trustedKey, DigestMethod.SHA256, SignatureMethod.RSA_SHA256, "",
                CanonicalizationMethod.EXCLUSIVE);
        Element trusted = entityElement(doc, 0);
        trusted.insertBefore(signature(doc.getDocumentElement()), trusted.getFirstChild());

        assertRejected(doc, trustedKeys, NOT_VALID);
    }

    @Test
    public void signatureWithOtherTransformsIsRejected() throws Exception {
        Document doc = DocumentUtil.getDocument(entity("_trusted", TRUSTED_ENTITY));
        Element root = doc.getDocumentElement();
        // the keys of the entity are left out of the signature with an XPath transform, and then modified
        sign(root, trustedKey, List.of(reference(root, FAC.newTransform(Transform.XPATH,
                new XPathFilterParameterSpec("not(ancestor-or-self::md:KeyDescriptor)", Map.of("md", METADATA_NS))))));
        keyName(root).setTextContent(OTHER_ENTITY);

        // the signature itself is still valid, but it does not cover the whole entity
        doc = DocumentUtil.getDocument(DocumentUtil.getDocumentAsString(doc));
        assertThat(AssertionUtil.isSignatureValid(doc.getDocumentElement(), trustedKeys), is(true));
        assertRejected(doc, trustedKeys, NOT_VALID);
    }

    @Test
    public void signatureWithSeveralReferencesIsRejected() throws Exception {
        Document doc = DocumentUtil.getDocument(entity("_trusted", TRUSTED_ENTITY));
        Element root = doc.getDocumentElement();
        sign(root, trustedKey, List.of(reference(root), reference(root)));

        // the signature itself is valid, but it does not follow the signature profile of the metadata
        doc = DocumentUtil.getDocument(DocumentUtil.getDocumentAsString(doc));
        assertThat(AssertionUtil.isSignatureValid(doc.getDocumentElement(), trustedKeys), is(true));
        assertRejected(doc, trustedKeys, NOT_VALID);
    }

    private static void assertParsed(Document doc, String expectedEntityId) throws Exception {
        EntityDescriptorType entity = SAMLMetadataUtil.parseEntityDescriptorType(DocumentUtil.getDocumentAsString(doc), trustedKeys);

        assertThat(entity.getEntityID(), is(expectedEntityId));
        Element keyInfo = SAMLMetadataUtil.locateIDPSSODescriptorType(entity).getKeyDescriptor().get(0).getKeyInfo();
        assertThat(keyInfo.getTextContent(), is(expectedEntityId));
    }

    private static void assertRejected(Document doc, KeyLocator keyLocator, String expectedMessage) throws Exception {
        String descriptor = DocumentUtil.getDocumentAsString(doc);

        VerificationException e = assertThrows(VerificationException.class,
                () -> SAMLMetadataUtil.parseEntityDescriptorType(descriptor, keyLocator));
        assertThat(e.getMessage(), is(expectedMessage));
    }

    private static void sign(Element element, KeyPair keyPair) throws Exception {
        element.setIdAttribute("ID", true);
        XMLSignatureUtil.sign(element, element.getFirstChild(), "test-key", keyPair, DigestMethod.SHA256,
                SignatureMethod.RSA_SHA256, "#" + element.getAttribute("ID"), CanonicalizationMethod.EXCLUSIVE);
    }

    // signs the element with references that are not created by the signature utility class
    private static void sign(Element element, KeyPair keyPair, List<Reference> references) throws Exception {
        SignedInfo signedInfo = FAC.newSignedInfo(
                FAC.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                FAC.newSignatureMethod(SignatureMethod.RSA_SHA256, null), references);
        element.setIdAttribute("ID", true);
        FAC.newXMLSignature(signedInfo, null).sign(new DOMSignContext(keyPair.getPrivate(), element, element.getFirstChild()));
    }

    private static Reference reference(Element element, Transform... otherTransforms) throws Exception {
        List<Transform> transforms = new ArrayList<>();
        transforms.add(FAC.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.addAll(List.of(otherTransforms));
        transforms.add(FAC.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));
        return FAC.newReference("#" + element.getAttribute("ID"), FAC.newDigestMethod(DigestMethod.SHA256, null), transforms, null, null);
    }

    private static Element entityElement(Document doc, int index) {
        return (Element) doc.getElementsByTagNameNS(METADATA_NS, "EntityDescriptor").item(index);
    }

    // the name of the key that the entity publishes, not the one in the signature
    private static Element keyName(Element entity) {
        Element keyDescriptor = (Element) entity.getElementsByTagNameNS(METADATA_NS, "KeyDescriptor").item(0);
        return (Element) keyDescriptor.getElementsByTagNameNS(XMLSignature.XMLNS, "KeyName").item(0);
    }

    private static Element signature(Element element) {
        return DocumentUtil.getDirectChildElement(element, XMLSignature.XMLNS, "Signature");
    }

    private static String entities(String id, String... entities) {
        return "<md:EntitiesDescriptor xmlns:md=\"" + METADATA_NS + "\" ID=\"" + id + "\">"
                + String.join("", entities)
                + "</md:EntitiesDescriptor>";
    }

    // the name of the key in the entity is the entity ID to identify where the keys are read from
    private static String entity(String id, String entityId) {
        return "<md:EntityDescriptor xmlns:md=\"" + METADATA_NS + "\" xmlns:ds=\"" + XMLSignature.XMLNS + "\""
                + (id == null ? "" : " ID=\"" + id + "\"") + " entityID=\"" + entityId + "\">"
                + "<md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<md:KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:KeyName>" + entityId + "</ds:KeyName></ds:KeyInfo></md:KeyDescriptor>"
                + "<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"" + entityId + "\"/>"
                + "</md:IDPSSODescriptor>"
                + "</md:EntityDescriptor>";
    }
}
