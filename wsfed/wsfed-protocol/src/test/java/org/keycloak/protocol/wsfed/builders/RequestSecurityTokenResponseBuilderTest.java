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

package org.keycloak.protocol.wsfed.builders;

import org.keycloak.wsfed.common.MockHelper;
import org.keycloak.wsfed.common.TestHelpers;
import org.keycloak.wsfed.common.WSFedConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.util.Base64;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityTokenResponse;
import org.picketlink.identity.federation.ws.addressing.EndpointReferenceType;
import org.picketlink.identity.federation.ws.wss.secext.BinarySecurityTokenType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.KeyPair;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.keycloak.wsfed.common.TestHelpers.*;

/**
 * Created by dbarentine on 8/21/2015.
 */
public class RequestSecurityTokenResponseBuilderTest {

    @Rule
    public ExpectedException configException = ExpectedException.none();

    @Test
    public void testNoTokenException() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();

        mockHelper.initializeMockValues();

        RequestSecurityTokenResponseBuilder builder = new RequestSecurityTokenResponseBuilder();

        builder.setRealm(mockHelper.getClientId())
                .setAction(WSFedConstants.WSFED_SIGNIN_ACTION)
                .setDestination("https://localhost:8443")
                .setContext("context")
                .setTokenExpiration(mockHelper.getAccessTokenLifespan())
                .setRequestIssuer("https://issuer");

        configException.expect(ConfigurationException.class);
        configException.expectMessage(equalTo("SAML or JWT must be set."));

        builder.getStringValue();
    }

    @Test
    public void testSamlTokenGeneration() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();

        mockHelper.getClientAttributes().put(WSFedSAML2AssertionTypeBuilder.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "false");
        mockHelper.getClientSessionNotes().put(GeneralConstants.NAMEID_FORMAT, "email");

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

        RequestSecurityTokenResponse rstr = builder.build();

        assertEquals(builder.getContext(), rstr.getContext());
        assertNotNull(rstr.getLifetime().getCreated());
        assertNotNull(rstr.getLifetime().getExpires());
        assertEquals(XMLTimeUtil.add(rstr.getLifetime().getCreated(), mockHelper.getAccessTokenLifespan() * 1000), rstr.getLifetime().getExpires());

        assertEquals(builder.getRequestIssuer(), ((EndpointReferenceType) rstr.getAppliesTo().getAny().get(0)).getAddress().getValue());
        assertEquals(URI.create("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0"), rstr.getTokenType());

        assertThat(rstr.getRequestedSecurityToken().getAny().get(0), instanceOf(Element.class));
        Element element = (Element)rstr.getRequestedSecurityToken().getAny().get(0);
        assertTrue(AssertionUtil.isSignatureValid(element, mockHelper.getRealm().getPublicKey()));
    }

    @Test
    public void testOIDCTokenGeneration() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();

        mockHelper.getClientSessionNotes().put(OIDCLoginProtocol.ISSUER, String.format("%s/realms/%s", mockHelper.getBaseUri(), mockHelper.getRealmName()));

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

        //OIDC Token generation
        WSFedOIDCAccessTokenBuilder oidcBuilder = new WSFedOIDCAccessTokenBuilder();
        oidcBuilder.setSession(mockHelper.getSession())
                .setUserSession(mockHelper.getUserSessionModel())
                .setAccessCode(mockHelper.getAccessCode())
                .setClient(mockHelper.getClient())
                .setClientSession(mockHelper.getClientSessionModel())
                .setRealm(mockHelper.getRealm())
                .setX5tIncluded(false);

        String jwtString = oidcBuilder.build();
        builder.setJwt(jwtString);

        RequestSecurityTokenResponse rstr = builder.build();

        assertEquals(builder.getContext(), rstr.getContext());
        assertNotNull(rstr.getLifetime().getCreated());
        assertNotNull(rstr.getLifetime().getExpires());
        assertEquals(XMLTimeUtil.add(rstr.getLifetime().getCreated(), mockHelper.getAccessTokenLifespan() * 1000), rstr.getLifetime().getExpires());

        assertEquals(builder.getRequestIssuer(), ((EndpointReferenceType) rstr.getAppliesTo().getAny().get(0)).getAddress().getValue());
        assertEquals(URI.create("urn:ietf:params:oauth:token-type:jwt"), rstr.getTokenType());

        assertThat(rstr.getRequestedSecurityToken().getAny().get(0), instanceOf(BinarySecurityTokenType.class));

        BinarySecurityTokenType bstt = (BinarySecurityTokenType)rstr.getRequestedSecurityToken().getAny().get(0);

        assertEquals("urn:ietf:params:oauth:token-type:jwt", bstt.getValueType());
        assertEquals("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary", bstt.getEncodingType());
        assertEquals(jwtString, new String(Base64.decode(bstt.getValue())));
    }

    @Test
    public void testResponseGeneration() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();

        mockHelper.getClientSessionNotes().put(OIDCLoginProtocol.ISSUER, String.format("%s/realms/%s", mockHelper.getBaseUri(), mockHelper.getRealmName()));

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

        //OIDC Token generation
        WSFedOIDCAccessTokenBuilder oidcBuilder = new WSFedOIDCAccessTokenBuilder();
        oidcBuilder.setSession(mockHelper.getSession())
                .setUserSession(mockHelper.getUserSessionModel())
                .setAccessCode(mockHelper.getAccessCode())
                .setClient(mockHelper.getClient())
                .setClientSession(mockHelper.getClientSessionModel())
                .setRealm(mockHelper.getRealm())
                .setX5tIncluded(false);

        String jwtString = oidcBuilder.build();
        builder.setJwt(jwtString);

        Response response = builder.buildResponse();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("no-cache", response.getMetadata().getFirst("Pragma"));
        assertEquals("no-cache, no-store", response.getMetadata().getFirst("Cache-Control"));

        Document doc = responseToDocument(response);

        assertFormAction(doc, "POST", builder.getDestination());
        assertInputNode(doc, WSFedConstants.WSFED_ACTION, builder.getAction());
        assertInputNode(doc, WSFedConstants.WSFED_REALM, mockHelper.getClientId());
        getInputNodeValue(doc, WSFedConstants.WSFED_RESULT); //This will just validate it exists
        assertInputNodeMissing(doc, WSFedConstants.WSFED_REPLY);
        assertInputNode(doc, WSFedConstants.WSFED_CONTEXT, builder.getContext());
    }
}