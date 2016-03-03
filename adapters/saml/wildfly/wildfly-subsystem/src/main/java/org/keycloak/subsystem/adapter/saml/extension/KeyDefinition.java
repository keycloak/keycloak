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
package org.keycloak.subsystem.adapter.saml.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.HashMap;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KeyDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition SIGNING =
            new SimpleAttributeDefinitionBuilder(Constants.Model.SIGNING, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.SIGNING)
                    .build();

    static final SimpleAttributeDefinition ENCRYPTION =
            new SimpleAttributeDefinitionBuilder(Constants.Model.ENCRYPTION, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.ENCRYPTION)
                    .build();

    static final SimpleAttributeDefinition PRIVATE_KEY_PEM =
            new SimpleAttributeDefinitionBuilder(Constants.Model.PRIVATE_KEY_PEM, ModelType.STRING, true)
                    .setXmlName(Constants.XML.PRIVATE_KEY_PEM)
                    .build();

    static final SimpleAttributeDefinition PUBLIC_KEY_PEM =
            new SimpleAttributeDefinitionBuilder(Constants.Model.PUBLIC_KEY_PEM, ModelType.STRING, true)
                    .setXmlName(Constants.XML.PUBLIC_KEY_PEM)
                    .build();

    static final SimpleAttributeDefinition CERTIFICATE_PEM =
            new SimpleAttributeDefinitionBuilder(Constants.Model.CERTIFICATE_PEM, ModelType.STRING, true)
                    .setXmlName(Constants.XML.CERTIFICATE_PEM)
                    .build();

    static final ObjectTypeAttributeDefinition KEY_STORE =
            ObjectTypeAttributeDefinition.Builder.of(Constants.Model.KEY_STORE,
                    KeyStoreDefinition.ALL_ATTRIBUTES)
                    .setAllowNull(true)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {SIGNING, ENCRYPTION};
    static final SimpleAttributeDefinition[] ELEMENTS = {PRIVATE_KEY_PEM, PUBLIC_KEY_PEM, CERTIFICATE_PEM};
    static final AttributeDefinition[] ALL_ATTRIBUTES = {SIGNING, ENCRYPTION, PRIVATE_KEY_PEM, PUBLIC_KEY_PEM, CERTIFICATE_PEM, KEY_STORE};

    static final HashMap<String, SimpleAttributeDefinition> ATTRIBUTE_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition def : ATTRIBUTES) {
            ATTRIBUTE_MAP.put(def.getXmlName(), def);
        }
    }

    static final HashMap<String, SimpleAttributeDefinition> ELEMENT_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition def : ELEMENTS) {
            ELEMENT_MAP.put(def.getXmlName(), def);
        }
    }

    static final KeyDefinition INSTANCE = new KeyDefinition();

    private KeyDefinition() {
        super(PathElement.pathElement(Constants.Model.KEY),
                KeycloakSamlExtension.getResourceDescriptionResolver(Constants.Model.KEY),
                new KeyAddHandler(),
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

    static SimpleAttributeDefinition lookupElement(String xmlName) {
        return ELEMENT_MAP.get(xmlName);
    }
}
