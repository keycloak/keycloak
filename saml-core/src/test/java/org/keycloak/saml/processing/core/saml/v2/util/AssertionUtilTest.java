package org.keycloak.saml.processing.core.saml.v2.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Scanner;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType.STSubType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParserTest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AssertionUtilTest {

    private static final String PRIVATE_KEY = "MIICWwIBAAKBgQDVG8a7xGN6ZIkDbeecySygcDfsypjUMNPE4QJjis8B316CvsZQ0hcTTLUyiRpHlHZys2k3xEhHBHymFC1AONcvzZzpb40tAhLHO1qtAnut00khjAdjR3muLVdGkM/zMC7G5s9iIwBVhwOQhy+VsGnCH91EzkjZ4SVEr55KJoyQJQIDAQABAoGADaTtoG/+foOZUiLjRWKL/OmyavK9vjgyFtThNkZY4qHOh0h3og0RdSbgIxAsIpEa1FUwU2W5yvI6mNeJ3ibFgCgcxqPk6GkAC7DWfQfdQ8cS+dCuaFTs8ObIQEvU50YzeNPiiFxRA+MnauCUXaKm/PnDfjd4tPgru7XZvlGh0wECQQDsBbN2cKkBKpr/b5oJiBcBaSZtWiMNuYBDn9x8uORj+Gy/49BUIMHF2EWyxOWz6ocP5YiynNRkPe21Zus7PEr1AkEA5yWQOkxUTIg43s4pxNSeHtL+Ebqcg54lY2xOQK0yufxUVZI8ODctAKmVBMiCKpU3mZQquOaQicuGtocpgxlScQI/YM31zZ5nsxLGf/5GL6KhzPJT0IYn2nk7IoFu7bjn9BjwgcPurpLA52TNMYWQsTqAKwT6DEhG1NaRqNWNpb4VAkBehObAYBwMm5udyHIeEc+CzUalm0iLLa0eRdiN7AUVNpCJ2V2Uo0NcxPux1AgeP5xXydXafDXYkwhINWcNO9qRAkEA58ckAC5loUGwU5dLaugsGH/a2Q8Ac8bmPglwfCstYDpl8Gp/eimb1eKyvDEELOhyImAv4/uZV9wN85V0xZXWsw==";

    /**
     * The public certificate that corresponds to {@link #PRIVATE_KEY}.
     */
    private static final String PUBLIC_CERT = "MIIDdzCCAl+gAwIBAgIEbySuqTANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE1MDEyODIyMTYyMFoXDTE3MTAyNDIyMTYyMFowbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAII/K9NNvXi9IySl7+l2zY/kKrGTtuR4WdCI0xLW/Jn4dLY7v1/HOnV4CC4ecFOzhdNFPtJkmEhP/q62CpmOYOKApXk3tfmm2rwEz9bWprVxgFGKnbrWlz61Z/cjLAlhD3IUj2ZRBquYgSXQPsYfXo1JmSWF5pZ9uh1FVqu9f4wvRqY20ZhUN+39F+1iaBsoqsrbXypCn1HgZkW1/9D9GZug1c3vB4wg1TwZZWRNGtxwoEhdK6dPrNcZ+6PdanVilWrbQFbBjY4wz8/7IMBzssoQ7Usmo8F1Piv0FGfaVeJqBrcAvbiBMpk8pT+27u6p8VyIX6LhGvnxIwM07NByeSUCAwEAAaMhMB8wHQYDVR0OBBYEFFlcNuTYwI9W0tQ224K1gFJlMam0MA0GCSqGSIb3DQEBCwUAA4IBAQB5snl1KWOJALtAjLqD0mLPg1iElmZP82Lq1htLBt3XagwzU9CaeVeCQ7lTp+DXWzPa9nCLhsC3QyrV3/+oqNli8C6NpeqI8FqN2yQW/QMWN1m5jWDbmrWwtQzRUn/rh5KEb5m3zPB+tOC6e/2bV3QeQebxeW7lVMD0tSCviUg1MQf1l2gzuXQo60411YwqrXwk6GMkDOhFDQKDlMchO3oRbQkGbcP8UeiKAXjMeHfzbiBr+cWz8NYZEtxUEDYDjTpKrYCSMJBXpmgVJCZ00BswbksxJwaGqGMPpUKmCV671pf3m8nq3xyiHMDGuGwtbU+GE8kVx85menmp8+964nin";

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testSaml20Signed() throws Exception {

        X509Certificate decodeCertificate = DerUtils.decodeCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(PUBLIC_CERT)));

        try (InputStream st = AssertionUtilTest.class.getResourceAsStream("saml20-signed-response.xml")) {
            Document document = DocumentUtil.getDocument(st);

            Element assertion = DocumentUtil.getDirectChildElement(document.getDocumentElement(), "urn:oasis:names:tc:SAML:2.0:assertion", "Assertion");

            assertTrue(AssertionUtil.isSignatureValid(assertion, decodeCertificate.getPublicKey()));

            // test manipulation of signature
            Element signatureElement = AssertionUtil.getSignature(assertion);
            Element signatureValue = (Element) signatureElement.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "SignatureValue").item(0);
            byte[] validSignature = Base64.getDecoder().decode(signatureValue.getTextContent());

            // change the signature value slightly
            byte[] invalidSignature =  Arrays.copyOf(validSignature, validSignature.length);
            invalidSignature[0] ^= invalidSignature[0];
            signatureValue.setTextContent(Base64.getEncoder().encodeToString(invalidSignature));

            // check that signature now is invalid
            assertFalse(AssertionUtil.isSignatureValid(document.getDocumentElement(), decodeCertificate.getPublicKey()));

            // restore valid signature, but remove Signature element, check that still invalid
            signatureElement.setTextContent(Base64.getEncoder().encodeToString(validSignature));

            assertion.removeChild(signatureElement);
            assertFalse(AssertionUtil.isSignatureValid(document.getDocumentElement(), decodeCertificate.getPublicKey()));
        }
    }

    @Test
    public void testSaml20DecryptId() throws Exception {
        try (InputStream st = getEncryptedIdTestFileInputStream()) {
            ResponseType responseType = (ResponseType) SAMLParser.getInstance().parse(st);

            STSubType subType = responseType.getAssertions().get(0).getAssertion().getSubject().getSubType();

            assertNotNull(subType.getEncryptedID());
            assertNull(subType.getBaseID());

            PrivateKey pk = extractPrivateKey();
            AssertionUtil.decryptId(responseType, data -> Collections.singletonList(pk));

            assertNull(subType.getEncryptedID());
            assertNotNull(subType.getBaseID());
            assertTrue(subType.getBaseID() instanceof NameIDType);
            assertEquals("myTestId",
                    ((NameIDType) subType.getBaseID()).getValue());
        }

    }

    private InputStream getEncryptedIdTestFileInputStream() {
        return SAMLParserTest.class.getResourceAsStream("saml20-encrypted-id-response.xml");
    }

    private PrivateKey extractPrivateKey() throws IOException {

        StringBuilder sb = new StringBuilder();
        try (Scanner sc = new Scanner(getEncryptedIdTestFileInputStream())) {
            while (sc.hasNextLine()) {
                if (sc.nextLine().contains("BEGIN RSA PRIVATE KEY")) {
                    sb.append("-----BEGIN RSA PRIVATE KEY-----").append("\n");
                    while (sc.hasNextLine()) {
                        String line = sc.nextLine();
                        if (line.contains("END RSA PRIVATE KEY")) {
                            sb.append("-----END RSA PRIVATE KEY-----");
                            break;
                        }
                        sb.append(line).append("\n");
                    }
                }
            }
        }
        assertNotEquals("PEM certificate not found in test data", 0, sb.length());
        return PemUtils.decodePrivateKey(sb.toString());
    }

}
