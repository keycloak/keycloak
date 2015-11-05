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

package org.keycloak.protocol.wsfed.mappers;

import org.keycloak.protocol.wsfed.WSFedLoginProtocol;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class SAMLUserPropertyAttributeStatementMapper extends AbstractWsfedProtocolMapper implements WSFedSAMLAttributeStatementMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        UserPropertyAttributeStatementMapper mapper = new UserPropertyAttributeStatementMapper();
        configProperties.addAll(mapper.getConfigProperties());
    }

    public static final String PROVIDER_ID = "wsfed-saml-user-property-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "SAML User Property";
    }

    @Override
    public String getDisplayCategory() {
        return ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a built in user property to a SAML attribute type.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        UserPropertyAttributeStatementMapper mapper = new UserPropertyAttributeStatementMapper();
        mapper.transformAttributeStatement(attributeStatement, mappingModel, session, userSession, clientSession);
    }

    public static ProtocolMapperModel createAttributeMapper(String name, String userAttribute,
                                                            String samlAttributeName, String nameFormat, String friendlyName,
                                                            boolean consentRequired, String consentText) {
        ProtocolMapperModel mapper = UserPropertyAttributeStatementMapper.createAttributeMapper(name, userAttribute, samlAttributeName, nameFormat, friendlyName, consentRequired, consentText);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(WSFedLoginProtocol.LOGIN_PROTOCOL);
        return mapper;
    }
}
