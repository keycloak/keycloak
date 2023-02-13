/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.saml.mappers;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleListMapper extends AbstractSAMLProtocolMapper implements SAMLRoleListMapper {
    public static final String PROVIDER_ID = "saml-role-list-mapper";
    public static final String SINGLE_ROLE_ATTRIBUTE = "single";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.SAML_ATTRIBUTE_NAME);
        property.setLabel("Role attribute name");
        property.setDefaultValue("Role");
        property.setHelpText("Name of the SAML attribute you want to put your roles into.  i.e. 'Role', 'memberOf'.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.FRIENDLY_NAME);
        property.setLabel(AttributeStatementHelper.FRIENDLY_NAME_LABEL);
        property.setHelpText(AttributeStatementHelper.FRIENDLY_NAME_HELP_TEXT);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT);
        property.setLabel("SAML Attribute NameFormat");
        property.setHelpText("SAML Attribute NameFormat.  Can be basic, URI reference, or unspecified.");
        List<String> types = new ArrayList(3);
        types.add(AttributeStatementHelper.BASIC);
        types.add(AttributeStatementHelper.URI_REFERENCE);
        types.add(AttributeStatementHelper.UNSPECIFIED);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(types);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(SINGLE_ROLE_ATTRIBUTE);
        property.setLabel("Single Role Attribute");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText("If true, all roles will be stored under one attribute with multiple attribute values.");
        configProperties.add(property);

    }


    @Override
    public String getDisplayCategory() {
        return "Role Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Role list";
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
    public void mapRoles(AttributeStatementType roleAttributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        String single = mappingModel.getConfig().get(SINGLE_ROLE_ATTRIBUTE);
        boolean singleAttribute = Boolean.parseBoolean(single);

        List<SamlProtocol.ProtocolMapperProcessor<SAMLRoleNameMapper>> roleNameMappers = new LinkedList<>();
        AtomicReference<AttributeType> singleAttributeType = new AtomicReference<>(null);

        ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx)
                .forEach(entry -> {
                    ProtocolMapperModel mapping = entry.getKey();
                    ProtocolMapper mapper = entry.getValue();

                    if (mapper instanceof SAMLRoleNameMapper) {
                        roleNameMappers.add(new SamlProtocol.ProtocolMapperProcessor<>((SAMLRoleNameMapper) mapper,mapping));
                    }

                    if (mapper instanceof HardcodedRole) {
                        AttributeType attributeType;
                        if (singleAttribute) {
                            if (singleAttributeType.get() == null) {
                                singleAttributeType.set(AttributeStatementHelper.createAttributeType(mappingModel));
                                roleAttributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(singleAttributeType.get()));
                            }
                            attributeType = singleAttributeType.get();
                        } else {
                            attributeType = AttributeStatementHelper.createAttributeType(mappingModel);
                            roleAttributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attributeType));
                        }

                        attributeType.addAttributeValue(mapping.getConfig().get(HardcodedRole.ROLE_ATTRIBUTE));
                    }
                });


        List<String> allRoleNames = clientSessionCtx.getRolesStream()
          // todo need a role mapping
          .map(roleModel -> roleNameMappers.stream()
            .map(entry -> entry.mapper.mapName(entry.model, roleModel))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(roleModel.getName())
          ).collect(Collectors.toList());

        for (String roleName : allRoleNames) {
            AttributeType attributeType;
            if (singleAttribute) {
                if (singleAttributeType.get() == null) {
                    singleAttributeType.set(AttributeStatementHelper.createAttributeType(mappingModel));
                    roleAttributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(singleAttributeType.get()));
                }
                attributeType = singleAttributeType.get();
            } else {
                attributeType = AttributeStatementHelper.createAttributeType(mappingModel);
                roleAttributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attributeType));
            }

            attributeType.addAttributeValue(roleName);
        }

    }

    public static ProtocolMapperModel create(String name, String samlAttributeName, String nameFormat, String friendlyName, boolean singleAttribute) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        if (friendlyName != null) {
            config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        }
        if (nameFormat != null) {
            config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        }
        config.put(SINGLE_ROLE_ATTRIBUTE, Boolean.toString(singleAttribute));
        mapper.setConfig(config);

        return mapper;
    }

}
