/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.keycloak.subsystem.extension.authserver;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.OperationEntry;
import org.keycloak.subsystem.extension.KeycloakAdapterConfigService;
import org.keycloak.subsystem.extension.KeycloakExtension;

/**
 * Defines attributes and operations for an Auth Server
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class AuthServerDefinition extends SimpleResourceDefinition {

    public static final String TAG_NAME = "auth-server";

    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder("enabled", ModelType.BOOLEAN, true)
            .setXmlName("enabled")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition WEB_CONTEXT =
            new SimpleAttributeDefinitionBuilder("web-context", ModelType.STRING, true)
            .setXmlName("web-context")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("auth"))
            .setValidator(new WebContextValidator())
            .setRestartAllServices()
            .build();

    protected static final ResourceDescriptionResolver rscDescriptionResolver = KeycloakExtension.getResourceDescriptionResolver(TAG_NAME);

    public static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
        ALL_ATTRIBUTES.add(ENABLED);
        ALL_ATTRIBUTES.add(WEB_CONTEXT);
    }

    private static final Map<String, SimpleAttributeDefinition> DEFINITION_LOOKUP = new HashMap<String, SimpleAttributeDefinition>();
    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    private static AuthServerWriteAttributeHandler attrHandler = new AuthServerWriteAttributeHandler(ALL_ATTRIBUTES);

    public AuthServerDefinition() {
        super(PathElement.pathElement(TAG_NAME),
                rscDescriptionResolver,
                AuthServerAddHandler.INSTANCE,
                AuthServerRemoveHandler.INSTANCE,
                null,
                OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(AddProviderHandler.DEFINITION, AddProviderHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(OverlayKeycloakServerJsonHandler.DEFINITION, OverlayKeycloakServerJsonHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(ListOverlaysHandler.DEFINITION, ListOverlaysHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(RemoveOverlayHandler.DEFINITION, RemoveOverlayHandler.INSTANCE);
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

    private static class WebContextValidator implements ParameterValidator {

        @Override
        public void validateParameter(String paramName, ModelNode value) throws OperationFailedException {
            String strValue = value.asString();
            if (KeycloakAdapterConfigService.INSTANCE.isWebContextUsed(strValue)) {
                throw new OperationFailedException("Can not set web-context to '" + strValue + "'. web-context must be unique among all deployments.");
            }
        }

        @Override
        public void validateResolvedParameter(String paramName, ModelNode value) throws OperationFailedException {
            String strValue = value.asString();
            if (KeycloakAdapterConfigService.INSTANCE.isWebContextUsed(strValue)) {
                throw new OperationFailedException("Can not set web-context to '" + strValue + "'. web-context must be unique among all deployments.");
            }
        }

    }
}
