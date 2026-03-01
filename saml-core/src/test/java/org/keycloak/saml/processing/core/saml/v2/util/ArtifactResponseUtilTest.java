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

package org.keycloak.saml.processing.core.saml.v2.util;

import java.io.InputStream;
import java.util.Optional;

import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;

import junit.framework.TestCase;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Thibault Morin (https://tmorin.github.io)
 */
public class ArtifactResponseUtilTest extends TestCase {

    public void testConvertResponseToString() throws Exception {
        InputStream artifactResponseAsInputStream = ArtifactResponseUtilTest.class.getResourceAsStream(
                "saml20-artifact-response-assertion-signed.xml"
        );
        Assert.assertNotNull(artifactResponseAsInputStream);

        InputStream expectedResponseAsInputStream = ArtifactResponseUtilTest.class.getResourceAsStream(
                "saml20-response-assertion-signed.xml"
        );
        Assert.assertNotNull(expectedResponseAsInputStream);
        String expectedResponseAsString = new String(expectedResponseAsInputStream.readAllBytes());

        // transform the InputStream to a SAMLDocumentHolder to get the Document as implemented in org.keycloak.broker.saml.SAMLEndpoint
        SAMLDocumentHolder saml2ObjectFromDocument = SAML2Request.getSAML2ObjectFromStream(artifactResponseAsInputStream);
        Assert.assertNotNull(saml2ObjectFromDocument);

        // the value shall be present
        Optional<String> optionalString = ArtifactResponseUtil.convertResponseToString(saml2ObjectFromDocument.getSamlDocument());
        Assert.assertTrue(optionalString.isPresent());

        // the value shall be equal to the expected value
        Assert.assertEquals(expectedResponseAsString, optionalString.get());
    }

    public void testNodeToString() throws Exception {
        String documentAstring = "<foo><bar>VALUE</bar></foo>";

        // create a document
        Document document = DocumentUtil.getDocument(documentAstring);
        Assert.assertNotNull(document);

        // transform the document to a string
        String transformedDocument = ArtifactResponseUtil.nodeToString(document);

        // assert the transformed document is equal to the original document
        Assert.assertEquals(documentAstring, transformedDocument);
    }

    public void testExtractResponseElement() throws Exception {
        InputStream artifactResponseAsInputStream = ArtifactResponseUtilTest.class.getResourceAsStream(
                "saml20-artifact-response-assertion-signed.xml"
        );
        Assert.assertNotNull(artifactResponseAsInputStream);

        // transform the InputStream to a SAMLDocumentHolder to get the Document as implemented in org.keycloak.broker.saml.SAMLEndpoint
        SAMLDocumentHolder saml2ObjectFromDocument = SAML2Request.getSAML2ObjectFromStream(artifactResponseAsInputStream);

        // the element shall be present
        Optional<Element> optionalElement = ArtifactResponseUtil.extractResponseElement(saml2ObjectFromDocument.getSamlDocument());
        Assert.assertTrue(optionalElement.isPresent());
    }

    public void testExtractResponseElementWhenResponseNotFound() throws Exception {
        InputStream artifactResponseAsInputStream = ArtifactResponseUtilTest.class.getResourceAsStream(
                "saml20-artifact-response-assertion-signed.xml"
        );
        Assert.assertNotNull(artifactResponseAsInputStream);

        // transform the InputStream to a SAMLDocumentHolder to get the Document as implemented in org.keycloak.broker.saml.SAMLEndpoint
        SAMLDocumentHolder saml2ObjectFromDocument = SAML2Request.getSAML2ObjectFromStream(artifactResponseAsInputStream);

        // get the Response element and remove it
        Node responseNode = saml2ObjectFromDocument.getSamlDocument().getElementsByTagNameNS(
                "urn:oasis:names:tc:SAML:2.0:protocol",
                "Response"
        ).item(0);
        responseNode.getParentNode().removeChild(responseNode);

        // the element shall be absent
        Optional<Element> optionalElement = ArtifactResponseUtil.extractResponseElement(saml2ObjectFromDocument.getSamlDocument());
        Assert.assertTrue(optionalElement.isEmpty());
    }
}
