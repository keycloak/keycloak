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

package org.keycloak.protocol.wsfed;

import org.keycloak.wsfed.common.WSFedConstants;
import org.keycloak.wsfed.common.MockHelper;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.messages.Messages;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.keycloak.wsfed.common.TestHelpers.*;

public class WSFedLoginProtocolTest {
    @Mock private EventBuilder event;
    @Mock private HttpHeaders headers;

    private MockHelper mockHelper;
    private WSFedLoginProtocol loginProtocol;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockHelper = getMockHelper().initializeMockValues();

        loginProtocol = spy(new WSFedLoginProtocol());
        loginProtocol.setSession(mockHelper.getSession())
                    .setRealm(mockHelper.getRealm())
                    .setUriInfo(mockHelper.getUriInfo())
                    .setEventBuilder(event)
                    .setHttpHeaders(headers);
    }

    @Test
    public void testCancelLogin() throws Exception {
        Response response = loginProtocol.cancelLogin(mockHelper.getClientSessionModel());
        assertNotNull(response);

        assertErrorPage(mockHelper.getLoginFormsProvider(), WSFedConstants.WSFED_ERROR_NOTSIGNEDIN);
    }

    @Test
    public void testConsentDenied() throws Exception {
        Response response = loginProtocol.consentDenied(mockHelper.getClientSessionModel());
        assertNotNull(response);

        assertErrorPage(mockHelper.getLoginFormsProvider(), WSFedConstants.WSFED_ERROR_NOTSIGNEDIN);
    }

    @Test
    public void testGetEndpoint() throws Exception {
        String endpoint = loginProtocol.getEndpoint(mockHelper.getUriInfo(), mockHelper.getRealm());

        assertEquals(String.format("%s/realms/%s/protocol/wsfed", mockHelper.getBaseUri(), mockHelper.getRealmName()), endpoint);
    }

    @Test
    public void testClose() throws Exception {
        loginProtocol.close();
    }

    @Test
    public void testUseJwt() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn("true").when(client).getAttribute(WSFedLoginProtocol.WSFED_JWT);

        assertTrue(loginProtocol.useJwt(client));
    }

    @Test
    public void testDontUseJwt() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn("false").when(client).getAttribute(WSFedLoginProtocol.WSFED_JWT);

        assertFalse(loginProtocol.useJwt(client));
    }

    @Test
    public void testIncludeX5t() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn("true").when(client).getAttribute(WSFedLoginProtocol.WSFED_X5T);

        assertTrue(loginProtocol.isX5tIncluded(client));
    }

    @Test
    public void testDontIncludeX5t() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn("false").when(client).getAttribute(WSFedLoginProtocol.WSFED_X5T);

        assertFalse(loginProtocol.isX5tIncluded(client));
    }

    @Test
    public void testAuthenticatedSaml() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn("false").when(client).getAttribute(WSFedLoginProtocol.WSFED_JWT);

        Response response = loginProtocol.authenticated(mockHelper.getUserSessionModel(), mockHelper.getAccessCode());

        //We already validate token generation through other test classes so this is mainly to ensure the response gets built correctly
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMetadata().getFirst("Content-Type"));
        assertEquals("no-cache", response.getMetadata().getFirst("Pragma"));
        assertEquals("no-cache, no-store", response.getMetadata().getFirst("Cache-Control"));

        Document doc = responseToDocument(response);

        assertFormAction(doc, "POST", mockHelper.getClientSessionModel().getRedirectUri());
        assertInputNode(doc, WSFedConstants.WSFED_ACTION, WSFedConstants.WSFED_SIGNIN_ACTION);
        assertInputNode(doc, WSFedConstants.WSFED_REALM, client.getClientId());

        String wsfedResponse = getInputNodeValue(doc, WSFedConstants.WSFED_RESULT);
        assertNotNull(wsfedResponse);
        assertTokenType(wsfedResponse, "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
    }

    @Test
    public void testAuthenticatedJwt() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn("true").when(client).getAttribute(WSFedLoginProtocol.WSFED_JWT);
        doReturn("false").when(client).getAttribute(WSFedLoginProtocol.WSFED_X5T);

        Response response = loginProtocol.authenticated(mockHelper.getUserSessionModel(), mockHelper.getAccessCode());

        //We already validate token generation through other test classes so this is mainly to ensure the response gets built correctly
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMetadata().getFirst("Content-Type"));
        assertEquals("no-cache", response.getMetadata().getFirst("Pragma"));
        assertEquals("no-cache, no-store", response.getMetadata().getFirst("Cache-Control"));

        Document doc = responseToDocument(response);

        assertFormAction(doc, "POST", mockHelper.getClientSessionModel().getRedirectUri());
        assertInputNode(doc, WSFedConstants.WSFED_ACTION, WSFedConstants.WSFED_SIGNIN_ACTION);
        assertInputNode(doc, WSFedConstants.WSFED_REALM, client.getClientId());

        String wsfedResponse = getInputNodeValue(doc, WSFedConstants.WSFED_RESULT);
        assertNotNull(wsfedResponse);
        assertTokenType(wsfedResponse, "urn:ietf:params:oauth:token-type:jwt");
    }

    @Test
    public void testFinishLogoutNoUrl() throws Exception {
        Response response = loginProtocol.finishLogout(mockHelper.getUserSessionModel());
        assertNotNull(response);
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.FAILED_LOGOUT);
    }

    @Test
    public void testFinishLogout() throws Exception {
        UserSessionModel userSession = mockHelper.getUserSessionModel();
        doReturn("https://someurl").when(userSession).getNote(WSFedLoginProtocol.WSFED_LOGOUT_BINDING_URI);
        doReturn("context").when(userSession).getNote(WSFedLoginProtocol.WSFED_CONTEXT);

        Response response = loginProtocol.finishLogout(userSession);
        verifyZeroInteractions(mockHelper.getLoginFormsProvider());

        Document doc = responseToDocument(response);

        assertFormAction(doc, HttpMethod.GET, userSession.getNote(WSFedLoginProtocol.WSFED_LOGOUT_BINDING_URI));
        assertInputNode(doc, WSFedConstants.WSFED_CONTEXT, userSession.getNote(WSFedLoginProtocol.WSFED_CONTEXT));
    }

    @Test
    public void testFrontchannelLogout() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn(new HashSet<String>(Arrays.asList("https://slourl"))).when(client).getRedirectUris();

        Response response = loginProtocol.frontchannelLogout(mockHelper.getUserSessionModel(), mockHelper.getClientSessionModel());
        verifyZeroInteractions(mockHelper.getLoginFormsProvider());
        Document doc = responseToDocument(response);

        assertFormAction(doc, HttpMethod.GET, "https://slourl");
        assertInputNode(doc, WSFedConstants.WSFED_ACTION, WSFedConstants.WSFED_SIGNOUT_CLEANUP_ACTION);
        assertInputNode(doc, WSFedConstants.WSFED_REPLY, String.format("%s/realms/%s/protocol/wsfed", mockHelper.getBaseUri(), mockHelper.getRealmName()));
    }

    @Test
    public void testFrontchannelLogoutNoUrl() throws Exception {
        Response response = loginProtocol.frontchannelLogout(mockHelper.getUserSessionModel(), mockHelper.getClientSessionModel());
        assertNotNull(response);
        assertErrorPage(mockHelper.getLoginFormsProvider(), Messages.FAILED_LOGOUT);
    }

    @Test
    public void testBackchannelLogoutNoUrl() throws Exception {
        loginProtocol.backchannelLogout(mockHelper.getUserSessionModel(), mockHelper.getClientSessionModel());
        verify(mockHelper.getSession(), times(0)).getProvider(eq(HttpClientProvider.class));
    }

    @Test
    public void testBackchannelLogout() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn(new HashSet<String>(Arrays.asList("https://slourl"))).when(client).getRedirectUris();

        KeycloakSession session = mockHelper.getSession();
        HttpClientProvider provider = mock(HttpClientProvider.class);
        HttpClient httpClient = mock(HttpClient.class);

        doReturn(httpClient).when(provider).getHttpClient();
        doReturn(provider).when(session).getProvider(eq(HttpClientProvider.class));

        HttpResponse response = mock(HttpResponse.class);
        StatusLine sl = mock(StatusLine.class);
        doReturn(200).when(sl).getStatusCode();
        doReturn(sl).when(response).getStatusLine();
        doReturn(response).when(httpClient).execute(any(HttpGet.class));

        loginProtocol.backchannelLogout(mockHelper.getUserSessionModel(), mockHelper.getClientSessionModel());

        verify(sl, times(1)).getStatusCode();
        verify(response, times(0)).getFirstHeader(eq(HttpHeaders.LOCATION));
    }

    @Test
    public void testBackchannelLogoutWithSingleRedirect() throws Exception {
        ClientModel client = mockHelper.getClient();
        doReturn(new HashSet<String>(Arrays.asList("https://slourl"))).when(client).getRedirectUris();

        KeycloakSession session = mockHelper.getSession();
        HttpClientProvider provider = mock(HttpClientProvider.class);
        HttpClient httpClient = mock(HttpClient.class);

        doReturn(httpClient).when(provider).getHttpClient();
        doReturn(provider).when(session).getProvider(eq(HttpClientProvider.class));

        HttpResponse response = mock(HttpResponse.class);
        Header location = mock(Header.class);
        doReturn("https://slourl/").when(location).getValue();
        doReturn(location).when(response).getFirstHeader(HttpHeaders.LOCATION);

        StatusLine sl = mock(StatusLine.class);
        doReturn(302).when(sl).getStatusCode();
        doReturn(sl).when(response).getStatusLine();
        doReturn(response).when(httpClient).execute(any(HttpGet.class));

        loginProtocol.backchannelLogout(mockHelper.getUserSessionModel(), mockHelper.getClientSessionModel());

        verify(sl, times(2)).getStatusCode();
        verify(response, times(1)).getFirstHeader(eq(HttpHeaders.LOCATION));
    }
}