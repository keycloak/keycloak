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


import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import java.io.InputStream;

import org.junit.Test;

import org.junit.Before;
import org.w3c.dom.Element;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test class for SAML AuthNRequest parser.
 *
 * @author hmlnarik
 */
public class SAMLAuthNRequestParserTest {

    private SAMLParser parser;

    @Before
    public void initParser() {
        this.parser = new SAMLParser();
    }

    @Test(timeout = 2000)
    public void testSaml20AttributeQuery() throws Exception {
        try (InputStream is = SAMLAuthNRequestParserTest.class.getResourceAsStream("saml20-authnrequest.xml")) {
            Object parsedObject = parser.parse(is);
            assertThat(parsedObject, instanceOf(AuthnRequestType.class));

            AuthnRequestType req = (AuthnRequestType) parsedObject;
            assertThat(req.getSignature(), nullValue());
            assertThat(req.getConsent(), nullValue());
            assertThat(req.getIssuer(), not(nullValue()));
            assertThat(req.getIssuer().getValue(), is("https://sp/"));

            assertThat(req.getNameIDPolicy().getFormat().toString(), is("urn:oasis:names:tc:SAML:2.0:nameid-format:transient"));
        }
    }

    @Test(timeout = 2000)
    public void testSaml20AttributeQueryWithExtension() throws Exception {
        try (InputStream is = SAMLAuthNRequestParserTest.class.getResourceAsStream("saml20-authnrequest-with-extension.xml")) {
            Object parsedObject = parser.parse(is);
            assertThat(parsedObject, instanceOf(AuthnRequestType.class));

            AuthnRequestType req = (AuthnRequestType) parsedObject;
            assertThat(req.getSignature(), nullValue());
            assertThat(req.getConsent(), nullValue());
            assertThat(req.getIssuer(), not(nullValue()));
            assertThat(req.getIssuer().getValue(), is("https://sp/"));

            assertThat(req.getNameIDPolicy().getFormat().toString(), is("urn:oasis:names:tc:SAML:2.0:nameid-format:transient"));

            assertThat(req.getExtensions(), not(nullValue()));
            assertThat(req.getExtensions().getAny().size(), is(2));
            assertThat(req.getExtensions().getAny().get(0), instanceOf(Element.class));
            assertThat(req.getExtensions().getAny().get(1), instanceOf(Element.class));
            Element el = (Element) req.getExtensions().getAny().get(0);
            assertThat(el.getLocalName(), is("KeyInfo"));
            assertThat(el.getNamespaceURI(), is("urn:keycloak:ext:key:1.0"));
            assertThat(el.getAttribute("MessageSigningKeyId"), is("FJ86GcF3jTbNLOco4NvZkUCIUmfYCqoqtOQeMfbhNlE"));
        }
    }
}
