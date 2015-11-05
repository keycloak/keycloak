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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.common.ClientConnection;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityTokenResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeFactory;
import java.lang.reflect.Field;
import java.net.URI;
import java.security.PublicKey;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.keycloak.wsfed.common.TestHelpers.*;

public class WSFedEndpointTest {
    @Mock private EventBuilder event;
    @Mock private WSFedIdentityProviderConfig config;
    @Mock private IdentityProvider.AuthenticationCallback callback;
    @Mock private WSFedIdentityProvider provider;

    @Mock private ClientConnection clientConnection;
    @Mock private HttpHeaders headers;

    private MockHelper mockHelper;
    private WSFedEndpoint endpoint;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockHelper = TestHelpers.getMockHelper().initializeMockValues();

        endpoint = spy(new WSFedEndpoint(mockHelper.getRealm(), this.provider, this.config, this.callback));
        injectMocks(endpoint);
    }

    //@InjectMocks seems to have issues with the @Context fields. There are also some known issues with @InjectMocks and @Spy.
    //So instead inject manually.
    private void injectMocks(WSFedEndpoint endpoint) throws Exception {
        endpoint.event = this.event;

        setPrivateField(endpoint, "uriInfo", mockHelper.getUriInfo());
        setPrivateField(endpoint, "session", mockHelper.getSession());
        setPrivateField(endpoint, "clientConnection", this.clientConnection);
        setPrivateField(endpoint, "headers", this.headers);
    }

    private void setPrivateField(WSFedEndpoint endpoint, String field, Object value) throws Exception {
        Field f = WSFedEndpoint.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(endpoint, value);
    }

    @Test
    public void testRedirectBinding() {
        String action = "wa";
        String result = "result";
        String context = "context";

        doReturn(null).when(endpoint).execute(action, result, context);

        endpoint.redirectBinding(action, result, context);

        verify(endpoint, times(1)).execute(eq(action), eq(result), eq(context));
    }

    @Test
    public void testPostBinding() {
        String action = "wa";
        String result = "result";
        String context = "context";

        doReturn(null).when(endpoint).execute(action, result, context);

        endpoint.postBinding(action, result, context);

        verify(endpoint, times(1)).execute(eq(action), eq(result), eq(context));
    }

    @Test
    public void testExecuteEmptySignoutAction() {
        when(config.handleEmptyActionAsLogout()).thenReturn(true);
        doReturn(mock(Response.class)).when(endpoint).handleSignoutResponse(anyString());

        endpoint.execute(null, "result", "context");
        verify(endpoint, times(1)).handleSignoutResponse("context");
    }

    @Test
    public void testExecuteFailedBasicChecks() {
        when(config.handleEmptyActionAsLogout()).thenReturn(false);
        when(mockHelper.getRealm().isEnabled()).thenReturn(false);

        Response response = endpoint.execute(null, "result", "context");
        assertNotNull(response);

        //We can't use the event mock here because it gets recreated on first call to execute.
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.REALM_NOT_ENABLED);
    }

    @Test
    public void testExecuteHandleWsfedResponse() {
        when(config.handleEmptyActionAsLogout()).thenReturn(false);
        doReturn(mock(Response.class)).when(endpoint).handleWsFedResponse("result", "context");

        Response response = endpoint.execute(WSFedConstants.WSFED_SIGNIN_ACTION, "result", "context");
        assertNotNull(response);

        verify(endpoint, times(1)).handleWsFedResponse("result", "context");
    }

    @Test
    public void testExecuteHandleSignoutRequest() {
        when(config.handleEmptyActionAsLogout()).thenReturn(false);
        doReturn(mock(Response.class)).when(endpoint).handleSignoutRequest("context");

        Response response = endpoint.execute(WSFedConstants.WSFED_SIGNOUT_ACTION, null, "context");
        assertNotNull(response);

        verify(endpoint, times(1)).handleSignoutRequest("context");
    }

    @Test
    public void testExecuteHandleSignoutResponse() {
        when(config.handleEmptyActionAsLogout()).thenReturn(false);
        doReturn(mock(Response.class)).when(endpoint).handleSignoutResponse("context");

        Response response = endpoint.execute(WSFedConstants.WSFED_SIGNOUT_CLEANUP_ACTION, null, "context");
        assertNotNull(response);

        verify(endpoint, times(1)).handleSignoutResponse("context");
    }

    @Test
    public void testExecuteError() {
        when(config.handleEmptyActionAsLogout()).thenReturn(false);

        Response response = endpoint.execute(WSFedConstants.WSFED_SIGNIN_ACTION, null, "context");
        assertNotNull(response);

        //We can't use the event mock here because it gets recreated on first call to execute.
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.INVALID_REQUEST);
    }

    @Test
    public void testHandleSignoutRequestNoIdentityCookie() {
        endpoint.authMgr = mock(AuthenticationManager.class);
        when(endpoint.authMgr.authenticateIdentityCookie(mockHelper.getSession(), mockHelper.getRealm())).thenReturn(null);

        Response response = endpoint.handleSignoutRequest("context");
        assertNotNull(response);

        verify(event, times(1)).error(Errors.USER_SESSION_NOT_FOUND);
    }

    @Test
    public void testHandleSignoutRequest() throws Exception {
        when(config.getWsFedRealm()).thenReturn("https://wsfedRealm");
        when(config.getSingleLogoutServiceUrl()).thenReturn("https://wsfed.com/slo");
        when(provider.getEndpoint(mockHelper.getUriInfo(), mockHelper.getRealm())).thenReturn("https://kc/realms/etc/etc");

        endpoint.authMgr = mock(AuthenticationManager.class);

        AuthenticationManager.AuthResult authResult = mock(AuthenticationManager.AuthResult.class);
        when(authResult.getSession()).thenReturn(mockHelper.getUserSessionModel());
        when(endpoint.authMgr.authenticateIdentityCookie(eq(mockHelper.getSession()), eq(mockHelper.getRealm()))).thenReturn(authResult);
        when(mockHelper.getUserSessionModel().getState()).thenReturn(UserSessionModel.State.LOGGING_OUT);

        Response response = endpoint.handleSignoutRequest("context");
        Document doc = responseToDocument(response);

        assertFormAction(doc, HttpMethod.GET, config.getSingleLogoutServiceUrl());
        assertInputNode(doc, WSFedConstants.WSFED_ACTION, WSFedConstants.WSFED_SIGNOUT_ACTION);
        assertInputNode(doc, WSFedConstants.WSFED_REALM, config.getWsFedRealm());
        assertInputNode(doc, WSFedConstants.WSFED_REPLY, provider.getEndpoint(mockHelper.getUriInfo(), mockHelper.getRealm()));
        assertInputNode(doc, WSFedConstants.WSFED_CONTEXT, "context");
    }

    @Test
    public void testHandleSignoutResponseNoIdentityCookie() {
        endpoint.authMgr = mock(AuthenticationManager.class);
        when(endpoint.authMgr.authenticateIdentityCookie(mockHelper.getSession(), mockHelper.getRealm())).thenReturn(null);

        Response response = endpoint.handleSignoutResponse("context");
        assertNotNull(response);

        verify(event, times(1)).error(Errors.USER_SESSION_NOT_FOUND);
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
    }

    @Test
    public void testHandleSignoutResponseInvalidState() throws Exception {
        endpoint.authMgr = mock(AuthenticationManager.class);

        AuthenticationManager.AuthResult authResult = mock(AuthenticationManager.AuthResult.class);
        when(authResult.getSession()).thenReturn(mockHelper.getUserSessionModel());
        when(endpoint.authMgr.authenticateIdentityCookie(eq(mockHelper.getSession()), eq(mockHelper.getRealm()))).thenReturn(authResult);
        when(mockHelper.getUserSessionModel().getState()).thenReturn(UserSessionModel.State.LOGGED_IN);

        Response response = endpoint.handleSignoutResponse("context");
        assertNotNull(response);

        verify(event, times(1)).error(Errors.USER_SESSION_NOT_FOUND);
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.SESSION_NOT_ACTIVE);
    }

    @Test
    public void testHandleLoginResponseThrowsIdentityBrokerException() throws Exception{
        RequestedToken token = mock(RequestedToken.class);
        when(token.getId()).thenThrow(new NullPointerException("Expected null argument exception"));

        expectedException.expect(IdentityBrokerException.class);
        expectedException.expectMessage(equalTo("Could not process response from WS-Fed identity provider."));
        endpoint.handleLoginResponse(null, token, null);
    }

    @Test
    public void testHandleLoginResponse() throws Exception {
        final RequestedToken token = mock(RequestedToken.class);

        when(config.isStoreToken()).thenReturn(true);
        when(config.getAlias()).thenReturn("wsfedMock");

        when(token.getId()).thenReturn("mockId");
        when(token.getEmail()).thenReturn("test.email@taos.test");
        when(token.getSessionIndex()).thenReturn("123");
        when(token.getUsername()).thenReturn("username");

        when(callback.authenticated(any(BrokeredIdentityContext.class))).
                    thenAnswer(new Answer<Response>() {
                        public Response answer(InvocationOnMock invocation) {
                            BrokeredIdentityContext identity = invocation.getArgumentAt(0, BrokeredIdentityContext.class);

                            assertEquals("wsfedContext", identity.getCode());
                            assertEquals(token, identity.getContextData().get(WSFedEndpoint.WSFED_REQUESTED_TOKEN));
                            assertEquals(token.getUsername(), identity.getUsername());
                            assertEquals(token.getEmail(), identity.getEmail());
                            assertEquals("wsfedToken", identity.getToken());
                            assertEquals(String.format("%s.%s.%s", config.getAlias(), token.getId(), token.getSessionIndex()), identity.getBrokerSessionId());

                            return mock(Response.class);
                        }
                    });

        Response response = endpoint.handleLoginResponse("wsfedToken", token, "wsfedContext");
        assertNotNull("Response should not be null", response);

        verify(callback, times(1)).authenticated(any(BrokeredIdentityContext.class));
    }

    @Test
    public void testHandleLoginResponseException() throws Exception {
        final RequestedToken token = mock(RequestedToken.class);

        when(config.isStoreToken()).thenReturn(true);
        when(config.getAlias()).thenReturn("wsfedMock");

        when(token.getId()).thenReturn("mockId");
        when(token.getEmail()).thenReturn("test.email@taos.test");
        when(token.getSessionIndex()).thenReturn("123");
        when(token.getUsername()).thenReturn("username");

        when(callback.authenticated(any(BrokeredIdentityContext.class))).thenThrow(new RuntimeException("Exception"));

        expectedException.expect(IdentityBrokerException.class);
        expectedException.expectMessage(equalTo("Could not process response from WS-Fed identity provider."));
        endpoint.handleLoginResponse("wsfedToken", token, "wsfedContext");
    }

    @Test
    public void testHandleWsFedResponseInvalidToken() throws Exception {
        Response response = endpoint.handleWsFedResponse("", "");

        assertNotNull(response);
        verify(event, times(1)).error(Errors.INVALID_SAML_RESPONSE);
    }

    @Test
    public void testHandleWsFedResponseExpiredToken() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        doReturn(true).when(endpoint).hasExpired(any(RequestSecurityTokenResponse.class));
        Response response = endpoint.handleWsFedResponse(builder.getStringValue(), builder.getContext());

        assertNotNull(response);
        verify(event, times(1)).error(Errors.EXPIRED_CODE);
    }

    @Test
    public void testHandleWsFedResponseSAML10NotImplemented() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        RequestSecurityTokenResponse rstr = builder.build();
        rstr.setTokenType(URI.create("urn:oasis:names:tc:SAML:1.0:assertion"));

        Response response = endpoint.handleWsFedResponse(RequestSecurityTokenResponseBuilder.getStringValue(rstr), builder.getContext());

        assertNotNull(response);
        verify(event, times(1)).error(Errors.INVALID_SAML_RESPONSE);
    }

    @Test
    public void testHandleWsFedResponseJWTNotImplemented() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        RequestSecurityTokenResponse rstr = builder.build();
        rstr.setTokenType(URI.create("urn:ietf:params:oauth:token-type:jwt"));

        Response response = endpoint.handleWsFedResponse(RequestSecurityTokenResponseBuilder.getStringValue(rstr), builder.getContext());

        assertNotNull(response);
        verify(event, times(1)).error(Errors.INVALID_SAML_RESPONSE);
    }

    @Test
    public void testHandleWsFedResponseUnknownNotImplemented() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        RequestSecurityTokenResponse rstr = builder.build();
        rstr.setTokenType(URI.create("Unknown"));

        Response response = endpoint.handleWsFedResponse(RequestSecurityTokenResponseBuilder.getStringValue(rstr), builder.getContext());

        assertNotNull(response);
        verify(event, times(1)).error(Errors.INVALID_SAML_RESPONSE);
    }

    @Test
    public void testHandleWsFedResponseBadSig() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        when(config.isValidateSignature()).thenReturn(true);

        RealmModel junkRealm = mock(RealmModel.class);
        MockHelper.generateRealmKeys(junkRealm);
        doReturn(junkRealm.getPublicKey()).when(endpoint).getIDPKey();

        Response response = endpoint.handleWsFedResponse(builder.getStringValue(), builder.getContext());

        assertNotNull(response);
        verify(event, times(1)).error(Errors.INVALID_SIGNATURE);
    }

    @Test
    public void testHandleWsFedResponseValidSig() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        String wsfedResponse = builder.getStringValue();
        when(config.isValidateSignature()).thenReturn(true);
        when(config.getWsFedRealm()).thenReturn(mockHelper.getClientId());

        doReturn(mockHelper.getRealm().getPublicKey()).when(endpoint).getIDPKey();

        Response success = mock(Response.class);
        doReturn(success).when(endpoint).handleLoginResponse(eq(wsfedResponse), any(RequestedToken.class), eq(builder.getContext()));

        Response response = endpoint.handleWsFedResponse(wsfedResponse, builder.getContext());

        assertNotNull(response);
        assertEquals(success, response);

        verifyZeroInteractions(event);
    }

    @Test
    public void testGetIDPKey() throws Exception {
        String pem = mockHelper.getRealm().getCertificatePem();
        when(config.getSigningCertificate()).thenReturn(pem);

        PublicKey key = endpoint.getIDPKey();

        assertEquals(mockHelper.getRealm().getPublicKey(), key);
    }

    @Test
    public void testGetIDPKeyInvalid() throws Exception {
        when(config.getSigningCertificate()).thenReturn("badpem");

        expectedException.expect(ProcessingException.class);
        expectedException.expectMessage(equalTo(ErrorCodes.PROCESSING_EXCEPTION));
        endpoint.getIDPKey();
    }

    @Test
    public void testGetIDPKeyNullException() throws Exception {
        when(config.getSigningCertificate()).thenReturn(null);

        expectedException.expect(ConfigurationException.class);
        endpoint.getIDPKey();
    }

    @Test
    public void testHasExpired() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        builder.setTokenExpiration(1);

        RequestSecurityTokenResponse rstr = builder.build();

        Thread.sleep(2000);
        assertTrue(endpoint.hasExpired(rstr));
    }

    @Test
    public void testHasExpiredNotValidYet() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        RequestSecurityTokenResponse rstr = builder.build();

        rstr.getLifetime().getCreated().add(DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 00));
        rstr.getLifetime().getExpires().add(DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 00));

        assertTrue(endpoint.hasExpired(rstr));
    }

    @Test
    public void testHasNotExpired() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        RequestSecurityTokenResponse rstr = builder.build();

        assertFalse(endpoint.hasExpired(rstr));
    }

    @Test
    public void testGetWsfedTokenNull() throws Exception {
        expectedException.expect(ParsingException.class);
        expectedException.expectMessage(equalTo("WSFed response was null"));

        endpoint.getWsfedToken(null);
    }

    @Test
    public void testGetWsfedTokenEmpty() throws Exception {
        expectedException.expect(ParsingException.class);
        expectedException.expectMessage(equalTo("WSFed response was null"));

        endpoint.getWsfedToken("");
    }

    @Test
    public void testGetWsfedTokenException() throws Exception {
        String wsfedResponse = "some junk wsfed response";

        expectedException.expect(ParsingException.class);
        endpoint.getWsfedToken(wsfedResponse);
    }

    @Test
    public void testGetWsfedToken() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        String wsfedResponse = builder.getStringValue();

        RequestSecurityTokenResponse rstr = endpoint.getWsfedToken(wsfedResponse);
        assertNotNull(rstr);
    }

    @Test
    public void testGetWsfedTokenNoCollection() throws Exception {
        RequestSecurityTokenResponseBuilder builder = SAML2RequestedTokenTest.generateRequestSecurityTokenResponseBuilder(mockHelper);
        String wsfedResponse = builder.getStringValue();

        Document doc = DocumentUtil.getDocument(wsfedResponse);
        NodeList nodes = doc.getElementsByTagNameNS("http://docs.oasis-open.org/ws-sx/ws-trust/200512", "RequestSecurityTokenResponse");
        Node node = nodes.item(0);

        wsfedResponse = nodeToString(node);

        RequestSecurityTokenResponse rstr = endpoint.getWsfedToken(wsfedResponse);
        assertNotNull(rstr);
    }
}