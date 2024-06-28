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
public class IdentityProviderDefinition extends SimpleResourceDefinition {

    private static final SimpleAttributeDefinition SIGNATURES_REQUIRED =
            new SimpleAttributeDefinitionBuilder(Constants.Model.SIGNATURES_REQUIRED, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.SIGNATURES_REQUIRED)
                    .build();

    private static final SimpleAttributeDefinition SIGNATURE_ALGORITHM =
            new SimpleAttributeDefinitionBuilder(Constants.Model.SIGNATURE_ALGORITHM, ModelType.STRING, true)
                    .setXmlName(Constants.XML.SIGNATURE_ALGORITHM)
                    .build();

    private static final SimpleAttributeDefinition SIGNATURE_CANONICALIZATION_METHOD =
            new SimpleAttributeDefinitionBuilder(Constants.Model.SIGNATURE_CANONICALIZATION_METHOD, ModelType.STRING, true)
                    .setXmlName(Constants.XML.SIGNATURE_CANONICALIZATION_METHOD)
                    .build();

    private static final SimpleAttributeDefinition METADATA_URL =
            new SimpleAttributeDefinitionBuilder(Constants.Model.METADATA_URL, ModelType.STRING, true)
                    .setXmlName(Constants.XML.METADATA_URL)
                    .setAllowExpression(true)
                    .build();

    private static final ObjectTypeAttributeDefinition SINGLE_SIGN_ON =
            ObjectTypeAttributeDefinition.Builder.of(Constants.Model.SINGLE_SIGN_ON,
                    SingleSignOnDefinition.ATTRIBUTES)
                    .setRequired(true)
                    .build();

    private static final ObjectTypeAttributeDefinition SINGLE_LOGOUT =
            ObjectTypeAttributeDefinition.Builder.of(Constants.Model.SINGLE_LOGOUT,
                    SingleLogoutDefinition.ATTRIBUTES)
                    .setRequired(true)
                    .build();

    private static final ObjectTypeAttributeDefinition ALLOWED_CLOCK_SKEW =
            ObjectTypeAttributeDefinition.Builder.of(Constants.Model.ALLOWED_CLOCK_SKEW,
                    AllowedClockSkew.ATTRIBUTES)
                    .setRequired(false)
                    .build();

    private static final ObjectTypeAttributeDefinition HTTP_CLIENT =
            ObjectTypeAttributeDefinition.Builder.of(Constants.Model.HTTP_CLIENT,
                    HttpClientDefinition.ATTRIBUTES)
                    .setRequired(false)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {SIGNATURES_REQUIRED, SIGNATURE_ALGORITHM, SIGNATURE_CANONICALIZATION_METHOD, METADATA_URL};

    static final SimpleAttributeDefinition[] ALL_ATTRIBUTES = {SIGNATURES_REQUIRED, SIGNATURE_ALGORITHM, SIGNATURE_CANONICALIZATION_METHOD, METADATA_URL,
            SINGLE_SIGN_ON, SINGLE_LOGOUT, ALLOWED_CLOCK_SKEW, HTTP_CLIENT};

    private static final HashMap<String, SimpleAttributeDefinition> ATTRIBUTE_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            ATTRIBUTE_MAP.put(def.getXmlName(), def);
        }
    }

    static final IdentityProviderDefinition INSTANCE = new IdentityProviderDefinition();

    private IdentityProviderDefinition() {
        super(PathElement.pathElement(Constants.Model.IDENTITY_PROVIDER),
                KeycloakSamlExtension.getResourceDescriptionResolver(Constants.Model.IDENTITY_PROVIDER),
                new IdentityProviderAddHandler(),
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