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

package org.keycloak.broker.wsfed;

import org.keycloak.wsfed.common.TestHelpers;
import org.keycloak.wsfed.common.WSFedConstants;
import org.keycloak.protocol.wsfed.builders.RequestSecurityTokenResponseBuilder;
import org.keycloak.wsfed.common.MockHelper;
import org.keycloak.protocol.wsfed.builders.WSFedSAML2AssertionTypeBuilder;
import org.junit.Test;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.saml.RandomSecret;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.services.messages.Messages;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityTokenResponse;
import org.w3c.dom.Document;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.keycloak.wsfed.common.TestHelpers.*;

public class SAML2RequestedTokenTest {
    @Test
    public void testValidate() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        RequestedToken rt = getSAML2RequestToken(mockHelper);

        WSFedIdentityProviderConfig config = mock(WSFedIdentityProviderConfig.class);
        when(config.getWsFedRealm()).thenReturn(mockHelper.getClientId());

        Response response = rt.validate(mockHelper.getRealm().getPublicKey(), config, mock(EventBuilder.class), mockHelper.getSession());
        assertNull(response);
        verifyZeroInteractions(mockHelper.getLoginFormsProvider());
    }

    @Test
    public void testInvalidSignature() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        RequestedToken rt = getSAML2RequestToken(mockHelper);

        WSFedIdentityProviderConfig config = mock(WSFedIdentityProviderConfig.class);
        when(config.getWsFedRealm()).thenReturn(mockHelper.getClientId());

        //Generate new key to test exception
        KeyPair keyPair = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        EventBuilder event = mock(EventBuilder.class);

        //use new key to verify which we know will fail
        Response response = rt.validate(keyPair.getPublic(), config, event, mockHelper.getSession());
        assertNotNull(response);

        verify(event, times(1)).error(Errors.INVALID_SIGNATURE);
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.INVALID_FEDERATED_IDENTITY_ACTION);
        verifyNoMoreInteractions(mockHelper.getLoginFormsProvider());
    }

    @Test
    public void testExpired() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        mockHelper.setAccessCodeLifespan(1); //make this expire after a second
        RequestedToken rt = getSAML2RequestToken(mockHelper);

        WSFedIdentityProviderConfig config = mock(WSFedIdentityProviderConfig.class);
        when(config.getWsFedRealm()).thenReturn(mockHelper.getClientId());

        Thread.sleep(2000); //wait until token expires

        EventBuilder event = mock(EventBuilder.class);

        Response response = rt.validate(mockHelper.getRealm().getPublicKey(), config, event, mockHelper.getSession());
        assertNotNull(response);

        verify(event, times(1)).error(Errors.EXPIRED_CODE);
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.INVALID_FEDERATED_IDENTITY_ACTION);
        verifyNoMoreInteractions(mockHelper.getLoginFormsProvider());
    }

    @Test
    public void testInvalidAudience() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        RequestedToken rt = getSAML2RequestToken(mockHelper);

        WSFedIdentityProviderConfig config = mock(WSFedIdentityProviderConfig.class);
        when(config.getWsFedRealm()).thenReturn("https://someinvalidclientid");

        EventBuilder event = mock(EventBuilder.class);

        Response response = rt.validate(mockHelper.getRealm().getPublicKey(), config, event, mockHelper.getSession());
        assertNotNull(response);

        verify(event, times(1)).error(Errors.INVALID_SAML_RESPONSE);
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.INVALID_FEDERATED_IDENTITY_ACTION);
        verifyNoMoreInteractions(mockHelper.getLoginFormsProvider());
    }

    @Test
    public void testGetSubjectNameId() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        SAML2RequestedToken rt = getSAML2RequestToken(mockHelper);
        NameIDType nameId = rt.getSubjectNameID();

        assertEquals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), nameId.getFormat().toString());
        assertEquals(mockHelper.getEmail(), nameId.getValue());
    }

    @Test
    public void testGetEmailViaSubjectNameId() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        SAML2RequestedToken rt = getSAML2RequestToken(mockHelper);

        assertEquals(mockHelper.getEmail(), rt.getEmail());
    }

    @Test
    public void testGetId() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        SAML2RequestedToken rt = getSAML2RequestToken(mockHelper);

        assertEquals(mockHelper.getEmail(), rt.getId());
    }

    @Test
    public void testGetUser() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        SAML2RequestedToken rt = getSAML2RequestToken(mockHelper);

        assertEquals(mockHelper.getEmail(), rt.getUsername());
    }

    @Test
    public void testGetSession() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        SAML2RequestedToken rt = getSAML2RequestToken(mockHelper);

        //TODO: getSessionIndex still needs to be implemented
        assertNull(null, rt.getSessionIndex());
    }

    @Test
    public void testDescryptAssertion() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();
        SAML2RequestedToken rt = getSAML2RequestToken(mockHelper);
        AssertionType originalAssertion = rt.getAssertionType();

        //encrypt assertion and then decrypt it
        Document doc = AssertionUtil.asDocument(originalAssertion);
        encryptDocument(doc, mockHelper);

        AssertionType decryptedAssertion = rt.getAssertionType(doc.getDocumentElement(), mockHelper.getRealm());

        //Check a few things just to make sure it decrypted
        assertEquals(originalAssertion.getIssuer().getValue(), decryptedAssertion.getIssuer().getValue());
        assertEquals(originalAssertion.getID(), decryptedAssertion.getID());
    }

    protected static SAML2RequestedToken getSAML2RequestToken(MockHelper mockHelper) throws Exception {
        RequestSecurityTokenResponseBuilder builder = generateRequestSecurityTokenResponseBuilder(mockHelper);
        String wsfedResponse = builder.getStringValue();

        WSFedEndpoint endpoint = new WSFedEndpoint(null, null, null, null);
        RequestSecurityTokenResponse rstr = endpoint.getWsfedToken(wsfedResponse);

        return new SAML2RequestedToken(wsfedResponse, rstr.getRequestedSecurityToken().getAny().get(0), mockHelper.getRealm());
    }

    public static RequestSecurityTokenResponseBuilder generateRequestSecurityTokenResponseBuilder(MockHelper mockHelper) throws Exception {
        mockHelper.getClientAttributes().put(WSFedSAML2AssertionTypeBuilder.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "false");
        mockHelper.getClientSessionNotes().put(GeneralConstants.NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());

        mockHelper.initializeMockValues();

        RequestSecurityTokenResponseBuilder builder = new RequestSecurityTokenResponseBuilder();

        builder.setRealm(mockHelper.getClientId())
                .setAction(WSFedConstants.WSFED_SIGNIN_ACTION)
                .setDestination("https://localhost:8443")
                .setContext("context")
                .setTokenExpiration(mockHelper.getAccessTokenLifespan())
                .setRequestIssuer("https://issuer")
                .setSigningKeyPair(new KeyPair(mockHelper.getRealm().getPublicKey(), mockHelper.getRealm().getPrivateKey()))
                .setSigningCertificate(mockHelper.getRealm().getCertificate());

        //SAML Token generation
        WSFedSAML2AssertionTypeBuilder samlBuilder = new WSFedSAML2AssertionTypeBuilder();
        samlBuilder.setRealm(mockHelper.getRealm())
                .setUriInfo(mockHelper.getUriInfo())
                .setAccessCode(mockHelper.getAccessCode())
                .setClientSession(mockHelper.getClientSessionModel())
                .setUserSession(mockHelper.getUserSessionModel())
                .setSession(mockHelper.getSession());

        AssertionType token = samlBuilder.build();
        builder.setSamlToken(token);

        return builder;
    }

    protected void encryptDocument(Document samlDocument, MockHelper mockHelper) throws ProcessingException {
        String samlNSPrefix = samlDocument.getDocumentElement().getPrefix();

        try {
            QName encryptedAssertionElementQName = new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                    JBossSAMLConstants.ENCRYPTED_ASSERTION.get(), samlNSPrefix);

            byte[] secret = RandomSecret.createRandomSecret(128 / 8);
            SecretKey secretKey = new SecretKeySpec(secret, "AES");

            // encrypt the Assertion element and replace it with a EncryptedAssertion element.
            XMLEncryptionUtil.encryptElement(new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                            JBossSAMLConstants.ASSERTION.get(), samlNSPrefix), samlDocument, mockHelper.getRealm().getPublicKey(),
                    secretKey, 128, encryptedAssertionElementQName, true);
        } catch (Exception e) {
            throw new ProcessingException("failed to encrypt", e);
        }
    }
}