/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.subsystem.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

/**
 * Defines attributes and operations for a secure-deployment.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class SecureDeploymentDefinition extends SimpleResourceDefinition {

    public static final String TAG_NAME = "secure-deployment";

    protected static final SimpleAttributeDefinition REALM =
            new SimpleAttributeDefinitionBuilder("realm", ModelType.STRING, true)
                    .setXmlName("realm")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition RESOURCE =
            new SimpleAttributeDefinitionBuilder("resource", ModelType.STRING, true)
                    .setXmlName("resource")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition USE_RESOURCE_ROLE_MAPPINGS =
            new SimpleAttributeDefinitionBuilder("use-resource-role-mappings", ModelType.BOOLEAN, true)
            .setXmlName("use-resource-role-mappings")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();
    protected static final SimpleAttributeDefinition BEARER_ONLY =
            new SimpleAttributeDefinitionBuilder("bearer-only", ModelType.BOOLEAN, true)
                    .setXmlName("bearer-only")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition ENABLE_BASIC_AUTH =
            new SimpleAttributeDefinitionBuilder("enable-basic-auth", ModelType.BOOLEAN, true)
                    .setXmlName("enable-basic-auth")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition PUBLIC_CLIENT =
            new SimpleAttributeDefinitionBuilder("public-client", ModelType.BOOLEAN, true)
                    .setXmlName("public-client")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final List<SimpleAttributeDefinition> DEPLOYMENT_ONLY_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
        DEPLOYMENT_ONLY_ATTRIBUTES.add(REALM);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(RESOURCE);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(USE_RESOURCE_ROLE_MAPPINGS);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(BEARER_ONLY);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(ENABLE_BASIC_AUTH);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(PUBLIC_CLIENT);
    }

    protected static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
        ALL_ATTRIBUTES.addAll(DEPLOYMENT_ONLY_ATTRIBUTES);
        ALL_ATTRIBUTES.addAll(SharedAttributeDefinitons.ATTRIBUTES);
    }

    private static final Map<String, SimpleAttributeDefinition> DEFINITION_LOOKUP = new HashMap<String, SimpleAttributeDefinition>();
    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    private static SecureDeploymentWriteAttributeHandler attrHandler = new SecureDeploymentWriteAttributeHandler(ALL_ATTRIBUTES);

    public SecureDeploymentDefinition() {
        super(PathElement.pathElement(TAG_NAME),
                KeycloakExtension.getResourceDescriptionResolver(TAG_NAME),
                SecureDeploymentAddHandler.INSTANCE,
                SecureDeploymentRemoveHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        //resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attrDef : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attrDef, null, attrHandler);
        }
    }

    public static SimpleAttributeDefinition lookup(String name) {
        return DEFINITION_LOOKUP.get(name);
    }
}
