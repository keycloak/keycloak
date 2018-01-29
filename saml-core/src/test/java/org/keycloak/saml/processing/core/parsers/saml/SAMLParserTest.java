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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.keycloak.common.util.Base64;
import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.EncryptedAssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType;
import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509CertificateType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509DataType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import java.net.URI;
import java.util.List;
import org.hamcrest.Matcher;
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initParser() {
        this.parser = SAMLParser.getInstance();
    }

    private <T> T assertParsed(String fileName, Class<T> expectedType) throws IOException, ParsingException, ConfigurationException, ProcessingException {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream(fileName)) {
            Object parsedObject;
            if (SAML2Object.class.isAssignableFrom(expectedType)) {
                parsedObject = new SAML2Response().getSAML2ObjectFromStream(st);
            } else {
                parsedObject = parser.parse(st);
            }
            assertThat(parsedObject, instanceOf(expectedType));

            return expectedType.cast(parsedObject);
        }
    }

    @Test
    public void testSaml20EncryptedAssertionsSignedReceivedWithRedirectBinding() throws Exception {
        ResponseType resp = assertParsed("saml20-encrypted-signed-redirect-response.xml", ResponseType.class);

        assertThat(resp.getSignature(), nullValue());
        assertThat(resp.getConsent(), nullValue());
        assertThat(resp.getIssuer(), not(nullValue()));
        assertThat(resp.getIssuer().getValue(), is("http://localhost:8081/auth/realms/saml-demo"));
        assertThat(resp.getIssuer().getFormat(), is(JBossSAMLURIConstants.NAMEID_FORMAT_ENTITY.getUri()));


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
        final EncryptedAssertionType ea = resp.getAssertions().get(0).getEncryptedAssertion();
        assertThat(ea, notNullValue());
        assertThat(ea.getEncryptedElement(), notNullValue());
        assertThat(ea.getEncryptedElement().getLocalName(), is("EncryptedAssertion"));
    }

    @Test
    public void testSaml20EncryptedAssertion() throws Exception {
        EncryptedAssertionType ea = assertParsed("saml20-assertion-encrypted.xml", EncryptedAssertionType.class);

        assertThat(ea, notNullValue());
        assertThat(ea.getEncryptedElement(), notNullValue());
        assertThat(ea.getEncryptedElement().getLocalName(), is("EncryptedAssertion"));
    }

    @Test
    public void testSaml20EncryptedAssertionWithNewlines() throws Exception {
        ResponseType resp = assertParsed("KEYCLOAK-4489-encrypted-assertion-with-newlines.xml", ResponseType.class);
        assertThat(resp.getAssertions().size(), is(1));

        ResponseType.RTChoiceType rtChoiceType = resp.getAssertions().get(0);
        assertNull(rtChoiceType.getAssertion());
        assertNotNull(rtChoiceType.getEncryptedAssertion());

        PrivateKey privateKey = DerUtils.decodePrivateKey(Base64.decode(PRIVATE_KEY));
        AssertionUtil.decryptAssertion(resp, privateKey);

        rtChoiceType = resp.getAssertions().get(0);
        assertNotNull(rtChoiceType.getAssertion());
        assertNull(rtChoiceType.getEncryptedAssertion());
    }

    @Test
    public void testSaml20EncryptedAssertionsSignedTwoExtensionsReceivedWithRedirectBinding() throws Exception {
        Element el;

        ResponseType resp = assertParsed("saml20-encrypted-signed-redirect-response-two-extensions.xml", ResponseType.class);
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

    @Test
    public void testSaml20AuthnResponseNonAsciiNameDefaultUtf8() throws Exception {
        ResponseType rt = assertParsed("KEYCLOAK-3971-utf-8-no-header-authnresponse.xml", ResponseType.class);

        assertThat(rt.getAssertions().size(), is(1));
        final AssertionType assertion = rt.getAssertions().get(0).getAssertion();
        assertThat(assertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

        NameIDType nameId = (NameIDType) assertion.getSubject().getSubType().getBaseID();
        assertThat(nameId.getValue(), is("roàåאבčéèíñòøöùüßåäöü汉字"));

        assertThat(assertion.getSubject().getConfirmation(), hasSize(1));
        assertThat(assertion.getSubject().getConfirmation().get(0).getSubjectConfirmationData(), notNullValue());
        assertThat(assertion.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getAnyType(), instanceOf(KeyInfoType.class));

        KeyInfoType kit = (KeyInfoType) assertion.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getAnyType();
        assertThat(kit.getContent(), hasItem(instanceOf(X509DataType.class)));
        X509DataType rsaKit = (X509DataType) kit.getContent().get(0);
        assertThat(rsaKit.getDataObjects(), hasSize(1));
        assertThat(rsaKit.getDataObjects().get(0), instanceOf(X509CertificateType.class));
    }

    @Test
    public void testSaml20AuthnResponseNonAsciiNameDefaultLatin2() throws Exception {
        ResponseType rt = assertParsed("KEYCLOAK-3971-8859-2-in-header-authnresponse.xml", ResponseType.class);
        assertThat(rt.getAssertions().size(), is(1));
        final AssertionType assertion = rt.getAssertions().get(0).getAssertion();
        final SubjectType subject = assertion.getSubject();

        assertThat(subject.getConfirmation(), hasSize(1));
        SubjectConfirmationType confirmation = subject.getConfirmation().get(0);
        assertThat(confirmation.getMethod(), is(JBossSAMLURIConstants.SUBJECT_CONFIRMATION_BEARER.get()));
        assertThat(confirmation.getSubjectConfirmationData(), notNullValue());
        assertThat(confirmation.getSubjectConfirmationData().getInResponseTo(), is("ID_cc0ff6f7-b481-4c98-9a79-481d50958290"));
        assertThat(confirmation.getSubjectConfirmationData().getRecipient(), is("http://localhost:8080/sales-post-sig/saml"));

        assertThat(subject.getSubType().getBaseID(), instanceOf(NameIDType.class));
        NameIDType nameId = (NameIDType) subject.getSubType().getBaseID();
        assertThat(nameId.getValue(), is("ročéíöüßäöü"));
    }

    @Test
    public void testSaml20PostLogoutRequest() throws Exception {
        assertParsed("saml20-signed-logout-request.xml", LogoutRequestType.class);
    }

    @Test
    public void testOrganizationDetailsMetadata() throws Exception {
        assertParsed("KEYCLOAK-4040-sharefile-metadata.xml", EntityDescriptorType.class);
    }

    @Test
    public void testSaml20MetadataEntityDescriptorIdP() throws Exception {
        assertParsed("saml20-entity-descriptor-idp.xml", EntityDescriptorType.class);
    }

    @Test
    public void testSaml20MetadataEntityDescriptorSP() throws Exception {
        assertParsed("saml20-entity-descriptor-sp.xml", EntityDescriptorType.class);
    }

    @Test
    public void testSaml20MetadataEntityDescriptorAdfsIdP() throws Exception {
        assertParsed("KEYCLOAK-4809-IdPMetadata_test.xml", EntityDescriptorType.class);
    }

    @Test
    public void testAttributeProfileMetadata() throws Exception {
        assertParsed("KEYCLOAK-4236-AttributeProfile-element.xml", EntityDescriptorType.class);
    }

    @Test
    public void testEmptyAttributeValue() throws Exception {
        ResponseType resp = assertParsed("KEYCLOAK-4790-Empty-attribute-value.xml", ResponseType.class);

        assertThat(resp.getAssertions(), hasSize(1));
        final AssertionType a = resp.getAssertions().get(0).getAssertion();
        assertThat(a, notNullValue());

        assertThat(a.getAttributeStatements(), hasSize(1));
        final List<ASTChoiceType> attributes = a.getAttributeStatements().iterator().next().getAttributes();
        assertThat(attributes, hasSize(3));
        assertThat(attributes, everyItem(notNullValue(ASTChoiceType.class)));

        final AttributeType attr0 = attributes.get(0).getAttribute();
        final AttributeType attr1 = attributes.get(1).getAttribute();
        final AttributeType attr2 = attributes.get(2).getAttribute();

        assertThat(attr0.getName(), is("urn:oid:0.9.2342.19200300.100.1.2"));
        assertThat(attr0.getAttributeValue(), hasSize(1));
        assertThat(attr0.getAttributeValue().get(0), instanceOf(String.class));
        assertThat((String) attr0.getAttributeValue().get(0), is(""));

        assertThat(attr1.getName(), is("urn:oid:0.9.2342.19200300.100.1.3"));
        assertThat(attr1.getAttributeValue(), hasSize(1));
        assertThat(attr1.getAttributeValue().get(0), instanceOf(String.class));
        assertThat((String) attr1.getAttributeValue().get(0), is("aa"));

        assertThat(attr2.getName(), is("urn:oid:0.9.2342.19200300.100.1.4"));
        assertThat(attr2.getAttributeValue(), hasSize(1));
        assertThat(attr2.getAttributeValue().get(0), instanceOf(String.class));
        assertThat((String) attr2.getAttributeValue().get(0), is(""));
    }

    @Test
    public void testEmptyAttributeValueLast() throws Exception {
        assertParsed("KEYCLOAK-4790-Empty-attribute-value-last.xml", ResponseType.class);
    }

    @Test
    public void testAuthnRequest() throws Exception {
        AuthnRequestType req = assertParsed("saml20-authnrequest.xml", AuthnRequestType.class);

        assertThat(req.getRequestedAuthnContext(), notNullValue());
        assertThat(req.getRequestedAuthnContext().getAuthnContextClassRef(), hasItem(is("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")));
        assertThat(req.getRequestedAuthnContext().getAuthnContextDeclRef(), hasItem(is("urn:kc:SAML:2.0:ac:ref:demo:decl")));
    }

    @Test
    public void testAuthnRequestInvalidPerXsdWithValidationDisabled() throws Exception {
        AuthnRequestType req = assertParsed("saml20-authnrequest-invalid-per-xsd.xml", AuthnRequestType.class);
    }

    @Test
    public void testAuthnRequestInvalidPerXsdWithValidationEnabled() throws Exception {
        try {
            thrown.expect(ProcessingException.class);

            System.setProperty("picketlink.schema.validate", "true");
            AuthnRequestType req = assertParsed("saml20-authnrequest-invalid-per-xsd.xml", AuthnRequestType.class);
        } finally {
            System.clearProperty("picketlink.schema.validate");
        }
    }

    @Test
    public void testAuthnRequestInvalidNamespace() throws Exception {
        thrown.expect(ParsingException.class);
        thrown.expectMessage(containsString("Unknown Start Element"));

        assertParsed("saml20-authnrequest-invalid-namespace.xml", AuthnRequestType.class);
    }

    @Test
    public void testAuthnRequestScoping() throws Exception {
        assertParsed("KEYCLOAK-6109-authnrequest-scoping.xml", AuthnRequestType.class);
    }

    @Test
    public void testLogoutResponseStatusDetail() throws Exception {
        StatusResponseType resp = assertParsed("saml20-logout-response-status-detail.xml", StatusResponseType.class);

        assertThat(resp.getIssuer(), notNullValue());
        assertThat(resp.getIssuer().getValue(), is("http://idp.example.com/metadata.php"));
        assertThat(resp.getIssuer().getFormat(), is(JBossSAMLURIConstants.NAMEID_FORMAT_ENTITY.getUri()));

        assertThat(resp.getStatus(), notNullValue());

        assertThat(resp.getStatus().getStatusDetail(), notNullValue());
        assertThat(resp.getStatus().getStatusDetail().getAny(), notNullValue());
        assertThat(resp.getStatus().getStatusDetail().getAny().size(), is(2));

        assertThat(resp.getStatus().getStatusCode(), notNullValue());
        assertThat(resp.getStatus().getStatusCode().getValue(), is(URI.create("urn:oasis:names:tc:SAML:2.0:status:Responder")));

        assertThat(resp.getStatus().getStatusCode().getStatusCode(), nullValue());
    }

    @Test
    public void testLogoutResponseSimpleStatus() throws Exception {
        StatusResponseType resp = assertParsed("saml20-logout-response-status.xml", StatusResponseType.class);

        assertThat(resp.getStatus(), notNullValue());

        assertThat(resp.getStatus().getStatusMessage(), is("Status Message"));

        assertThat(resp.getStatus().getStatusCode(), notNullValue());
        assertThat(resp.getStatus().getStatusCode().getValue(), is(URI.create("urn:oasis:names:tc:SAML:2.0:status:Responder")));

        assertThat(resp.getStatus().getStatusCode().getStatusCode(), nullValue());
    }

    @Test
    public void testLogoutResponseNestedStatus() throws Exception {
        StatusResponseType resp = assertParsed("saml20-logout-response-nested-status.xml", StatusResponseType.class);

        assertThat(resp.getStatus(), notNullValue());

        assertThat(resp.getStatus().getStatusCode(), notNullValue());
        assertThat(resp.getStatus().getStatusCode().getValue(), is(URI.create("urn:oasis:names:tc:SAML:2.0:status:Responder")));

        assertThat(resp.getStatus().getStatusCode().getStatusCode(), notNullValue());
        assertThat(resp.getStatus().getStatusCode().getStatusCode().getValue(), is(URI.create("urn:oasis:names:tc:SAML:2.0:status:AuthnFailed")));

        assertThat(resp.getStatus().getStatusCode().getStatusCode().getStatusCode(), nullValue());
    }

    @Test
    public void testLogoutResponseDeepNestedStatus() throws Exception {
        StatusResponseType resp = assertParsed("saml20-logout-response-nested-status-deep.xml", StatusResponseType.class);

        assertThat(resp.getStatus(), notNullValue());

        assertThat(resp.getStatus().getStatusDetail(), notNullValue());
        assertThat(resp.getStatus().getStatusDetail().getAny(), notNullValue());
        assertThat(resp.getStatus().getStatusDetail().getAny().size(), is(2));

        assertThat(resp.getStatus().getStatusCode(), notNullValue());
        assertThat(resp.getStatus().getStatusCode().getValue(), is(URI.create("urn:oasis:names:tc:SAML:2.0:status:Responder")));

        assertThat(resp.getStatus().getStatusCode().getStatusCode(), notNullValue());
        assertThat(resp.getStatus().getStatusCode().getStatusCode().getValue(), is(URI.create("urn:oasis:names:tc:SAML:2.0:status:AuthnFailed")));

        assertThat(resp.getStatus().getStatusCode().getStatusCode().getStatusCode(), notNullValue());
        assertThat(resp.getStatus().getStatusCode().getStatusCode().getStatusCode().getValue(), is(URI.create("urn:oasis:names:tc:SAML:2.0:status:VersionMismatch")));
    }

    @Test
    public void testSaml20AssertionContents() throws Exception {
        AssertionType a = assertParsed("saml20-assertion-example.xml", AssertionType.class);

        assertThat(a.getSubject().getConfirmation(), hasSize(1));
        assertThat(a.getSubject().getConfirmation().get(0).getSubjectConfirmationData(), notNullValue());
        assertThat(a.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getAnyType(), instanceOf(KeyInfoType.class));

        KeyInfoType kit = (KeyInfoType) a.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getAnyType();
        assertThat(kit.getContent(), hasItem(instanceOf(RSAKeyValueType.class)));
        RSAKeyValueType rsaKit = (RSAKeyValueType) kit.getContent().get(0);
        assertThat(rsaKit.getModulus(), notNullValue());
        assertThat(rsaKit.getExponent(), notNullValue());

        assertThat(a.getStatements(), containsInAnyOrder(instanceOf(AuthnStatementType.class), instanceOf(AttributeStatementType.class)));
        for (StatementAbstractType statement : a.getStatements()) {
            if (statement instanceof AuthnStatementType) {
                AuthnStatementType as = (AuthnStatementType) statement;
                assertThat(as.getSessionNotOnOrAfter(), notNullValue());
                assertThat(as.getSessionNotOnOrAfter(), is(XMLTimeUtil.parse("2009-06-17T18:55:10.738Z")));

                final AuthnContextType ac = as.getAuthnContext();
                assertThat(ac, notNullValue());
                assertThat(ac.getSequence(), notNullValue());

                assertThat(ac.getSequence().getClassRef().getValue(), is(JBossSAMLURIConstants.AC_UNSPECIFIED.getUri()));

                assertThat(ac.getSequence(), notNullValue());
                assertThat(ac.getSequence().getAuthnContextDecl(), nullValue());
            }
        }
    }

    @Test
    public void testSaml20AssertionDsaKey() throws Exception {
        AssertionType a = assertParsed("saml20-assertion-dsakey.xml", AssertionType.class);

        assertThat(a.getSubject().getConfirmation(), hasSize(1));
        assertThat(a.getSubject().getConfirmation().get(0).getSubjectConfirmationData(), notNullValue());
        assertThat(a.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getAnyType(), instanceOf(KeyInfoType.class));

        KeyInfoType kit = (KeyInfoType) a.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getAnyType();
        assertThat(kit.getContent(), hasItem(instanceOf(DSAKeyValueType.class)));
        DSAKeyValueType rsaKit = (DSAKeyValueType) kit.getContent().get(0);
        assertThat(rsaKit.getG(), notNullValue());
        assertThat(rsaKit.getJ(), nullValue());
        assertThat(rsaKit.getP(), notNullValue());
        assertThat(rsaKit.getQ(), notNullValue());
        assertThat(rsaKit.getY(), notNullValue());
    }

    @Test
    public void testSaml20AssertionsAnyTypeAttributeValue() throws Exception {

        String[] xmlSamples = {
                "saml20-assertion-anytype-attribute-value.xml",
                "saml20-assertion-example.xml"
        };

        for (String fileName: xmlSamples) {
            try (InputStream st = SAMLParserTest.class.getResourceAsStream(fileName)) {
                Object parsedObject = parser.parse(st);
                assertThat("Problem detected in " + fileName + " sample.", parsedObject, instanceOf(AssertionType.class));
                checkCheckParsedResult(fileName, (AssertionType)parsedObject);
            } catch (Exception e) {
                throw new Exception("Problem detected in " + fileName + " sample.", e);
            }
        }
    }

    private void checkCheckParsedResult(String fileName, AssertionType assertion) throws Exception {
        AttributeStatementType attributeStatementType = assertion.getAttributeStatements().iterator().next();
        if ("saml20-assertion-anytype-attribute-value.xml".equals(fileName)) {
            assertTrue("There has to be 3 attributes", attributeStatementType.getAttributes().size() == 3);
            for (AttributeStatementType.ASTChoiceType choiceType: attributeStatementType.getAttributes()) {
                AttributeType attr = choiceType.getAttribute();
                String attrName = attr.getName();
                String attrValueStatement = "unexpected value of attribute " + attrName + " of " + fileName;
                String attrTypeStatement = "unexpected type of attribute " + attrName + " of " + fileName;
                // test selected attributes
                if (attrName.equals("attr:type:string")) {
                    assertEquals(attrValueStatement, attr.getAttributeValue().get(0), "CITIZEN");
                } else if (attrName.equals("attr:notype:string")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertEquals(attrValueStatement, value, "CITIZEN");
                } else if (attrName.equals("attr:notype:element")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertThat(attrValueStatement, value, containsString("hospitaal x"));
                    value = (String)attr.getAttributeValue().get(1);
                    assertThat(attrValueStatement, value, containsString("hopital x"));
                }
            }
        } else if ("saml20-assertion-example.xml".equals(fileName)) {
            assertThat("There has to be 9 attributes", attributeStatementType.getAttributes().size(), is(9));
            for (AttributeStatementType.ASTChoiceType choiceType: attributeStatementType.getAttributes()) {
                AttributeType attr = choiceType.getAttribute();
                String attrName = attr.getName();
                String attrValueStatement = "unexpected value of attribute " + attrName + " of " + fileName;
                String attrTypeStatement = "unexpected type of attribute " + attrName + " of " + fileName;
                // test selected attributes
                if (attrName.equals("portal_id")) {
                    assertEquals(attrValueStatement, attr.getAttributeValue().get(0), "060D00000000SHZ");
                } else if (attrName.equals("organization_id")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertThat(attrValueStatement, value, containsString("<n3:stuff xmlns:n3=\"ftp://example.org\">00DD0000000F7L5</n3:stuff>"));
                } else if (attrName.equals("has_sub_organization")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertThat(attrValueStatement, value, containsString("true"));
                } else if (attrName.equals("anytype_test")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertThat(attrValueStatement, value, containsString("<elem2>val2</elem2>"));
                } else if (attrName.equals("anytype_no_xml_test")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertEquals(attrValueStatement, value, "value_no_xml");
                } else if (attrName.equals("logouturl")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertEquals(attrValueStatement, value, "http://www.salesforce.com/security/del_auth/SsoLogoutPage.html");
                } else if (attrName.equals("nil_value_attribute")) {
                    assertNull(attrValueStatement, attr.getAttributeValue().get(0));
                } else if (attrName.equals("status")) {
                    assertThat(attrTypeStatement, attr.getAttributeValue().get(0), instanceOf(String.class));
                    String value = (String)attr.getAttributeValue().get(0);
                    assertThat(attrValueStatement, value, containsString("<status><code><status>XYZ</status></code></status>"));
                }
            }
        } else {
            throw new RuntimeException("test error: wrong file name to check");
        }
    }

    @Test(expected = ParsingException.class)
    public void testSaml20AssertionsNil1() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("saml20-assertion-nil-wrong-1.xml")) {
            parser.parse(st);
        }
    }

    @Test(expected = ParsingException.class)
    public void testSaml20AssertionsNil2() throws Exception {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream("saml20-assertion-nil-wrong-2.xml")) {
            parser.parse(st);
        }
    }

    @Test
    public void testSaml20AssertionsMissingId() throws Exception {
        try (InputStream st = removeAttribute("saml20-assertion-example.xml", "ID")) {
            thrown.expect(ParsingException.class);
            thrown.expectMessage(endsWith("Required attribute missing: ID"));
            parser.parse(st);
        }
    }

    @Test
    public void testSaml20AssertionsMissingVersion() throws Exception {
        try (InputStream st = removeAttribute("saml20-assertion-example.xml", "Version")) {
            thrown.expect(ParsingException.class);
            thrown.expectMessage(endsWith("Required attribute missing: Version"));
            parser.parse(st);
        }
    }

    @Test
    public void testSaml20AssertionsWrongVersion() throws Exception {
        try (InputStream st = updateAttribute("saml20-assertion-example.xml", "Version", "1.1")) {
            thrown.expect(ParsingException.class);
            thrown.expectMessage(endsWith("Assertion Version required to be \"2.0\""));
            parser.parse(st);
        }
    }

    @Test
    public void testSaml20AssertionsMissingIssueInstance() throws Exception {
        try (InputStream st = removeAttribute("saml20-assertion-example.xml", "IssueInstant")) {
            thrown.expect(ParsingException.class);
            thrown.expectMessage(endsWith("Required attribute missing: IssueInstant"));
            parser.parse(st);
        }
    }

    @Test
    public void testSaml20AssertionsAdviceTag() throws Exception {
        Matcher<String>[] ATTR_NAME = new Matcher[] {
            is("portal_id"),
            is("organization_id"),
            is("status"),
            is("has_sub_organization"),
            is("anytype_test"),
            is("anytype_no_xml_test"),
            is("ssostartpage"),
            is("logouturl"),
            is("nil_value_attribute"),
        };

        Matcher<List<Object>>[] ATTR_VALUE = new Matcher[] {
            contains(is("060D00000000SHZ")),
            contains(is("<n1:elem2 xmlns:n1=\"http://example.net\" xml:lang=\"en\"><n3:stuff xmlns:n3=\"ftp://example.org\">00DD0000000F7L5</n3:stuff></n1:elem2>")),
            contains(is("<status><code><status>XYZ</status></code></status>")),
            contains(is("true")),
            contains(is("<elem1 atttr1=\"en\"><elem2>val2</elem2></elem1>")),
            contains(is("value_no_xml")),
            contains(is("http://www.salesforce.com/security/saml/saml20-gen.jsp")),
            contains(is("http://www.salesforce.com/security/del_auth/SsoLogoutPage.html")),
            contains(nullValue()),
        };

        AssertionType a = assertParsed("saml20-assertion-advice.xml", AssertionType.class);

        assertThat(a.getStatements(), containsInAnyOrder(instanceOf(AuthnStatementType.class), instanceOf(AttributeStatementType.class)));
        for (StatementAbstractType statement : a.getStatements()) {
            if (statement instanceof AuthnStatementType) {
                AuthnStatementType as = (AuthnStatementType) statement;
                final AuthnContextType ac = as.getAuthnContext();
                assertThat(ac, notNullValue());
                assertThat(ac.getSequence(), notNullValue());

                assertThat(ac.getSequence().getClassRef().getValue(), is(JBossSAMLURIConstants.AC_UNSPECIFIED.getUri()));
                
                assertThat(ac.getSequence(), notNullValue());
                assertThat(ac.getSequence().getAuthnContextDecl(), notNullValue());
                assertThat(ac.getSequence().getAuthnContextDecl().getValue(), instanceOf(Element.class));
                final Element el = (Element) ac.getSequence().getAuthnContextDecl().getValue();
                assertThat(el.getTextContent(), is("auth.weak"));
            } else {
                AttributeStatementType as = (AttributeStatementType) statement;
                assertThat(as.getAttributes(), hasSize(9));
                for (int i = 0; i < as.getAttributes().size(); i ++) {
                    AttributeType attr = as.getAttributes().get(i).getAttribute();
                    assertThat(attr.getName(), ATTR_NAME[i]);
                    assertThat(attr.getAttributeValue(), ATTR_VALUE[i]);
                }
            }
        }

        assertThat(a.getConditions().getConditions(), contains(instanceOf(AudienceRestrictionType.class)));
    }

    private InputStream removeAttribute(String resourceName, String attribute) throws IOException {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream(resourceName)) {
            String str = StreamUtil.readString(st, StandardCharsets.UTF_8);
            String processed = str.replaceAll(attribute + "=\"[^\"]+\"", "");
            return new ByteArrayInputStream(processed.getBytes());
        }
    }

    private InputStream updateAttribute(String resourceName, String attribute, String newValue) throws IOException {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream(resourceName)) {
            String str = StreamUtil.readString(st, StandardCharsets.UTF_8);
            String processed = str.replaceAll("(" + attribute + "=)\"[^\"]+\"", "$1\"" + newValue + "\"");
            return new ByteArrayInputStream(processed.getBytes());
        }
    }


}
