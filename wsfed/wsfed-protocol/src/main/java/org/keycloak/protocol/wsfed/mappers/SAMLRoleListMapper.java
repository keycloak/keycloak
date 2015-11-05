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
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class SAMLRoleListMapper extends AbstractWsfedProtocolMapper implements WSFedSAMLRoleListMapper {
    public static final String PROVIDER_ID = "wsfed-saml-role-list-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        RoleListMapper mapper = new RoleListMapper();
        configProperties.addAll(mapper.getConfigProperties());
    }

    @Override
    public String getDisplayCategory() {
        return SAML_ROLE_MAPPER;
    }

    @Override
    public String getDisplayType() {
        return "SAML Role list";
    }

    @Override
    public String getHelpText() {
        return "Role names are stored in an attribute value.  There is either one attribute with multiple attribute values, or an attribute per role name depending on how you configure it.  You can also specify the attribute name i.e. 'Role' or 'memberOf' being examples.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void mapRoles(AttributeStatementType roleAttributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        RoleListMapper samlMapper = new RoleListMapper();
        samlMapper.mapRoles(roleAttributeStatement, mappingModel, session, userSession, clientSession);
    }

    public static ProtocolMapperModel create(String name, String samlAttributeName, String nameFormat, String friendlyName, boolean singleAttribute) {
        ProtocolMapperModel mapper = RoleListMapper.create(name, samlAttributeName, nameFormat, friendlyName, singleAttribute);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(WSFedLoginProtocol.LOGIN_PROTOCOL);
        return mapper;
    }

}
