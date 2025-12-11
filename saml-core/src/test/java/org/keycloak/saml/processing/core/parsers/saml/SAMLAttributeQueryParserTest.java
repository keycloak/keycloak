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


import java.io.InputStream;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AttributeQueryType;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test class for SAML AttributeQuery parser.
 *
 * @author hmlnarik
 */
public class SAMLAttributeQueryParserTest {

    private SAMLParser parser;

    @Before
    public void initParser() {
        this.parser = new SAMLParser();
    }

    @Test(timeout = 2000000)
    public void testSaml20AttributeQuery() throws Exception {
        try (InputStream is = SAMLAttributeQueryParserTest.class.getResourceAsStream("saml20-attributequery.xml")) {
            Object parsedObject = parser.parse(is);
            assertThat(parsedObject, instanceOf(AttributeQueryType.class));

            AttributeQueryType query = (AttributeQueryType) parsedObject;
            assertThat(query.getSignature(), nullValue());
            assertThat(query.getConsent(), nullValue());
            assertThat(query.getIssuer(), not(nullValue()));
            assertThat(query.getIssuer().getValue(), is("https://sp/"));

            NameIDType nameId = (NameIDType) query.getSubject().getSubType().getBaseID();
            assertThat(nameId.getValue(), is("CN=trscavo@uiuc.edu,OU=User,O=NCSA-TEST,C=US"));
        }
    }

    @Test(timeout = 2000)
    public void testSaml20AttributeQueryWithExtension() throws Exception {
        try (InputStream is = SAMLAttributeQueryParserTest.class.getResourceAsStream("saml20-attributequery-with-extension.xml")) {
            Object parsedObject = parser.parse(is);
            assertThat(parsedObject, instanceOf(AttributeQueryType.class));

            AttributeQueryType query = (AttributeQueryType) parsedObject;
            assertThat(query.getSignature(), nullValue());
            assertThat(query.getConsent(), nullValue());
            assertThat(query.getIssuer(), not(nullValue()));
            assertThat(query.getIssuer().getValue(), is("https://sp/"));

            NameIDType nameId = (NameIDType) query.getSubject().getSubType().getBaseID();
            assertThat(nameId.getValue(), is("CN=trscavo@uiuc.edu,OU=User,O=NCSA-TEST,C=US"));

            assertThat(query.getExtensions(), not(nullValue()));
            assertThat(query.getExtensions().getAny().size(), is(1));
            assertThat(query.getExtensions().getAny().get(0), instanceOf(Element.class));
            Element el = (Element) query.getExtensions().getAny().get(0);
            assertThat(el.getLocalName(), is("KeyInfo"));
            assertThat(el.getNamespaceURI(), is("urn:keycloak:ext:key:1.0"));
            assertThat(el.getAttribute("MessageSigningKeyId"), is("FJ86GcF3jTbNLOco4NvZkUCIUmfYCqoqtOQeMfbhNlE"));
        }
    }
}
