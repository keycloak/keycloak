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

package org.keycloak.wsfed.common.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.events.EventBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.wsfed.common.TestHelpers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Created on 5/27/15.
 */
public class WSFedValidatorTest {
    @Mock
    private RealmModel mockRealm;
    @Mock private UriInfo uriInfo;
    @Mock private ClientConnection connection;
    @Mock private EventBuilder mockEvent;
    @Mock private KeycloakSession mockSession;

    @Mock private LoginFormsProvider loginFormsProvider;
    @Mock private Response errorResponse;

    @InjectMocks
    private WSFedValidator validator;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(loginFormsProvider.setError(anyString())).thenReturn(loginFormsProvider);
        when(loginFormsProvider.createErrorPage()).thenReturn(errorResponse);
        when(mockSession.getProvider(LoginFormsProvider.class)).thenReturn(loginFormsProvider);
    }

    @After
    public void tearDown() throws Exception {
        reset(mockRealm);
    }

    @Test
    public void testBasicChecksCheckSSLFail() throws Exception {
        when(uriInfo.getBaseUri()).thenReturn(new URI("http://foo"));
        when(mockRealm.getSslRequired()).thenReturn(SslRequired.ALL);
        when(connection.getRemoteAddr()).thenReturn("http://foo");

        Response response = validator.basicChecks("", uriInfo, connection, mockSession);
        assertNotNull(response);
        assertEquals(errorResponse, response);

        verify(mockRealm, times(1)).getSslRequired();

        TestHelpers.assertErrorPage(loginFormsProvider, Messages.HTTPS_REQUIRED);
        verifyNoMoreInteractions(mockRealm, loginFormsProvider);
    }

    @Test
    public void testBasicChecksIsEnabledFail() throws Exception {
        when(uriInfo.getBaseUri()).thenReturn(new URI("https://foo"));
        when(mockRealm.isEnabled()).thenReturn(false);

        validator.basicChecks("", uriInfo, connection, mockSession);

        verify(mockRealm, times(1)).isEnabled();
        TestHelpers.assertErrorPage(loginFormsProvider, Messages.REALM_NOT_ENABLED);
        verifyNoMoreInteractions(mockRealm, loginFormsProvider);
    }

    @Test
    public void testBasicChecksActionIsNullFail() throws Exception {
        when(uriInfo.getBaseUri()).thenReturn(new URI("https://foo"));
        when(mockRealm.isEnabled()).thenReturn(true);

        validator.basicChecks(null, uriInfo, connection, mockSession);

        verify(mockRealm, times(1)).isEnabled();
        TestHelpers.assertErrorPage(loginFormsProvider, Messages.INVALID_REQUEST);
        verifyNoMoreInteractions(mockRealm, loginFormsProvider);
    }

    @Test
    public void testBasicChecksSuccess() throws Exception {
        when(uriInfo.getBaseUri()).thenReturn(new URI("https://foo"));
        when(mockRealm.isEnabled()).thenReturn(true);
        validator.basicChecks("", uriInfo, connection, mockSession);

        validateMockitoUsage();

        verify(mockRealm, times(1)).isEnabled();
        verifyZeroInteractions(loginFormsProvider);
        verifyNoMoreInteractions(mockRealm);
    }
}