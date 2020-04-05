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
package org.keycloak.subsystem.saml.as7;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ServiceProviderDefinition extends SimpleResourceDefinition {

    private static final SimpleAttributeDefinition SSL_POLICY =
            new SimpleAttributeDefinitionBuilder(Constants.Model.SSL_POLICY, ModelType.STRING, true)
                    .setXmlName(Constants.XML.SSL_POLICY)
                    .build();

    private static final SimpleAttributeDefinition NAME_ID_POLICY_FORMAT =
            new SimpleAttributeDefinitionBuilder(Constants.Model.NAME_ID_POLICY_FORMAT, ModelType.STRING, true)
                    .setXmlName(Constants.XML.NAME_ID_POLICY_FORMAT)
                    .build();

    private static final SimpleAttributeDefinition LOGOUT_PAGE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.LOGOUT_PAGE, ModelType.STRING, true)
                    .setXmlName(Constants.XML.LOGOUT_PAGE)
                    .build();

    private static final SimpleAttributeDefinition FORCE_AUTHENTICATION =
            new SimpleAttributeDefinitionBuilder(Constants.Model.FORCE_AUTHENTICATION, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.FORCE_AUTHENTICATION)
                    .build();

    private static final SimpleAttributeDefinition KEEP_DOM_ASSERTION =
            new SimpleAttributeDefinitionBuilder(Constants.Model.KEEP_DOM_ASSERTION, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.KEEP_DOM_ASSERTION)
                    .build();

    private static final SimpleAttributeDefinition IS_PASSIVE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.IS_PASSIVE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.IS_PASSIVE)
                    .build();

    private static final SimpleAttributeDefinition TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN =
            new SimpleAttributeDefinitionBuilder(Constants.Model.TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN)
                    .build();

    private static final SimpleAttributeDefinition AUTODETECT_BEARER_ONLY =
            new SimpleAttributeDefinitionBuilder(Constants.Model.AUTODETECT_BEARER_ONLY, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.AUTODETECT_BEARER_ONLY)
                    .setAllowExpression(true)
                    .build();

    static final SimpleAttributeDefinition PRINCIPAL_NAME_MAPPING_POLICY =
            new SimpleAttributeDefinitionBuilder(Constants.Model.PRINCIPAL_NAME_MAPPING_POLICY, ModelType.STRING, true)
                    .setXmlName(Constants.XML.PRINCIPAL_NAME_MAPPING_POLICY)
                    .build();

    static final SimpleAttributeDefinition PRINCIPAL_NAME_MAPPING_ATTRIBUTE_NAME =
            new SimpleAttributeDefinitionBuilder(Constants.Model.PRINCIPAL_NAME_MAPPING_ATTRIBUTE_NAME, ModelType.STRING, true)
                    .setXmlName(Constants.XML.PRINCIPAL_NAME_MAPPING_ATTRIBUTE_NAME)
                    .build();

    static final ListAttributeDefinition ROLE_ATTRIBUTES =
            new StringListAttributeDefinition.Builder(Constants.Model.ROLE_ATTRIBUTES)
                    .setAllowNull(true)
                    .build();

    static final SimpleAttributeDefinition ROLE_MAPPINGS_PROVIDER_ID =
            new SimpleAttributeDefinitionBuilder(Constants.Model.ROLE_MAPPINGS_PROVIDER_ID, ModelType.STRING, true)
                    .setXmlName(Constants.XML.ID)
                    .build();

    static final PropertiesAttributeDefinition ROLE_MAPPINGS_PROVIDER_CONFIG =
            new PropertiesAttributeDefinition.Builder(Constants.Model.ROLE_MAPPINGS_PROVIDER_CONFIG, true)
                    .setXmlName(Constants.XML.PROPERTY)
                    .setWrapXmlElement(false)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {SSL_POLICY, NAME_ID_POLICY_FORMAT, LOGOUT_PAGE, FORCE_AUTHENTICATION,
            IS_PASSIVE, TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN, KEEP_DOM_ASSERTION, AUTODETECT_BEARER_ONLY};
    static final AttributeDefinition[] ELEMENTS = {PRINCIPAL_NAME_MAPPING_POLICY, PRINCIPAL_NAME_MAPPING_ATTRIBUTE_NAME, ROLE_ATTRIBUTES,
            ROLE_MAPPINGS_PROVIDER_ID, ROLE_MAPPINGS_PROVIDER_CONFIG};


    private static final HashMap<String, SimpleAttributeDefinition> ATTRIBUTE_MAP = new HashMap<>();
    private static final HashMap<String, AttributeDefinition> ALL_MAP = new HashMap<>();
    static final Collection<AttributeDefinition> ALL_ATTRIBUTES;

    static {
        for (SimpleAttributeDefinition def : ATTRIBUTES) {
            ATTRIBUTE_MAP.put(def.getXmlName(), def);
        }

        ALL_MAP.putAll(ATTRIBUTE_MAP);
        for (AttributeDefinition def : ELEMENTS) {
            ALL_MAP.put(def.getXmlName(), def);
        }
        ALL_ATTRIBUTES = Collections.unmodifiableCollection(ALL_MAP.values());
    }

    static final ServiceProviderDefinition INSTANCE = new ServiceProviderDefinition();

    private ServiceProviderDefinition() {
        super(PathElement.pathElement(Constants.Model.SERVICE_PROVIDER),
                KeycloakSamlExtension.getResourceDescriptionResolver(Constants.Model.SERVICE_PROVIDER),
                ServiceProviderAddHandler.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ALL_ATTRIBUTES);
        for (AttributeDefinition attribute : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    static SimpleAttributeDefinition lookup(String xmlName) {
        return ATTRIBUTE_MAP.get(xmlName);
    }
}
