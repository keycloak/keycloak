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

import org.keycloak.common.util.Base64;
import org.keycloak.common.util.DerUtils;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;

import java.io.InputStream;
import java.security.PrivateKey;

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
 * To create SAML XML for use in the test, use for instance https://www.samltool.com, {@link #PRIVATE_KEY} and
 * {@link #PUBLIC_CERT}.
 *
 * TODO: Add further tests.
 *
 * @author hmlnarik
 */
public class SAMLParserTest {

    private static final String PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAOJDgeiUFOERg6N2qUeLjkE7HDzg4h2m4hMG8WDYLhuAm6k2pq+1SkRDvisD67y2AvXXFz069Vw5TMDnpbmGpigEbdr+YT7As+YpVhxI1nSwTzzEAuL2Ywun1FNfOFAeBt6hzQ/veJ+rc3D3BSgjeY/yiZNen36T3BD8pi3HojErAgMBAAECgYBWmtpVqKCZSXfmkJvYy70GkNaNItLJ4L+14rlvhS+YzVBHo6iHps+nc3qNwnFwCQb3DH5TrIaP50rOp5wSeEyOWk7fOJeAwM4Vsc3d+av/Iu/WwNDyAFW3gGO19YvccfGvEbToMPtppOt47UDK26xfibCwUFwEGg0hGc0gcVQWMQJBAPl8YnETSjI0wZuaSdkUp2/FtHEAZa1yFPtPx7CVCpUG53frKOAx2t0N7AQ2vQUNPqOas8gDGr5O5J+l9tjOD0MCQQDoK+asMuMnSClm/RZRFIckcToFwjfgNQY/AKN31k705jJr+3+er3VlODL7dpnF0X2mVDjiXIp4hH9K0qfW+TP5AkAQOIMaAPwQ+Zcg684jXBFq1freYf06YrF0iYJdO8N9Xv6LsHFu6i7lsnMG7xwpCOxqrLNFrNX/S5fXvW2oOPWLAkEAiTu547tIjaWX42ph0JdDsoTC+Tht8rck9ASam3Evxo5y62UDcHbh+2yWphDaoBVOIgzSeuqcZtRasY2G7AjtcQJAEiXYeHB5+lancDDkBhH8KKdl1+FORRB9kJ4gxTwrjLwprWFjdatMb3O+xJGss+KnVUa/THRa5CRX4pHh93711Q==";

    /**
     * The public certificate that corresponds to {@link #PRIVATE_KEY}.
     */
    private static final String PUBLIC_CERT = "MIICXjCCAcegAwIBAgIBADANBgkqhkiG9w0BAQ0FADBLMQswCQYDVQQGEwJubzERMA8GA1UECAwIVmVzdGZvbGQxEzARBgNVBAoMCkV4YW1wbGVPcmcxFDASBgNVBAMMC2V4YW1wbGUub3JnMCAXDTE3MDIyNzEwNTY0MFoYDzIxMTcwMjAzMTA1NjQwWjBLMQswCQYDVQQGEwJubzERMA8GA1UECAwIVmVzdGZvbGQxEzARBgNVBAoMCkV4YW1wbGVPcmcxFDASBgNVBAMMC2V4YW1wbGUub3JnMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDiQ4HolBThEYOjdqlHi45BOxw84OIdpuITBvFg2C4bgJupNqavtUpEQ74rA+u8tgL11xc9OvVcOUzA56W5hqYoBG3a/mE+wLPmKVYcSNZ0sE88xALi9mMLp9RTXzhQHgbeoc0P73ifq3Nw9wUoI3mP8omTXp9+k9wQ/KYtx6IxKwIDAQABo1AwTjAdBgNVHQ4EFgQUzWjvSL0O2V2B2N9G1qARQiVgv3QwHwYDVR0jBBgwFoAUzWjvSL0O2V2B2N9G1qARQiVgv3QwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQ0FAAOBgQBgvKTTcLGlF0KvnIGxkzdaFeYewQtsQZHgnUt+JGKge0CyUU+QPVFhrH19b7fjKeykq/avm/2hku4mKaPyRYpvU9Gm+ARz67rs/vr0ZgJFk00TGI6ssGhdFd7iCptuIh5lEvWk1hD5LzThOI3isq0gK2tTbhafQOkKa45IwbOQ8Q==";

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
    public void testSaml20EncryptedAssertionWithNewlines() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("KEYCLOAK-4489-encrypted-assertion-with-newlines.xml")) {
            Object parsedObject = parser.parse(st);
            assertThat(parsedObject, instanceOf(ResponseType.class));

            ResponseType resp = (ResponseType) parsedObject;
            assertThat(resp.getAssertions().size(), is(1));

            ResponseType.RTChoiceType rtChoiceType = resp.getAssertions().get(0);
            assertNull(rtChoiceType.getAssertion());
            assertNotNull(rtChoiceType.getEncryptedAssertion());

            PrivateKey privateKey = DerUtils.decodePrivateKey(Base64.decode(PRIVATE_KEY));
            ResponseType rtWithDecryptedAssertion = AssertionUtil.decryptAssertion(resp, privateKey);

            rtChoiceType = rtWithDecryptedAssertion.getAssertions().get(0);
            assertNotNull(rtChoiceType.getAssertion());
            assertNull(rtChoiceType.getEncryptedAssertion());
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
