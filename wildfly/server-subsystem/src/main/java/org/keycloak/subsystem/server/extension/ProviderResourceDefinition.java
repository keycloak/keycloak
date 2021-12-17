/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.subsystem.server.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ProviderResourceDefinition extends SimpleResourceDefinition {

    public static final String TAG_NAME = "provider";
    
    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder("enabled", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(true))
                    .setRequired(true)
                    .setRestartAllServices()
                    .build();
    
    static final PropertiesAttributeDefinition PROPERTIES =
            new PropertiesAttributeDefinition.Builder("properties", true)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();
    
    protected static final ReloadRequiredWriteAttributeHandler WRITE_ATTR_HANDLER = new ReloadRequiredWriteAttributeHandler(ENABLED, PROPERTIES);
    
    protected ProviderResourceDefinition() {
        super(PathElement.pathElement(TAG_NAME),
            KeycloakExtension.getResourceDescriptionResolver(TAG_NAME),
            ProviderResourceAddHandler.INSTANCE,
            ProviderResourceRemoveHandler.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(ENABLED, null, WRITE_ATTR_HANDLER);
        resourceRegistration.registerReadWriteAttribute(PROPERTIES, null, WRITE_ATTR_HANDLER);
    }
    
    
}
