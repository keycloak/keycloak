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
import org.keycloak.protocol.wsfed.mappers.WSFedOIDCAccessTokenMapper;
import org.codehaus.jackson.map.DeserializationConfig;
import org.junit.Test;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.saml.common.util.Base64;
import org.keycloak.util.JsonSerialization;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.UUID;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class WSFedOIDCAccessTokenBuilderTest {
    @Test
    public void testOIDCTokenGeneration() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();

        mockHelper.getClientSessionNotes().put(OIDCLoginProtocol.ISSUER, String.format("%s/realms/%s", mockHelper.getBaseUri(), mockHelper.getRealmName()));

        //Attribute Mapper
        ProtocolMapperModel atMappingModel = mock(ProtocolMapperModel.class);
        when(atMappingModel.getId()).thenReturn(UUID.randomUUID().toString());
        when(atMappingModel.getProtocolMapper()).thenReturn(UUID.randomUUID().toString());

        WSFedOIDCAccessTokenMapper atMapper = mock(WSFedOIDCAccessTokenMapper.class);
        when(atMapper.transformAccessToken(org.mockito.Mockito.any(AccessToken.class), eq(atMappingModel), eq(mockHelper.getSession()), eq(mockHelper.getUserSessionModel()), eq(mockHelper.getClientSessionModel()))).thenAnswer(new Answer<AccessToken>() {
            @Override
            public AccessToken answer(InvocationOnMock invocation) throws Throwable {
                AccessToken token = (AccessToken)invocation.getArguments()[0];
                return token;
            }
        });

        mockHelper.getProtocolMappers().put(atMappingModel, atMapper);

        mockHelper.initializeMockValues();

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
        assertNotNull(jwtString);
        JsonWebToken jwt = TestHelpers.assertToken(jwtString, mockHelper);
        assertNotNull(jwt);

        verify(atMapper, times(1)).transformAccessToken(org.mockito.Mockito.any(AccessToken.class), eq(atMappingModel), eq(mockHelper.getSession()), eq(mockHelper.getUserSessionModel()), eq(mockHelper.getClientSessionModel()));
    }

    @Test
    public void testOIDCHeader() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();

        mockHelper.getClientSessionNotes().put(OIDCLoginProtocol.ISSUER, String.format("%s/realms/%s", mockHelper.getBaseUri(), mockHelper.getRealmName()));

        mockHelper.initializeMockValues();

        //OIDC Token generation
        WSFedOIDCAccessTokenBuilder oidcBuilder = new WSFedOIDCAccessTokenBuilder();
        oidcBuilder.setSession(mockHelper.getSession())
                .setUserSession(mockHelper.getUserSessionModel())
                .setAccessCode(mockHelper.getAccessCode())
                .setClient(mockHelper.getClient())
                .setClientSession(mockHelper.getClientSessionModel())
                .setRealm(mockHelper.getRealm())
                .setX5tIncluded(true);

        String jwtString = oidcBuilder.build();
        assertNotNull(jwtString);

        JsonSerialization. mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JWSInput jws = new JWSInput(jwtString);
        String encodedHeader = jws.getEncodedHeader();
        encodedHeader = new String(Base64.decode(encodedHeader), "UTF-8");

        assertThat(encodedHeader, containsString(String.format("\"x5t\":\"%s\"", TestHelpers.getThumbPrint(mockHelper.getRealm().getCertificate()))));

        JWSHeader header = jws.getHeader();
        assertNotNull(header);
        assertEquals("JWT", header.getType());
        assertEquals("RS256", header.getAlgorithm().name());
    }
}