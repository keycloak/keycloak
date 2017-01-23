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
package org.keycloak.saml.processing.core.parsers.saml;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;

import java.io.InputStream;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;

import org.junit.Before;
import org.w3c.dom.Element;

/**
 * Test class for SAML parser.
 *
 * TODO: Add further tests.
 *
 * @author hmlnarik
 */
public class SAMLParserTest {

    private SAMLParser parser;

    @Before
    public void initParser() {
        this.parser = new SAMLParser();
    }

    @Test
    public void testSaml20EncryptedAssertionsSignedReceivedWithRedirectBinding() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("saml20-encrypted-signed-redirect-response.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(ResponseType.class));

            ResponseType resp = (ResponseType) parsedObject;
            assertThat(resp.getSignature(), nullValue());
            assertThat(resp.getConsent(), nullValue());
            assertThat(resp.getIssuer(), not(nullValue()));
            assertThat(resp.getIssuer().getValue(), is("http://localhost:8081/auth/realms/saml-demo"));

            assertThat(resp.getExtensions(), not(nullValue()));
            assertThat(resp.getExtensions().getAny().size(), is(1));
            assertThat(resp.getExtensions().getAny().get(0), instanceOf(Element.class));
            Element el = (Element) resp.getExtensions().getAny().get(0);
            assertThat(el.getLocalName(), is("KeyInfo"));
            assertThat(el.getNamespaceURI(), is("urn:keycloak:ext:key:1.0"));
            assertThat(el.hasAttribute("MessageSigningKeyId"), is(true));
            assertThat(el.getAttribute("MessageSigningKeyId"), is("FJ86GcF3jTbNLOco4NvZkUCIUmfYCqoqtOQeMfbhNlE"));

            assertThat(resp.getAssertions(), not(nullValue()));
            assertThat(resp.getAssertions().size(), is(1));
        }
    }

    @Test
    public void testSaml20EncryptedAssertionsSignedTwoExtensionsReceivedWithRedirectBinding() throws Exception {
        Element el;

        try (InputStream st = SAMLParserTest.class.getResourceAsStream("saml20-encrypted-signed-redirect-response-two-extensions.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(ResponseType.class));

            ResponseType resp = (ResponseType) parsedObject;
            assertThat(resp.getSignature(), nullValue());
            assertThat(resp.getConsent(), nullValue());
            assertThat(resp.getIssuer(), not(nullValue()));
            assertThat(resp.getIssuer().getValue(), is("http://localhost:8081/auth/realms/saml-demo"));

            assertThat(resp.getExtensions(), not(nullValue()));
            assertThat(resp.getExtensions().getAny().size(), is(2));
            assertThat(resp.getExtensions().getAny().get(0), instanceOf(Element.class));
            el = (Element) resp.getExtensions().getAny().get(0);
            assertThat(el.getLocalName(), is("KeyInfo"));
            assertThat(el.getNamespaceURI(), is("urn:keycloak:ext:key:1.0"));
            assertThat(el.hasAttribute("MessageSigningKeyId"), is(true));
            assertThat(el.getAttribute("MessageSigningKeyId"), is("FJ86GcF3jTbNLOco4NvZkUCIUmfYCqoqtOQeMfbhNlE"));
            assertThat(resp.getExtensions().getAny().get(1), instanceOf(Element.class));
            el = (Element) resp.getExtensions().getAny().get(1);
            assertThat(el.getLocalName(), is("ever"));
            assertThat(el.getNamespaceURI(), is("urn:keycloak:ext:what:1.0"));
            assertThat(el.hasAttribute("what"), is(true));
            assertThat(el.getAttribute("what"), is("ever"));

            assertThat(resp.getAssertions(), not(nullValue()));
            assertThat(resp.getAssertions().size(), is(1));
        }
    }

    @Test
    public void testSaml20AuthnResponseNonAsciiNameDefaultUtf8() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("KEYCLOAK-3971-utf-8-no-header-authnresponse.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(ResponseType.class));

            ResponseType rt = (ResponseType) parsedObject;
            assertThat(rt.getAssertions().size(), is(1));
            final AssertionType assertion = rt.getAssertions().get(0).getAssertion();
            assertThat(assertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

            NameIDType nameId = (NameIDType) assertion.getSubject().getSubType().getBaseID();
            assertThat(nameId.getValue(), is("roàåאבčéèíñòøöùüßåäöü汉字"));
        }
    }

    @Test
    public void testSaml20AuthnResponseNonAsciiNameDefaultLatin2() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("KEYCLOAK-3971-8859-2-in-header-authnresponse.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(ResponseType.class));

            ResponseType rt = (ResponseType) parsedObject;
            assertThat(rt.getAssertions().size(), is(1));
            final AssertionType assertion = rt.getAssertions().get(0).getAssertion();
            assertThat(assertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

            NameIDType nameId = (NameIDType) assertion.getSubject().getSubType().getBaseID();
            assertThat(nameId.getValue(), is("ročéíöüßäöü"));
        }
    }

    @Test
    public void testSaml20PostLogoutRequest() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("saml20-signed-logout-request.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(LogoutRequestType.class));
        }
    }

    @Test
    public void testOrganizationDetailsMetadata() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("KEYCLOAK-4040-sharefile-metadata.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(EntityDescriptorType.class));
        }
    }

    @Test
    public void testAttributeProfileMetadata() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("KEYCLOAK-4236-AttributeProfile-element.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(EntityDescriptorType.class));
        }
    }
}
