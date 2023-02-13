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


import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import java.io.InputStream;

import org.junit.Test;

import org.junit.Before;
import org.w3c.dom.Element;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test class for SAML SLO parser.
 *
 * @author hmlnarik
 */
public class SAMLSloRequestParserTest {

    private SAMLParser parser;

    @Before
    public void initParser() {
        this.parser = new SAMLParser();
    }

    @Test(timeout = 2000)
    public void testSaml20SloResponseWithExtension() throws Exception {
        try (InputStream is = SAMLSloRequestParserTest.class.getResourceAsStream("KEYCLOAK-4552-saml20-aslo-response-via-extension.xml")) {
            Object parsedObject = parser.parse(is);
            assertThat(parsedObject, instanceOf(LogoutRequestType.class));

            LogoutRequestType resp = (LogoutRequestType) parsedObject;
            assertThat(resp.getSignature(), nullValue());
            assertThat(resp.getConsent(), nullValue());
            assertThat(resp.getIssuer(), not(nullValue()));
            assertThat(resp.getIssuer().getValue(), is("https://sp/"));

            NameIDType nameId = resp.getNameID();
            assertThat(nameId.getValue(), is("G-XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"));

            assertThat(resp.getExtensions(), not(nullValue()));
            assertThat(resp.getExtensions().getAny().size(), is(1));
            assertThat(resp.getExtensions().getAny().get(0), instanceOf(Element.class));
            Element el = (Element) resp.getExtensions().getAny().get(0);
            assertThat(el.getLocalName(), is("Asynchronous"));
            assertThat(el.getNamespaceURI(), is("urn:oasis:names:tc:SAML:2.0:protocol:ext:async-slo"));
        }
    }
}
