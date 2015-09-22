/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.wsfed.common;

import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.common.util.Base64Url;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.ws.rs.core.Response;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by dbarentine on 9/2/2015.
 */
public class TestHelpers {
    public static void assertFormAction(Document doc, String method, String destination) throws XPathExpressionException {
        Node node = assertNode(doc, "/HTML/BODY/FORM");

        assertEquals(method, node.getAttributes().getNamedItem("METHOD").getNodeValue());
        assertEquals(destination, node.getAttributes().getNamedItem("ACTION").getNodeValue());
    }

    public static void assertInputNode(Document doc, String name, String value) throws XPathExpressionException {
        assertEquals(value, getInputNodeValue(doc, name));
    }

    public static String getInputNodeValue(Document doc, String name) throws XPathExpressionException {
        Node node = assertNode(doc, String.format("/HTML/BODY/FORM/INPUT[@NAME='%s']", name));
        return node.getAttributes().getNamedItem("VALUE").getNodeValue();
    }

    public static Node assertNode(Document doc, String xPath) throws XPathExpressionException {
        return assertNode(doc, xPath, null);
    }

    public static Node assertNode(Document doc, String xPath, NamespaceContext nsContext) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();

        if(nsContext != null) {
            xpath.setNamespaceContext(nsContext);
        }

        XPathExpression xPathExpression = xpath.compile(xPath);
        Node node = (Node) xPathExpression.evaluate(doc, XPathConstants.NODE);

        assertNotNull(node);
        return node;
    }

    public static void assertInputNodeMissing(Document doc, String name) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression = xpath.compile(String.format("/HTML/BODY/FORM/INPUT[@NAME='%s']", name));
        Node node = (Node) xPathExpression.evaluate(doc, XPathConstants.NODE);

        assertNull(node);
    }

    public static void assertTokenType(String wsfedResponse, String tokenType) throws Exception {
        Document doc = DocumentUtil.getDocument(wsfedResponse);
        Node node = assertNode(doc, "/wst:RequestSecurityTokenResponseCollection/wst:RequestSecurityTokenResponse/wst:TokenType", new WSFedNamespaceContext());
        assertEquals(tokenType, node.getTextContent());
    }

    public static class WSFedNamespaceContext implements NamespaceContext {
        private String defaultNamespace = XMLConstants.NULL_NS_URI;

        public WSFedNamespaceContext() {
        }

        public WSFedNamespaceContext(String defaultNamespace) {
            this.defaultNamespace = defaultNamespace;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) throw new NullPointerException("Invalid Namespace Prefix");
            else if ("wst".equals(prefix))
                return "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
            else if ("wsse".equals(prefix))
                return "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
            else if ("saml".equals(prefix))
                return "urn:oasis:names:tc:SAML:2.0:assertion";
            else if ("wsu".equals(prefix))
                return "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
            else if ("wsp".equals(prefix))
                return "http://schemas.xmlsoap.org/ws/2004/09/policy";
            else if ("wsa".equals(prefix))
                return "http://www.w3.org/2005/08/addressing";
            else if ("dsig".equals(prefix))
                return "http://www.w3.org/2000/09/xmldsig#";
            else if ("fed".equals(prefix))
                return "http://docs.oasis-open.org/wsfed/federation/200706";
            else if ("xsi".equals(prefix))
                return "http://www.w3.org/2001/XMLSchema-instance";
            else
                return defaultNamespace;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return null;
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }
    }

    public static void assertErrorPage(LoginFormsProvider loginFormsProvider, String message) {
        verify(loginFormsProvider, times(1)).setError(eq(message));
        verify(loginFormsProvider, times(1)).createErrorPage();
    }

    public static Document responseToDocument(Response response) throws Exception {
        assertNotNull(response);

        String form = (String)response.getEntity();
        assertNotNull(form);

        Document doc = DocumentUtil.getDocument(form);
        assertNotNull(doc);

        return doc;
    }

    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    public static MockHelper getMockHelper() {
        MockHelper mockHelper = new MockHelper();
        mockHelper.setBaseUri("https://dib.software.dell.com/auth")
                .setClientId("https://clientid")
                .setEmail("first.last@somedomain.com")
                .setUserName("username")
                .setRealmName("ExampleRealm")
                .setAccessCodeLifespan(1000)
                .setAccessTokenLifespan(2000);

        return mockHelper;
    }

    public static String getThumbPrint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return Base64Url.encode(digest);
    }

    public static JsonWebToken assertToken(String encodedToken, MockHelper mockHelper) {
        assertNotNull(encodedToken);

        try {
            JWSInput jws = new JWSInput(encodedToken);
            assertTrue(RSAProvider.verify(jws, mockHelper.getRealm().getPublicKey()));

            JsonWebToken token = jws.readJsonContent(JsonWebToken.class);

            String[] aud = token.getAudience();
            String iss = token.getIssuer();

            assertTrue("Wrong audience from token.", aud[0].equals(mockHelper.getClientId()));
            assertTrue("Token is no longer valid", token.isActive());

            String trustedIssuers = String.format("%s/realms/%s", mockHelper.getBaseUri(), mockHelper.getRealmName());

            if (trustedIssuers != null) {
                String[] issuers = trustedIssuers.split(",");

                for (String trustedIssuer : issuers) {
                    if (iss != null && iss.equals(trustedIssuer.trim())) {
                        return token;
                    }
                }

                fail("Wrong issuer from token. Got: " + iss + " expected: " + String.format("%s/realms/%s", mockHelper.getBaseUri(), mockHelper.getRealmName()));
            }
            return token;
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not decode token.", e);
        }
    }
}
