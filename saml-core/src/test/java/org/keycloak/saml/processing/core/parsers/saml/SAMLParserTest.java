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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
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
import org.keycloak.dom.saml.v2.metadata.AttributeAuthorityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.AuthnAuthorityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.dom.saml.v2.metadata.PDPDescriptorType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType;
import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509CertificateType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509DataType;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptionMethodType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Element;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Before
    public void initParser() {
        this.parser = SAMLParser.getInstance();
    }

    private <T> T assertParsed(String fileName, Class<T> expectedType) throws IOException, ParsingException, ConfigurationException, ProcessingException {
        try (InputStream st = SAMLParserTest.class.getResourceAsStream(fileName)) {
            Object parsedObject;
            if (SAML2Object.class.isAssignableFrom(expectedType)) {
                parsedObject = new SAML2Response().getSAML2ObjectFromStream(st);
            } else if (SAMLDocumentHolder.class.isAssignableFrom(expectedType)) {
                parsedObject = SAML2Request.getSAML2ObjectFromStream(st);
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
    public void testSaml20EncryptedId() throws Exception {
        ResponseType rt = assertParsed("saml20-encrypted-id-response.xml",  ResponseType.class);

        assertThat(rt, notNullValue());
        assertThat(rt.getAssertions(), notNullValue());
        assertThat(rt.getAssertions().size(), is(1));
        assertThat(rt.getAssertions().get(0).getAssertion().getSubject(), notNullValue());
        assertThat(rt.getAssertions().get(0).getAssertion().getSubject().getSubType(), notNullValue());
        assertThat(rt.getAssertions().get(0).getAssertion().getSubject().getSubType().getEncryptedID(), notNullValue());
    }

    @Test
    public void testSaml20EncryptedAssertionWithNewlines() throws Exception {
        SAMLDocumentHolder holder = assertParsed("KEYCLOAK-4489-encrypted-assertion-with-newlines.xml", SAMLDocumentHolder.class);
        assertThat(holder.getSamlObject(), instanceOf(ResponseType.class));
        ResponseType resp = (ResponseType) holder.getSamlObject();
        assertThat(resp.getAssertions().size(), is(1));

        ResponseType.RTChoiceType rtChoiceType = resp.getAssertions().get(0);
        assertNull(rtChoiceType.getAssertion());
        assertNotNull(rtChoiceType.getEncryptedAssertion());

        PrivateKey privateKey = DerUtils.decodePrivateKey(Base64.getDecoder().decode(PRIVATE_KEY));
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
    public void testProxyRestrictionTagHandling() throws Exception {
        assertParsed("KEYCLOAK-6412-response-with-proxy-restriction.xml", ResponseType.class);
    }

    @Test
    public void testSaml20MetadataEntityDescriptorIdP() throws Exception {
        EntityDescriptorType entityDescriptor = assertParsed("saml20-entity-descriptor-idp.xml", EntityDescriptorType.class);

        List<EntityDescriptorType.EDTChoiceType> descriptors = entityDescriptor.getChoiceType();
        assertThat(descriptors, hasSize(2));

        // IDPSSO descriptor
        IDPSSODescriptorType idpDescriptor = descriptors.get(0).getDescriptors().get(0).getIdpDescriptor();
        assertThat(idpDescriptor, is(notNullValue()));
        assertThat(idpDescriptor.isWantAuthnRequestsSigned(), is(true));
        assertThat(idpDescriptor.getProtocolSupportEnumeration(), contains("urn:oasis:names:tc:SAML:2.0:protocol"));

        // Key descriptor
        List<KeyDescriptorType> keyDescriptors = idpDescriptor.getKeyDescriptor();
        assertThat(keyDescriptors, hasSize(1));

        KeyDescriptorType signingKey = keyDescriptors.get(0);
        assertThat(signingKey.getUse(), is(KeyTypes.SIGNING));
        assertThat(signingKey.getEncryptionMethod(), is(emptyCollectionOf(EncryptionMethodType.class)));
        assertThat(signingKey.getKeyInfo().getElementsByTagName("ds:KeyName").item(0).getTextContent(), is("IdentityProvider.com SSO Key"));

        // Single logout services
        assertThat(idpDescriptor.getSingleLogoutService(), hasSize(2));
        EndpointType singleLS1 = idpDescriptor.getSingleLogoutService().get(0);
        assertThat(singleLS1.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:SOAP")));
        assertThat(singleLS1.getLocation(), is(URI.create("https://IdentityProvider.com/SAML/SLO/SOAP")));
        assertThat(singleLS1.getResponseLocation(), is(nullValue()));
        assertThat(singleLS1.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(singleLS1.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        EndpointType singleLS2 = idpDescriptor.getSingleLogoutService().get(1);
        assertThat(singleLS2.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect")));
        assertThat(singleLS2.getLocation(), is(URI.create("https://IdentityProvider.com/SAML/SLO/Browser")));
        assertThat(singleLS2.getResponseLocation(), is(URI.create("https://IdentityProvider.com/SAML/SLO/Response")));
        assertThat(singleLS2.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(singleLS2.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        // NameID
        assertThat(idpDescriptor.getNameIDFormat(),
                containsInAnyOrder("urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
                        "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
                        "urn:oasis:names:tc:SAML:2.0:nameid-format:transient"
                ));

        // Single sign on services
        assertThat(idpDescriptor.getSingleSignOnService(), hasSize(2));

        EndpointType singleSO1 = idpDescriptor.getSingleSignOnService().get(0);
        assertThat(singleSO1.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect")));
        assertThat(singleSO1.getLocation(), is(URI.create("https://IdentityProvider.com/SAML/SSO/Browser")));
        assertThat(singleSO1.getResponseLocation(), is(nullValue()));
        assertThat(singleSO1.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(singleSO1.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        EndpointType singleSO2 = idpDescriptor.getSingleSignOnService().get(1);
        assertThat(singleSO2.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")));
        assertThat(singleSO2.getLocation(), is(URI.create("https://IdentityProvider.com/SAML/SSO/Browser")));
        assertThat(singleSO2.getResponseLocation(), is(nullValue()));
        assertThat(singleSO2.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(singleSO2.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        // Attributes
        assertThat(idpDescriptor.getAttribute(), hasSize(2));

        AttributeType attr1 = idpDescriptor.getAttribute().get(0);
        assertThat(attr1.getNameFormat(), is("urn:oasis:names:tc:SAML:2.0:attrname-format:uri"));
        assertThat(attr1.getName(), is("urn:oid:1.3.6.1.4.1.5923.1.1.1.6"));
        assertThat(attr1.getFriendlyName(), is("eduPersonPrincipalName"));
        assertThat(attr1.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));
        assertThat(attr1.getAttributeValue(), is(emptyCollectionOf(Object.class)));

        AttributeType attr2 = idpDescriptor.getAttribute().get(1);
        assertThat(attr2.getNameFormat(), is("urn:oasis:names:tc:SAML:2.0:attrname-format:uri"));
        assertThat(attr2.getName(), is("urn:oid:1.3.6.1.4.1.5923.1.1.1.1"));
        assertThat(attr2.getFriendlyName(), is("eduPersonAffiliation"));
        assertThat(attr2.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));
        assertThat(attr2.getAttributeValue(), containsInAnyOrder((Object) "member", "student", "faculty", "employee", "staff"));

        // Organization
        assertThat(entityDescriptor.getOrganization().getOrganizationName(), hasSize(1));
        LocalizedNameType orgName = entityDescriptor.getOrganization().getOrganizationName().get(0);
        assertThat(orgName.getLang(), is("en"));
        assertThat(orgName.getValue(), is("Identity Providers R\n            US"));

        assertThat(entityDescriptor.getOrganization().getOrganizationDisplayName(), hasSize(1));
        LocalizedNameType orgDispName = entityDescriptor.getOrganization().getOrganizationDisplayName().get(0);
        assertThat(orgDispName.getLang(), is("en"));
        assertThat(orgDispName.getValue(), is("Identity Providers R US, a Division of Lerxst Corp."));

        assertThat(entityDescriptor.getOrganization().getOrganizationURL(), hasSize(1));
        LocalizedURIType orgURL = entityDescriptor.getOrganization().getOrganizationURL().get(0);
        assertThat(orgURL.getLang(), is("en"));
        assertThat(orgURL.getValue(), is(URI.create("https://IdentityProvider.com")));
    }

    @Test
    public void testSAML20MetadataEntityDescriptorAttrA() throws Exception{
        EntityDescriptorType entityDescriptor = assertParsed("saml20-entity-descriptor-idp.xml", EntityDescriptorType.class);

        List<EntityDescriptorType.EDTChoiceType> descriptors = entityDescriptor.getChoiceType();
        assertThat(descriptors, hasSize(2));

        AttributeAuthorityDescriptorType aaDescriptor = descriptors.get(1).getDescriptors().get(0).getAttribDescriptor();
        assertThat(aaDescriptor, is(notNullValue()));
        assertThat(aaDescriptor.getProtocolSupportEnumeration(), contains("urn:oasis:names:tc:SAML:2.0:protocol"));

        // Key descriptor
        List<KeyDescriptorType> keyDescriptors = aaDescriptor.getKeyDescriptor();
        assertThat(keyDescriptors, hasSize(1));

        KeyDescriptorType signingKey = keyDescriptors.get(0);
        assertThat(signingKey.getUse(), is(KeyTypes.SIGNING));
        assertThat(signingKey.getEncryptionMethod(), is(emptyCollectionOf(EncryptionMethodType.class)));
        assertThat(signingKey.getKeyInfo().getElementsByTagName("ds:KeyName").item(0).getTextContent(), is("IdentityProvider.com AA Key"));

        // Attribute service
        assertThat(aaDescriptor.getAttributeService(), hasSize(1));
        EndpointType attrServ = aaDescriptor.getAttributeService().get(0);
        assertThat(attrServ.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:SOAP")));
        assertThat(attrServ.getLocation(), is(URI.create("https://IdentityProvider.com/SAML/AA/SOAP")));
        assertThat(attrServ.getResponseLocation(), is(nullValue()));
        assertThat(attrServ.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(attrServ.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        // AssertionIDRequestService
        assertThat(aaDescriptor.getAssertionIDRequestService(), hasSize(1));
        EndpointType assertIDRServ = aaDescriptor.getAssertionIDRequestService().get(0);
        assertThat(assertIDRServ.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:URI")));
        assertThat(assertIDRServ.getLocation(), is(URI.create("https://IdentityProvider.com/SAML/AA/URI")));
        assertThat(assertIDRServ.getResponseLocation(), is(nullValue()));
        assertThat(assertIDRServ.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(assertIDRServ.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        // NameID
        assertThat(aaDescriptor.getNameIDFormat(),
                containsInAnyOrder("urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
                        "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
                        "urn:oasis:names:tc:SAML:2.0:nameid-format:transient"
                ));

        assertThat(aaDescriptor.getAttribute(), hasSize(2));

        AttributeType attr1 = aaDescriptor.getAttribute().get(0);
        assertThat(attr1.getNameFormat(), is("urn:oasis:names:tc:SAML:2.0:attrname-format:uri"));
        assertThat(attr1.getName(), is("urn:oid:1.3.6.1.4.1.5923.1.1.1.6"));
        assertThat(attr1.getFriendlyName(), is("eduPersonPrincipalName"));
        assertThat(attr1.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));
        assertThat(attr1.getAttributeValue(), is(emptyCollectionOf(Object.class)));

        AttributeType attr2 = aaDescriptor.getAttribute().get(1);
        assertThat(attr2.getNameFormat(), is("urn:oasis:names:tc:SAML:2.0:attrname-format:uri"));
        assertThat(attr2.getName(), is("urn:oid:1.3.6.1.4.1.5923.1.1.1.1"));
        assertThat(attr2.getFriendlyName(), is("eduPersonAffiliation"));
        assertThat(attr2.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));
        assertThat(attr2.getAttributeValue(), containsInAnyOrder((Object) "member", "student", "faculty", "employee", "staff"));
    }

    @Test
    public void testSaml20MetadataEntityDescriptorSP() throws Exception {
        EntityDescriptorType entityDescriptor = assertParsed("saml20-entity-descriptor-sp.xml", EntityDescriptorType.class);

        assertThat(entityDescriptor.getEntityID(), is("https://ServiceProvider.com/SAML"));
        assertThat(entityDescriptor.getValidUntil(), is(nullValue()));
        assertThat(entityDescriptor.getCacheDuration(), is(nullValue()));
        assertThat(entityDescriptor.getID(), is(nullValue()));

        assertThat(entityDescriptor.getExtensions(), is(nullValue()));

        List<EntityDescriptorType.EDTChoiceType> descriptors = entityDescriptor.getChoiceType();
        assertThat(descriptors, hasSize(1));

        // SP Descriptor
        SPSSODescriptorType spDescriptor = descriptors.get(0).getDescriptors().get(0).getSpDescriptor();
        assertThat(spDescriptor, is(notNullValue()));

        assertThat(spDescriptor.isAuthnRequestsSigned(), is(true));
        assertThat(spDescriptor.isWantAssertionsSigned(), is(false));
        assertThat(spDescriptor.getProtocolSupportEnumeration(), contains("urn:oasis:names:tc:SAML:2.0:protocol"));

        // Key descriptor
        List<KeyDescriptorType> keyDescriptors = spDescriptor.getKeyDescriptor();
        assertThat(keyDescriptors, hasSize(2));

        KeyDescriptorType signingKey = keyDescriptors.get(0);
        assertThat(signingKey.getUse(), is(KeyTypes.SIGNING));
        assertThat(signingKey.getEncryptionMethod(), is(emptyCollectionOf(EncryptionMethodType.class)));
        assertThat(signingKey.getKeyInfo().getElementsByTagName("ds:KeyName").item(0).getTextContent(), is("ServiceProvider.com SSO Key"));

        KeyDescriptorType encryptionKey = keyDescriptors.get(1);
        assertThat(encryptionKey.getUse(), is(KeyTypes.ENCRYPTION));
        assertThat(encryptionKey.getKeyInfo().getElementsByTagName("ds:KeyName").item(0).getTextContent(), is("ServiceProvider.com Encrypt Key"));

        List<EncryptionMethodType> encryptionMethods = encryptionKey.getEncryptionMethod();
        assertThat(encryptionMethods, Matchers.<EncryptionMethodType>hasSize(1));
        assertThat(encryptionMethods.get(0).getAlgorithm(), is("http://www.w3.org/2001/04/xmlenc#rsa-1_5"));
        assertThat(encryptionMethods.get(0).getEncryptionMethod(), is(nullValue()));

        // Single logout services
        assertThat(spDescriptor.getSingleLogoutService(), hasSize(2));
        EndpointType singleLS1 = spDescriptor.getSingleLogoutService().get(0);
        assertThat(singleLS1.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:SOAP")));
        assertThat(singleLS1.getLocation(), is(URI.create("https://ServiceProvider.com/SAML/SLO/SOAP")));
        assertThat(singleLS1.getResponseLocation(), is(nullValue()));
        assertThat(singleLS1.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(singleLS1.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        EndpointType singleLS2 = spDescriptor.getSingleLogoutService().get(1);
        assertThat(singleLS2.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect")));
        assertThat(singleLS2.getLocation(), is(URI.create("https://ServiceProvider.com/SAML/SLO/Browser")));
        assertThat(singleLS2.getResponseLocation(), is(URI.create("https://ServiceProvider.com/SAML/SLO/Response")));
        assertThat(singleLS2.getAny(), is(emptyCollectionOf(Object.class)));
        assertThat(singleLS2.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        // NameID
        assertThat(spDescriptor.getNameIDFormat(), contains("urn:oasis:names:tc:SAML:2.0:nameid-format:transient"));

        // Assertion consumer services
        List<IndexedEndpointType> assertionConsumerServices = spDescriptor.getAssertionConsumerService();
        assertThat(assertionConsumerServices, hasSize(2));

        IndexedEndpointType assertionCS1 = assertionConsumerServices.get(0);
        assertThat(assertionCS1.getIndex(), is(0));
        assertThat(assertionCS1.isIsDefault(), is(true));
        assertThat(assertionCS1.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact")));
        assertThat(assertionCS1.getLocation(), is(URI.create("https://ServiceProvider.com/SAML/SSO/Artifact")));
        assertThat(assertionCS1.getResponseLocation(), is(nullValue()));
        assertThat(assertionCS1.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        IndexedEndpointType assertionCS2 = assertionConsumerServices.get(1);
        assertThat(assertionCS2.getIndex(), is(1));
        assertThat(assertionCS2.isIsDefault(), is(nullValue()));
        assertThat(assertionCS2.getBinding(), is(URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")));
        assertThat(assertionCS2.getLocation(), is(URI.create("https://ServiceProvider.com/SAML/SSO/POST")));
        assertThat(assertionCS2.getResponseLocation(), is(nullValue()));
        assertThat(assertionCS2.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        // Attribute consuming services
        List<AttributeConsumingServiceType> attributeConsumingServices = spDescriptor.getAttributeConsumingService();
        assertThat(attributeConsumingServices, hasSize(1));

        AttributeConsumingServiceType attributeConsumingService = attributeConsumingServices.get(0);
        assertThat(attributeConsumingService.getIndex(), is(0));
        assertThat(attributeConsumingService.getServiceName(), hasSize(1));
        LocalizedNameType servName = attributeConsumingService.getServiceName().get(0);
        assertThat(servName.getLang(), is("en"));
        assertThat(servName.getValue(), is("Academic Journals R US"));
        assertThat(attributeConsumingService.getServiceDescription(), is(emptyCollectionOf(LocalizedNameType.class)));

        List<RequestedAttributeType> requestedAttributes = attributeConsumingService.getRequestedAttribute();
        assertThat(requestedAttributes, hasSize(1));

        // Requested attribute
        RequestedAttributeType requestedAttribute = requestedAttributes.get(0);
        assertThat(requestedAttribute.getNameFormat(), is("urn:oasis:names:tc:SAML:2.0:attrname-format:uri"));
        assertThat(requestedAttribute.getName(), is("urn:oid:1.3.6.1.4.1.5923.1.1.1.7"));
        assertThat(requestedAttribute.getFriendlyName(), is("eduPersonEntitlement"));

        assertThat(requestedAttribute.getAttributeValue(), hasSize(1));
        assertThat((String) requestedAttribute.getAttributeValue().get(0), is("https://ServiceProvider.com/entitlements/123456789"));

        assertThat(requestedAttribute.getOtherAttributes(), is(Collections.<QName, String>emptyMap()));

        // Organization
        assertThat(entityDescriptor.getOrganization().getOrganizationName(), hasSize(1));
        LocalizedNameType orgName = entityDescriptor.getOrganization().getOrganizationName().get(0);
        assertThat(orgName.getLang(), is("en"));
        assertThat(orgName.getValue(), is("Academic Journals R\n            US"));

        assertThat(entityDescriptor.getOrganization().getOrganizationDisplayName(), hasSize(1));
        LocalizedNameType orgDispName = entityDescriptor.getOrganization().getOrganizationDisplayName().get(0);
        assertThat(orgDispName.getLang(), is("en"));
        assertThat(orgDispName.getValue(), is("Academic Journals R US, a Division of Dirk Corp."));

        assertThat(entityDescriptor.getOrganization().getOrganizationURL(), hasSize(1));
        LocalizedURIType orgURL = entityDescriptor.getOrganization().getOrganizationURL().get(0);
        assertThat(orgURL.getLang(), is("en"));
        assertThat(orgURL.getValue(), is(URI.create("https://ServiceProvider.com")));
    }

    @Test
    public void testSaml20MetadataEntityDescriptorPDP() throws Exception {
        EntityDescriptorType descriptor = assertParsed("saml20-entity-descriptor-pdp.xml", EntityDescriptorType.class);

        assertThat(descriptor.getChoiceType(), Matchers.<EntityDescriptorType.EDTChoiceType>hasSize(1));
        assertThat(descriptor.getChoiceType().get(0).getDescriptors().get(0).getPdpDescriptor(), is(notNullValue()));

        PDPDescriptorType pdpDescriptor = descriptor.getChoiceType().get(0).getDescriptors().get(0).getPdpDescriptor();

        assertThat(pdpDescriptor.getKeyDescriptor(), Matchers.<KeyDescriptorType>hasSize(1));

        KeyDescriptorType keyDescriptorType = pdpDescriptor.getKeyDescriptor().get(0);
        assertThat(keyDescriptorType.getEncryptionMethod(), Matchers.<EncryptionMethodType>hasSize(1));

        EncryptionMethodType encryptionMethodType = keyDescriptorType.getEncryptionMethod().get(0);
        assertThat(encryptionMethodType.getAlgorithm(), is("http://www.example.com/"));

        EncryptionMethodType.EncryptionMethod encryptionMethod = encryptionMethodType.getEncryptionMethod();
        assertThat(encryptionMethod.getKeySize(), is(BigInteger.ONE));
        assertThat(encryptionMethod.getOAEPparams(), is("GpM7".getBytes()));

        // EndpointType parser already tested so we are not checking further
        assertThat(pdpDescriptor.getAuthzService(), Matchers.<EndpointType>hasSize(1));
        assertThat(pdpDescriptor.getAssertionIDRequestService(), Matchers.<EndpointType>hasSize(1));
    }

    @Test
    public void testSaml20MetadataEntityDescriptorAuthnAuthority() throws Exception {
        EntityDescriptorType descriptor = assertParsed("saml20-entity-descriptor-authn-authority.xml", EntityDescriptorType.class);

        assertThat(descriptor.getChoiceType(), Matchers.<EntityDescriptorType.EDTChoiceType>hasSize(1));
        assertThat(descriptor.getChoiceType().get(0).getDescriptors().get(0).getAuthnDescriptor(), is(notNullValue()));

        AuthnAuthorityDescriptorType authnDescriptor = descriptor.getChoiceType().get(0).getDescriptors().get(0).getAuthnDescriptor();

        assertThat(authnDescriptor.getAssertionIDRequestService(), hasSize(1));
        assertThat(authnDescriptor.getAuthnQueryService(),  hasSize(1));
        assertThat(authnDescriptor.getProtocolSupportEnumeration(), containsInAnyOrder("http://www.example.com/", "http://www.example2.com/"));
    }

    @Test
    public void testSaml20MetadataEntitiesDescriptor() throws Exception {
        EntitiesDescriptorType entities = assertParsed("saml20-entities-descriptor.xml", EntitiesDescriptorType.class);

        assertThat(entities.getName(), is("https://your-federation.org/metadata/federation-name.xml"));
        assertThat(entities.getID(), is(nullValue()));
        assertThat(entities.getCacheDuration(), is(nullValue()));
        assertThat(entities.getExtensions(), is(nullValue()));
        assertThat(entities.getSignature(), is(nullValue()));
        assertThat(entities.getValidUntil(), is(nullValue()));
        assertThat(entities.getEntityDescriptor(), hasSize(3));
        assertThat(entities.getEntityDescriptor().get(0), instanceOf(EntityDescriptorType.class));
        assertThat(entities.getEntityDescriptor().get(1), instanceOf(EntityDescriptorType.class));
        assertThat(entities.getEntityDescriptor().get(2), instanceOf(EntitiesDescriptorType.class));

        EntitiesDescriptorType nestedEntities = (EntitiesDescriptorType) entities.getEntityDescriptor().get(2);
        assertThat(nestedEntities.getEntityDescriptor(), hasSize(2));
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

    @Test //https://issues.jboss.org/browse/KEYCLOAK-7316
    public void testAuthnRequestOptionalIsPassive() throws Exception {
        AuthnRequestType req = assertParsed("KEYCLOAK-7316-noAtrributes.xml", AuthnRequestType.class);

        assertThat("Not null!", req.isIsPassive(), nullValue());
        assertThat("Not null!", req.isForceAuthn(), nullValue());

        req = assertParsed("KEYCLOAK-7316-withTrueAttributes.xml", AuthnRequestType.class);

        assertThat(req.isIsPassive(), notNullValue());
        assertTrue("Wrong value!", req.isIsPassive().booleanValue());
        assertThat(req.isForceAuthn(), notNullValue());
        assertTrue("Wrong value!", req.isForceAuthn().booleanValue());

        req = assertParsed("KEYCLOAK-7316-withFalseAttributes.xml", AuthnRequestType.class);

        assertThat(req.isIsPassive(), notNullValue());
        assertFalse("Wrong value!", req.isIsPassive().booleanValue());
        assertThat(req.isForceAuthn(), notNullValue());
        assertFalse("Wrong value!", req.isForceAuthn().booleanValue());
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
    public void testInvalidEndElement() throws Exception {
        thrown.expect(ParsingException.class);
        // see KEYCLOAK-7444
        thrown.expectMessage(containsString("NameIDFormat"));

        assertParsed("saml20-entity-descriptor-idp-invalid-end-element.xml", EntityDescriptorType.class);
    }

    @Test
    public void testMissingRequiredAttributeIDPSSODescriptorType()  throws Exception {
        testMissingAttribute("IDPSSODescriptorType", "protocolSupportEnumeration");
    }

    @Test
    public void testMissingRequiredAttributeSPSSODescriptorType()  throws Exception {
        testMissingAttribute("SPSSODescriptorType", "protocolSupportEnumeration");
    }

    @Test
    public void testMissingRequiredAttributeAttributeAuthorityDescriptorType()  throws Exception {
        testMissingAttribute("AttributeAuthorityDescriptorType", "protocolSupportEnumeration");
    }

    @Test
    public void testMissingRequiredAttributeAuthnAuthorityDescriptorType()  throws Exception {
        testMissingAttribute("AuthnAuthorityDescriptorType", "protocolSupportEnumeration");
    }

    @Test
    public void testMissingRequiredAttributePDPDescriptorType()  throws Exception {
        testMissingAttribute("PDPDescriptorType", "protocolSupportEnumeration");
    }

    @Test
    public void testMissingRequiredAttributeAttributeConsumingServiceType()  throws Exception {
        testMissingAttribute("AttributeConsumingServiceType", "index");
    }

    @Test
    public void testMissingRequiredAttributeAttributeType()  throws Exception {
        testMissingAttribute("AttributeType", "Name");
    }

    @Test
    public void testMissingRequiredAttributeContactType()  throws Exception {
        testMissingAttribute("ContactType", "contactType");
    }

    @Test
    public void testMissingRequiredAttributeEncryptionMethodType()  throws Exception {
        testMissingAttribute("EncryptionMethodType", "Algorithm");
    }

    @Test
    public void testMissingRequiredAttributeEndpointTypeBinding()  throws Exception {
        testMissingAttribute("EndpointType", "Binding");
    }

    @Test
    public void testMissingRequiredAttributeEndpointTypeLocation()  throws Exception {
        testMissingAttribute("EndpointType", "Location");
    }

    @Test
    public void testMissingRequiredAttributeEntityDescriptorType()  throws Exception {
        testMissingAttribute("EntityDescriptorType", "entityID");
    }

    @Test
    public void testMissingRequiredAttributeRequestedAttributeType()  throws Exception {
        testMissingAttribute("RequestedAttributeType", "Name");
    }

    private void testMissingAttribute(String type, String attributeName) throws Exception {
        thrown.expect(ParsingException.class);
        thrown.expectMessage(containsString("Parser: Required attribute missing: " + attributeName));

        assertParsed("missing-attribute/saml20-" + type + "-" + attributeName + ".xml", EntityDescriptorType.class);
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
        AssertionType assertion = assertParsed("saml20-assertion-anytype-attribute-value.xml", AssertionType.class);

        AttributeStatementType attributeStatementType = assertion.getAttributeStatements().iterator().next();
        assertThat(attributeStatementType.getAttributes(), hasSize(5));

        for (AttributeStatementType.ASTChoiceType choiceType: attributeStatementType.getAttributes()) {
            AttributeType attr = choiceType.getAttribute();
            String attrName = attr.getName();
            Object value = attr.getAttributeValue().get(0);
            // test selected attributes
            switch (attrName) {
                case "attr:type:string":
                    assertThat(value, is((Object) "CITIZEN"));
                    break;
                case "attr:notype:string":
                    assertThat(value, instanceOf(String.class));
                    assertThat(value, is((Object) "CITIZEN"));
                    break;
                case "attr:notype:element":
                    assertThat(value, instanceOf(String.class));
                    assertThat((String) value, containsString("hospitaal x"));
                    value = attr.getAttributeValue().get(1);
                    assertThat(value, instanceOf(String.class));
                    assertThat((String) value, containsString("hopital x"));
                    break;
                case "founded":
                    assertThat(value, is((Object) XMLTimeUtil.parse("2002-05-30T09:30:10-06:00")));
                    break;
                case "expanded":
                    assertThat(value, is((Object) XMLTimeUtil.parse("2002-06-30")));
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    public void testSaml20AssertionExample() throws Exception {
        AssertionType assertion = assertParsed("saml20-assertion-example.xml", AssertionType.class);

        AttributeStatementType attributeStatementType = assertion.getAttributeStatements().iterator().next();
        assertThat(attributeStatementType.getAttributes(), hasSize(12));

        for (AttributeStatementType.ASTChoiceType choiceType: attributeStatementType.getAttributes()) {
            AttributeType attr = choiceType.getAttribute();
            String attrName = attr.getName();
            Object value = attr.getAttributeValue().get(0);
            // test selected attributes
            switch (attrName) {
                case "portal_id":
                    assertEquals("060D00000000SHZ", value);
                    break;
                case "organization_id":
                    assertThat(value, instanceOf(String.class));
                    assertThat((String) value, containsString("<n3:stuff xmlns:n3=\"ftp://example.org\">00DD0000000F7L5</n3:stuff>"));
                    break;
                case "has_sub_organization":
                    assertThat(value, is((Object) "true"));
                    break;
                case "anytype_test":
                    assertThat(value, instanceOf(String.class));
                    assertThat((String) value, containsString("<elem2>val2</elem2>"));
                    break;
                case "anytype_no_xml_test":
                    assertThat(value, is((Object) "value_no_xml"));
                    break;
                case "anytype_xml_fragment":
                    assertThat(value, is((Object) "<elem1>Foo</elem1><elem2>Bar</elem2>"));
                    break;
                case "logouturl":
                    assertThat(value, is((Object) "http://www.salesforce.com/security/del_auth/SsoLogoutPage.html"));
                    break;
                case "nil_value_attribute":
                    assertNull(value);
                    break;
                case "status":
                    assertThat(value, is((Object) "<status><code><status>XYZ</status></code></status>"));
                    break;
                case "userDefined":
                    assertThat(value, is((Object) "<A><B>Foo</B><C>Bar</C></A>"));
                    break;
                case "userDefinedFragmentWithNamespace":
                    assertThat(value, is((Object) "<myPrefix:B xmlns:myPrefix=\"urn:myNamespace\">Foo</myPrefix:B><myPrefix:C xmlns:myPrefix=\"urn:myNamespace\">Bar</myPrefix:C>"));
                    break;
                default:
                    break;
            }
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
