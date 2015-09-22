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

import org.keycloak.protocol.wsfed.mappers.WSFedSAMLAttributeStatementMapper;
import org.keycloak.wsfed.common.MockHelper;
import org.keycloak.wsfed.common.TestHelpers;
import org.keycloak.protocol.wsfed.mappers.WSFedSAMLRoleListMapper;
import org.junit.Test;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import java.net.URI;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by dbarentine on 8/24/2015.
 */
public class WSFedSAML2AssertionTypeBuilderTest {
    @Test
    public void testSamlTokenGeneration() throws Exception {
        MockHelper mockHelper = TestHelpers.getMockHelper();

        mockHelper.getClientAttributes().put(WSFedSAML2AssertionTypeBuilder.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "false");
        mockHelper.getClientSessionNotes().put(GeneralConstants.NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());

        //Role Mapper
        ProtocolMapperModel roleMappingModel = mock(ProtocolMapperModel.class);
        when(roleMappingModel.getProtocolMapper()).thenReturn(UUID.randomUUID().toString());
        WSFedSAMLRoleListMapper roleListMapper = mock(WSFedSAMLRoleListMapper.class);
        mockHelper.getProtocolMappers().put(roleMappingModel, roleListMapper);

        //Attribute Mapper
        ProtocolMapperModel attributeMappingModel = mock(ProtocolMapperModel.class);
        when(attributeMappingModel.getProtocolMapper()).thenReturn(UUID.randomUUID().toString());
        WSFedSAMLAttributeStatementMapper attributeMapper = mock(WSFedSAMLAttributeStatementMapper.class);
        mockHelper.getProtocolMappers().put(attributeMappingModel, attributeMapper);

        mockHelper.initializeMockValues();

        //SAML Token generation
        WSFedSAML2AssertionTypeBuilder samlBuilder = new WSFedSAML2AssertionTypeBuilder();
        samlBuilder.setRealm(mockHelper.getRealm())
                .setUriInfo(mockHelper.getUriInfo())
                .setAccessCode(mockHelper.getAccessCode())
                .setClientSession(mockHelper.getClientSessionModel())
                .setUserSession(mockHelper.getUserSessionModel())
                .setSession(mockHelper.getSession());

        AssertionType token = samlBuilder.build();

        assertNotNull(token);

        assertEquals(String.format("%s/realms/%s", mockHelper.getBaseUri(), mockHelper.getRealmName()), token.getIssuer().getValue());
        assertEquals(URI.create(mockHelper.getClientSessionNotes().get(GeneralConstants.NAMEID_FORMAT)), token.getSubject().getConfirmation().get(0).getNameID().getFormat());
        assertEquals(mockHelper.getEmail(), token.getSubject().getConfirmation().get(0).getNameID().getValue());

        assertNotNull(token.getIssueInstant());
        assertNotNull(token.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getNotBefore());
        assertNotNull(token.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getNotOnOrAfter());

        assertNotNull(token.getConditions().getNotBefore());
        assertNotNull(token.getConditions().getNotOnOrAfter());

        assertEquals(token.getConditions().getNotBefore(), token.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getNotBefore());
        assertEquals(XMLTimeUtil.add(token.getConditions().getNotBefore(), mockHelper.getAccessCodeLifespan() * 1000), token.getConditions().getNotOnOrAfter());
        assertEquals(XMLTimeUtil.add(token.getConditions().getNotBefore(), mockHelper.getAccessTokenLifespan() * 1000), token.getSubject().getConfirmation().get(0).getSubjectConfirmationData().getNotOnOrAfter());

        assertEquals(mockHelper.getClientId(), ((AudienceRestrictionType) token.getConditions().getConditions().get(0)).getAudience().get(0).toString());

        ClientSessionModel clientSession = mockHelper.getClientSessionModel();
        verify(clientSession, times(1)).setNote(WSFedSAML2AssertionTypeBuilder.WSFED_NAME_ID, mockHelper.getEmail());
        verify(clientSession, times(1)).setNote(WSFedSAML2AssertionTypeBuilder.WSFED_NAME_ID_FORMAT, mockHelper.getClientSessionNotes().get(GeneralConstants.NAMEID_FORMAT));

        verify(roleListMapper, times(1)).mapRoles(org.mockito.Mockito.any(AttributeStatementType.class), eq(roleMappingModel), eq(mockHelper.getSession()), eq(mockHelper.getUserSessionModel()), eq(mockHelper.getClientSessionModel()));
        verify(attributeMapper, times(1)).transformAttributeStatement(org.mockito.Mockito.any(AttributeStatementType.class), eq(attributeMappingModel), eq(mockHelper.getSession()), eq(mockHelper.getUserSessionModel()), eq(mockHelper.getClientSessionModel()));
    }
}