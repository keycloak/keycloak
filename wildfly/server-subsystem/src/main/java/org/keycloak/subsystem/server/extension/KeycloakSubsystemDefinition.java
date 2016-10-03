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
package org.keycloak.subsystem.server.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.keycloak.subsystem.server.attributes.ProvidersListAttributeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of subsystem=keycloak-server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class KeycloakSubsystemDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition WEB_CONTEXT =
        new SimpleAttributeDefinitionBuilder("web-context", ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("auth"))
            .setRestartAllServices()
            .build();

    static final StringListAttributeDefinition PROVIDERS = new ProvidersListAttributeBuilder().build();
    
    static final SimpleAttributeDefinition MASTER_REALM_NAME =
        new SimpleAttributeDefinitionBuilder("master-realm-name", ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("master"))
            .setRestartAllServices()
            .build();
    
    static final SimpleAttributeDefinition SCHEDULED_TASK_INTERVAL =
        new SimpleAttributeDefinitionBuilder("scheduled-task-interval", ModelType.LONG, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("900"))
            .setRestartAllServices()
            .build();
    
    static final List<AttributeDefinition> ALL_ATTRIBUTES = new ArrayList<AttributeDefinition>();

    static {
        ALL_ATTRIBUTES.add(WEB_CONTEXT);
        ALL_ATTRIBUTES.add(PROVIDERS);
        ALL_ATTRIBUTES.add(MASTER_REALM_NAME);
        ALL_ATTRIBUTES.add(SCHEDULED_TASK_INTERVAL);
    }

    private static final Map<String, AttributeDefinition> DEFINITION_LOOKUP = new HashMap<String, AttributeDefinition>();
    static {
        for (AttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    private static KeycloakSubsystemWriteAttributeHandler attrHandler = new KeycloakSubsystemWriteAttributeHandler(ALL_ATTRIBUTES);

    protected KeycloakSubsystemDefinition() {
        super(KeycloakExtension.PATH_SUBSYSTEM,
            KeycloakExtension.getResourceDescriptionResolver("subsystem"),
            KeycloakSubsystemAdd.INSTANCE,
            KeycloakSubsystemRemoveHandler.INSTANCE
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(MigrateJsonOperation.DEFINITION, new MigrateJsonOperation());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attrDef : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attrDef, null, attrHandler);
        }
    }

    public static AttributeDefinition lookup(String name) {
        return DEFINITION_LOOKUP.get(name);
    }
}
